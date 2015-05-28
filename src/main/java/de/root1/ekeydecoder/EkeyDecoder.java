package de.root1.ekeydecoder;

import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
public class EkeyDecoder implements Runnable {

    private final Logger log = LoggerFactory.getLogger(EkeyDecoder.class);

    /**
     * Puffergröße für empfangene RS485 Frames
     */
    private static final int FRAME_BUFFER_MAX = 256;
    private EkeyDecoderListener listener;

    private enum DecoderState {

        WaitingForStart, WaitingForLength, WaitingForLengthExtension, WaitingForContent
    }
    private DecoderState state = DecoderState.WaitingForStart;

    int frameLength;
    int[] frame = new int[FRAME_BUFFER_MAX];
    int frameIndex = -1;

    private final InputStream inputstream;

    public EkeyDecoder(InputStream inputstream) {
        this.inputstream = inputstream;
        new Thread(this, "EkeyDecoder").start();
    }

    void setListener(EkeyDecoderListener listener) {
        this.listener = listener;
    }

    /**
     * Byte-Stuffing rückgängig machen. Zum Transport wurden die Daten
     * aufgeblasen, um das Auftreten der Beginn- und Endekennung innerhalb der
     * Nutzerdaten zu vermeiden:
     *
     * <pre>
     * 02 --> 3F 41
     * 03 --> 3F 81
     * 3F --> 3F C1
     * </pre>
     *
     * Diese Methode macht dies wieder rückgängig.
     */
    private void unstuff() {

        /*
         Gehen wir mal davon aus, dass im Addressierungsteil (Index 0 bis 14) 
         kein Byte-Stuffing verwendet wird. Das spart hier etwas Zeit zum Durchlaufen.
         */
        if (frameLength <= 15) {
            return;
        }

        boolean unstuffed;
        int i = 15; // start am Index 15, da zuvor wohl nichts zum unstuffen da ist

        while (true) {
            if (frame[i] == 0x3F) {
                unstuffed = false;
                switch (frame[i + 1]) {
                    case 0x41:
                        frame[i] = (byte) 0x02;
                        unstuffed = true;
                        break;
                    case 0x81:
                        frame[i] = (byte) 0x03;
                        unstuffed = true;
                        break;
                    case 0xC1:
                        frame[i] = (byte) 0x3F;
                        unstuffed = true;
                        break;
                    /* 
                     Default: Falls irgend etwas anderes der 3F folgt, 
                     dann scheint die 3F ein normales Datenbyte zu sein 
                     --> kein Unstuffing. 
                     */

                }
                if (unstuffed) {
                    for (int j = i + 1; j < frameLength; j++) {
                        frame[j] = frame[j + 1];
                    }
                    frameLength--;
                }
            }
            i++;
            if (i >= frameLength) {
                return;
            }
        }

    }

    private void frameReceived() {

        boolean dumpFrame = false;
        /* Plausibilisierungen */
        if ((frame[0] != 0x02)) {
            log.warn("head wrong");
            dumpFrame=true;
        }
        if (frame[frameLength - 1] != 0x03) {
            log.warn("tail wrong");
            dumpFrame=true;
        }

        unstuff();
        
        /*
        byte index      Funktion
        0               Start-Byte, immer 0x02
        1               Nutzdatenlänge. Wert muss ganzzahlig durch 4 geteilt werden, Bsp. 0x71(hex) -> 113(dez), geteilt durch 4 -> 28. + Startbyte + Stopbyte = 30 Bytes im Frame
        2               Fix auf 0x20??
        3               Fix auf 0x82??
        4               Nachrichtentyp????
        5               Nachrichtentyp????
        6               Nachrichtentyp????
        7               Zieladresse
        8               Zieladresse
        9               Zieladresse
        10              Zieladresse
        11              Quelladresse
        12              Quelladresse
        13              Quelladresse
        14              Quelladresse
        15
        16              Zähler der von Anfrage zu Antwort zusammenpasst
        17              Fingerhash, wenn die Nachricht 47 bytes lang ist
        ...             ???
        n               End-Byte, immer 0x03
        
        
        */
        if (log.isTraceEnabled() || dumpFrame) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < frameLength; i++) {
                sb.append(String.format("%02x", frame[i]));
                if (i < frameLength) {
                    sb.append(" ");
                }
            }
            if (dumpFrame) {
                log.warn("Raw Frame [{}]: {}", String.format(sb.toString(), frame.length, frame));    
            } else {
                log.trace("Raw Frame [{}]: {}", String.format(sb.toString(), frame.length, frame));
            }
            
        }

        if (frameLength == 47) {
            log.debug("***** Fingerhash: {}", String.format("0x%02x", frame[17]));
            listener.fingerhashReceived(frame[17]);
        }

    }

    // Hier kommen die empfangenen Bytes einzeln rein
    private void byteReceived(int n) {

        switch (state) {
            case WaitingForStart:
                if (n == 2) {
                    state = DecoderState.WaitingForLength;
                    frame[0] = n;
                    log.trace("Waiting for length");
                }
                break;
            case WaitingForLength:
                /* Längenbyte ist angekommen. Die eigentliche Länge der Nutzerdaten
                 ist (n-1)/4. Weitere Teile der Längeninformation stecken im nächsten Byte.
                 */
                frameLength = (n - 1) >> 2;
                /* Zusätzlich zur Nutzdatenlänge wollen wir noch "StartOfFrame" 
                 und Längenbyte im Trace haben --> zwei byte mehr.
                 */
                frameLength = frameLength + 2;
                state = DecoderState.WaitingForLengthExtension;
                frame[1] = n;
                log.trace("Waiting for LengthExtension");
                break;
            case WaitingForLengthExtension:
                /* Zweites Längenbyte ist angekommen. Bit 0 bedeutet 64.
                 Ob Bit 1 dann 128 bedeutet ist noch unklar.
                 */
                if ((n & 1) == 1) {
                    log.trace("2nd lengthbyte -> +64");
                    frameLength = frameLength + 64;
                }
                state = DecoderState.WaitingForContent;
                frame[2] = n;
                log.trace("frame has " + frameLength + " Bytes");
                frameIndex = 3;
                break;
            case WaitingForContent:
                if (frameIndex < frameLength) {
                    log.trace("Received index {} (Byte {} of {})", new Object[]{frameIndex, frameIndex + 1, frameLength});
                    frame[frameIndex] = n;
                    frameIndex++;
                } else {
                    log.trace("Frame complete");
                    frameReceived();
                    state = DecoderState.WaitingForStart;
                }
                /*
                 frame[frameIndex] = n;
                 frameIndex++;
                 log.trace("Received " + frameIndex + " bytes. Frame must have "+frameLength +" bytes. So "+(frameLength-frameIndex)+" bytes are missing");
                 if (frameIndex == frameLength) {
                 frameReceived();
                 state = DecoderState.WaitingForStart;
                 }
                 */
                break;
            default:
                log.warn("Wrong state");
                state = DecoderState.WaitingForStart;

        }
    }

    @Override
    public void run() {

        try {
            int intValue;
            while ((intValue = inputstream.read()) != -1) {
                byteReceived(intValue);
            }
            log.warn("Stream finished");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

}
