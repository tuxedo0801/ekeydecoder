/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.root1.ekeydecoder;

import gnu.io.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author achr
 */
public class RS232 {
    
    private SerialPortEventListener spel = new SerialPortEventListener() {

        @Override
        public void serialEvent(SerialPortEvent spe) {
            System.out.println("serialEvent: "+spe);
            if (spe.getEventType()==SerialPortEvent.DATA_AVAILABLE) {
                try {
                    int available = inputStream.available();
                    byte[] data = new byte[available];
                    inputStream.read(data);
                    int columns = 8;
                    int columnCount = 0;
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append("Received data:\n");
                    for (int i=0;i<data.length;i++) {
                        sb.append(" ").append(Integer.toHexString(data[i]));
                        columnCount++;
                        if (columnCount==columns) {
                            columnCount=0;
                            sb.append("\n");
                        }
                    }
                    sb.append("\n");
                    System.out.println(sb.toString());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    };
    
    private InputStream inputStream;
    private OutputStream outputStream;
    private SerialPort port;
    private CommPortIdentifier portIdentifier;
    
    public RS232(String appName, RS232Config config) {
        
        try {
            System.out.println("Config: "+config);
            portIdentifier = CommPortIdentifier.getPortIdentifier(config.getPortName());
            port = (SerialPort) portIdentifier.open(appName, config.getTimeout());
            port.setSerialPortParams(config.getBaudrate(), config.getDatabits(), config.getStopbits(), config.getParity());
            port.setFlowControlMode(config.getFlowcontrol());
            port.setOutputBufferSize(1);
            port.addEventListener(spel);
            inputStream = port.getInputStream();
            outputStream = port.getOutputStream();
        } catch (TooManyListenersException ex) {
            Logger.getLogger(RS232.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        } catch (IOException ex) {
            Logger.getLogger(RS232.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        } catch (PortInUseException ex) {
            Logger.getLogger(RS232.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        } catch (UnsupportedCommOperationException ex) {
            Logger.getLogger(RS232.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        } catch (NoSuchPortException ex) {
            Logger.getLogger(RS232.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }
        
    }
    
    public InputStream getInputStream() throws IllegalStateException {
        if (port==null) {
            throw new IllegalStateException("Port not opened");
        }
        return inputStream;
    }
    
    public OutputStream getOutputStream() throws IllegalStateException {
        if (port==null) {
            throw new IllegalStateException("Port not opened");
        }
        return outputStream;
    }

    public boolean isOpened() {
        return port!=null;
    }
    
    public void close() {
        if (port!=null) {
            port.removeEventListener();
            port.close();
        }
    }
    
}
