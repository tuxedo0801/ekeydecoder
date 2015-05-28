/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.root1.ekeydecoder;

/**
 *
 * @author achr
 */
public class RS232Config {
    
    public static final int BAUDRATE_300 = 300;
    public static final int BAUDRATE_600 = 600;
    public static final int BAUDRATE_1200 = 1200;
    public static final int BAUDRATE_2400 = 2400;
    public static final int BAUDRATE_4800 = 4800;
    public static final int BAUDRATE_9600 = 9600;
    public static final int BAUDRATE_14400 = 14400;
    public static final int BAUDRATE_19200 = 19200;
    public static final int BAUDRATE_28800 = 28800;
    public static final int BAUDRATE_38400 = 38400;
    public static final int BAUDRATE_57600 = 57600;
    public static final int BAUDRATE_76800 = 76800;
    public static final int BAUDRATE_115200 = 115200;
    
    public static final int DATABITS_5 = 5;
    public static final int DATABITS_6 = 6;
    public static final int DATABITS_7 = 7;
    public static final int DATABITS_8 = 8;
    
    public static final int PARITY_NONE = 0;
    public static final int PARITY_ODD = 1;
    public static final int PARITY_EVEN = 2;
    public static final int PARITY_MARK = 3;
    public static final int PARITY_SPACE = 4;
    
    public static final int STOPBITS_1 = 1;
    public static final int STOPBITS_2 = 2;
    public static final int STOPBITS_1_5 = 3;
    
    public static final int FLOWCONTROL_NONE = 0;
    public static final int FLOWCONTROL_RTSCTS_IN = 1;
    public static final int FLOWCONTROL_RTSCTS_OUT = 2;
    public static final int FLOWCONTROL_XONXOFF_IN = 4;
    public static final int FLOWCONTROL_XONXOFF_OUT = 8;
    
    private String portName = "/dev/ttyS0";
    private int baudrate = BAUDRATE_9600;
    private int databits = DATABITS_8;
    private int parity = PARITY_NONE;
    private int stopbits = STOPBITS_1;
    private int flowcontrol = FLOWCONTROL_NONE;
    private int timeout = 5000;

    /**
     * Default Config:
     * 
     * /dev/ttyS0, 9600-8-N-1, no handshaking or flowcontrol, 5000ms timeout
     */
    public RS232Config(){
        
    }
    
    public RS232Config(String portName, int baudrate, int databits, int parity, int stopbits, int flowcontrol, int timeout) {
        this.portName = portName;
        this.baudrate = baudrate;
        this.databits = databits;
        this.parity = parity;
        this.stopbits = stopbits;
        this.flowcontrol = flowcontrol;
        this.timeout = timeout;
    }

    public String getPortName() {
        return portName;
    }

    public void setPortName(String portName) {
        this.portName = portName;
    }

    public int getBaudrate() {
        return baudrate;
    }

    public void setBaudrate(int baudrate) {
        this.baudrate = baudrate;
    }

    public int getDatabits() {
        return databits;
    }

    public void setDatabits(int databits) {
        this.databits = databits;
    }

    public int getFlowcontrol() {
        return flowcontrol;
    }

    public void setFlowcontrol(int flowcontrol) {
        this.flowcontrol = flowcontrol;
    }

    public int getParity() {
        return parity;
    }

    public void setParity(int parity) {
        this.parity = parity;
    }

    public int getStopbits() {
        return stopbits;
    }

    public void setStopbits(int stopbits) {
        this.stopbits = stopbits;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public String toString() {
        return "RS232Config{" + "portName=" + portName + ", baudrate=" + baudrate + ", databits=" + databits + ", parity=" + parity + ", stopbits=" + stopbits + ", flowcontrol=" + flowcontrol + ", timeout=" + timeout + '}';
    }
    
    
    
}
