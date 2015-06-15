package de.root1.ekeydecoder;

import de.root1.slicknx.Knx;
import de.root1.slicknx.KnxException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Ekey {

    private static Logger log = LoggerFactory.getLogger(Ekey.class);

    static {
        if (System.getProperty("java.util.logging.config.file") == null) {
            System.out.println("Please specify logfile by passing '-Djava.util.logging.config.file=<logconfig-file>' to JVM to get advanced log possibilities.");
            LogFormatter formatter = new LogFormatter();
            Handler[] handlers = java.util.logging.Logger.getLogger("").getHandlers();
            for (Handler handler : handlers) {
                handler.setFormatter(formatter);
            }
        }
    }
    private InputStream is;
    private RS232 rs232;
    private Knx knx;
    private final File confdir;
    private boolean running;

    private Ekey(File confdir) {
//        Runtime.getRuntime().addShutdownHook(new Thread("ShutdownHook") {
//            public void run() {
//                log.info("CTRL+C detected. Shutdown.");
//                } finally {
//                    System.out.println("Exiting...");
//                    System.exit(0);
//                }
//            }
//        });

        this.confdir = confdir;

    }

    public Ekey(File confdir, File f) throws IOException, KnxException {
        this(confdir);
        is = new FileInputStream(f);
        work();
    }

    public Ekey(File confdir, String host, int port) throws IOException, KnxException {
        this(confdir);
        Socket s = new Socket(host, port);
        is = s.getInputStream();
        work();
    }

    public Ekey(File confdir, String device) throws IOException, KnxException {
        this(confdir);
        RS232Config config = new RS232Config(device,
                RS232Config.BAUDRATE_115200,
                RS232Config.DATABITS_8,
                RS232Config.PARITY_NONE,
                RS232Config.STOPBITS_1,
                RS232Config.FLOWCONTROL_NONE,
                1000);
        rs232 = new RS232("ekeydecoder", config);
        is = rs232.getInputStream();
        work();
    }

    private void work() throws IOException, KnxException {

        running = true;
        File f = new File(confdir, "finger.properties");
        final Properties fingerprop = new Properties();
        fingerprop.load(new FileReader(f));

        File k = new File(confdir, "knx.properties");
        final Properties knxprop = new Properties();
        knxprop.load(new FileReader(k));

        if (knxprop.getProperty("knx.pa") != null) {
            knx = new Knx(knxprop.getProperty("knx.pa"));
        } else {
            knx = new Knx();
        }

        final String gaFingerhash = knxprop.getProperty("knx.ga.fingerhash");
        final String gaString = knxprop.getProperty("knx.ga.string");
        final String stringFormat = knxprop.getProperty("knx.stringformat");

        EkeyDecoder decoder = new EkeyDecoder(is);
        decoder.setListener(new EkeyDecoderListener() {

            @Override
            public void fingerhashReceived(int fingerhash) {
                String fingerhashString = String.format("0x%02x", fingerhash);
                log.info("Fingerhash received: {}", fingerhashString);
                String name = fingerprop.getProperty(fingerhashString);

                if (gaFingerhash != null) {
                    try {
                        knx.writeDpt6(false, gaFingerhash, (byte) fingerhash);
                        log.info("Sent fingerhash {} to {}", fingerhashString, gaFingerhash);
                    } catch (KnxException ex) {
                        log.error("unable to send fingerhash to knx", ex);
                    }
                }
                if (name != null) {
                    log.info("Fingerhash [{}] resolves to '{}'", fingerhashString, name);
                    if (gaString != null) {
                        String string = fingerhash + "/" + name;
                        if (stringFormat != null) {
                            string = String.format(stringFormat, fingerhash, name);
                        }
                        if (string.length() > 14) {
                            string = string.substring(0, 14);
                        }
                        try {
                            knx.writeString(false, gaString, string);
                            log.info("Send string '{}' to {}", string, gaString);
                        } catch (KnxException ex) {
                            log.error("unable to send string to knx", ex);
                        }
                    }
                }
            }

        });

        decoder.waitForTermination();
        if (is != null) {
            try {
                is.close();
            } catch (IOException ex) {
                // nothing to do
            }
        }
        log.warn("decoder terminated. Stream closed.");
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public static void main(String[] args) throws KnxException, FileNotFoundException, IOException {

        log.info("Running ...");

        log.debug("Args (" + args.length + "): " + Arrays.toString(args));

        File configdir = new File(".");

        if (args.length == 2) {
            if (args[0].equals("-confdir")) {
                configdir = new File(args[1]);
            }
        }
        log.info("Configdir: {}", configdir);

        boolean reconnactable = true;

        while (true) {
            try {
                File c = new File(configdir, "config.properties");
                final Properties configprop = new Properties();
                configprop.load(new FileReader(c));

                String type = configprop.getProperty("type").toUpperCase();
                String setting = configprop.getProperty("setting");

                switch (type) {
                    case "FILE":
                        reconnactable = false;
                        log.info("Using File-Mode: '" + setting + "'");
                        new Ekey(configdir, new File(setting));
                        break;
                    case "DEVICE":
                        log.info("Using Device-Mode: '" + setting + "'");
                        new Ekey(configdir, setting);
                        break;
                    case "SOCKET":
                        log.info("Using Socket-Mode: '" + setting + "'");
                        String host = setting.split(":")[0];
                        int port = Integer.parseInt(setting.split(":")[1]);
                        new Ekey(configdir, host, port);
                        break;
                    default:
                        log.error("Type '{}' unknown.", type);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (reconnactable) {
                    log.info("Trying to get connection back online in 5sec...");
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                    }
                } else {
                    System.exit(0);
                }
            }

        }

    }

}
