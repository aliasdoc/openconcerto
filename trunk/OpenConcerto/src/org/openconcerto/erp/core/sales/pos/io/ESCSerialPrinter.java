/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 OpenConcerto, by ILM Informatique. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the GNU General Public License Version 3
 * only ("GPL"). You may not use this file except in compliance with the License. You can obtain a
 * copy of the License at http://www.gnu.org/licenses/gpl-3.0.html See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each file.
 */
 
 package org.openconcerto.erp.core.sales.pos.io;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;

public class ESCSerialPrinter extends DefaultTicketPrinter {
    private static final int GS = 0x1D;
    private static final int ESC = 0x1B;
    private String port;

    /**
     * Valid port are COM1: , COM1 , /dev/ttyS0 or /dev/tty.mydevice
     * */
    public ESCSerialPrinter(String port) {
        port = port.trim();
        if (port.endsWith(":")) {
            port = port.substring(0, port.length() - 1);
        }
        this.port = port;
    }

    public void addToBuffer(String t) {
        addToBuffer(t, NORMAL);
    }

    public void addToBuffer(String t, int mode) {
        this.strings.add(t);
        this.modes.add(mode);
    }

    public synchronized void openDrawer() throws Exception {
        final SerialPort serialPort = getSerialPort();

        OutputStream out = serialPort.getOutputStream();
        boolean useESCP = false;
        if (useESCP) {
            // Pin 2, 200ms min
            out.write(ESC);
            out.write(0x70);
            out.write(0x00); // Pin 2
            out.write(100); // 2x100ms On
            out.write(100); // 2x100ms Off
            try {
                // 300ms to ensure opening
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Pin 2, 200ms min
            out.write(ESC);
            out.write(0x70);
            out.write(0x01); // Pin 5
            out.write(100); // 2x100ms On
            out.write(100); // 2x100ms Off
            try {
                // 300ms to ensure opening
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        // Pin 2, 200ms min
        out.write(0x10);// DLE
        out.write(0x14);// DC4
        out.write(0x01);
        out.write(0x00);// Pin 2
        out.write(0x02);
        // Vista 32bits bug: out.flush(); // Crash, works fine without

        try {
            // 300ms to ensure opening
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Pin 5, 200ms
        out.write(0x10);// DLE
        out.write(0x14);// DC4
        out.write(0x01);
        out.write(0x01);// Pin 5
        out.write(0x02);
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Vista 32bits bug: out.flush(); // Crash, works fine without
        out.close();

        serialPort.close();
    }

    public synchronized void printBuffer() throws Exception {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        // Init
        bOut.write(ESC);
        bOut.write(0x40);
        // French characters
        bOut.write(ESC);
        bOut.write(0x52);
        bOut.write(0x01);
        //

        final int size = this.strings.size();
        for (int i = 0; i < size; i++) {
            String string = this.strings.get(i);
            int mode = modes.get(i);

            if (mode == BARCODE) {
                //
                bOut.write(GS);
                bOut.write(0x48);
                bOut.write(0x02); // en bas

                //
                bOut.write(GS);
                bOut.write(0x77);
                bOut.write(0x02); // Zoom 2

                //
                bOut.write(GS);
                bOut.write(0x68);
                bOut.write(60); // Hauteur
                // Code 39
                bOut.write(GS);
                bOut.write(0x6B);
                bOut.write(0x04); // Code 39
                for (int k = 0; k < string.length(); k++) {
                    char c = string.charAt(k);

                    bOut.write(c);
                }
                bOut.write(0x00); // End
            } else {
                if (mode == NORMAL) {
                    bOut.write(ESC);
                    bOut.write(0x21);
                    bOut.write(0);// Default
                } else if (mode == BOLD) {
                    bOut.write(ESC);
                    bOut.write(0x21);
                    bOut.write(8);// Emphasis
                } else if (mode == BOLD_LARGE) {
                    bOut.write(GS);
                    bOut.write(0x21);
                    bOut.write(0x11);//
                }

                for (int k = 0; k < string.length(); k++) {
                    char c = string.charAt(k);
                    if (c == 'é') {
                        c = 130;
                    } else if (c == 'è') {
                        c = 138;
                    } else if (c == 'ê') {
                        c = 136;
                    } else if (c == 'ù') {
                        c = 151;
                    } else if (c == 'à') {
                        c = 133;
                    } else if (c == 'ç') {
                        c = 135;
                    } else if (c == 'ô') {
                        c = 147;
                    }
                    bOut.write(c);
                }
            }
            bOut.write(0x0A);// Retour a la ligne

        }
        // Eject
        bOut.write(0x0A);
        bOut.write(0x0A);
        bOut.write(0x0A);
        bOut.write(0x0A);
        // Coupe
        bOut.write(GS);
        bOut.write(0x56); // V
        bOut.write(0x01);

        // Do NOT flush or use BufferedOutputStream !
        final SerialPort serialPort = getSerialPort();
        final OutputStream out = serialPort.getOutputStream();
        out.write(bOut.toByteArray());
        out.close();
        serialPort.close();
    }

    private SerialPort getSerialPort() throws Exception {
        if (port == null || port.length() == 0) {
            throw new IllegalStateException("Invalid serial port name: " + port);
        }
        final CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(this.port);
        if (portIdentifier.isCurrentlyOwned()) {
            throw new IllegalAccessError("Port " + this.port + " is currently in use");
        }
        final int timeOutMs = 2000;
        final CommPort commPort = portIdentifier.open("ESCSerialPrinter", timeOutMs);
        if (!(commPort instanceof SerialPort)) {
            throw new IllegalStateException("Invalid serial port: " + port);
        }

        final SerialPort serialPort = (SerialPort) commPort;
        serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        return serialPort;
    }

    public static void main(String[] args) {
        listPorts();
        final ESCSerialPrinter prt = new ESCSerialPrinter("COM1");
        prt.setPort(getSerialPortNames().get(0));
        prt.addToBuffer("ILM INFORMATIQUE", BOLD_LARGE);
        prt.addToBuffer("");
        prt.addToBuffer("22 place de la liberation");
        prt.addToBuffer("80100 ABBEVILLE");
        prt.addToBuffer("Tél: 00 00 00 00 00");
        prt.addToBuffer("Fax: 00 00 00 00 00");
        prt.addToBuffer("");
        final SimpleDateFormat df = new SimpleDateFormat("EEEE d MMMM yyyy à HH:mm");
        prt.addToBuffer(formatRight(45, "Le " + df.format(Calendar.getInstance().getTime())));
        prt.addToBuffer("");
        prt.addToBuffer(formatRight(5, "3") + " " + formatLeft(30, "ILM Informatique") + " " + formatRight(8, "3.00"));
        prt.addToBuffer("      =======================================");
        prt.addToBuffer(formatRight(37, "Total") + formatRight(8, "3.00"), BOLD);
        prt.addToBuffer("");

        prt.addToBuffer("Merci de votre visite, à bientôt.");
        prt.addToBuffer("");
        prt.addToBuffer("01 05042010 00002", BARCODE);

        try {
            prt.printBuffer();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void setPort(String string) {
        this.port = string;

    }

    public static void listPorts() {
        @SuppressWarnings("unchecked")
        final Enumeration<CommPortIdentifier> portEnum = CommPortIdentifier.getPortIdentifiers();
        while (portEnum.hasMoreElements()) {
            CommPortIdentifier portIdentifier = portEnum.nextElement();
            System.out.println("Port: " + portIdentifier.getName() + " Type: " + getPortTypeName(portIdentifier.getPortType()));
        }
    }

    private static List<String> getSerialPortNames() {
        ArrayList<String> r = new ArrayList<String>();
        @SuppressWarnings("unchecked")
        Enumeration<CommPortIdentifier> portEnum = CommPortIdentifier.getPortIdentifiers();
        while (portEnum.hasMoreElements()) {
            CommPortIdentifier portIdentifier = portEnum.nextElement();
            System.out.println("Port: " + portIdentifier.getName() + " Type: " + getPortTypeName(portIdentifier.getPortType()));
            if (portIdentifier.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                r.add(portIdentifier.getName());
            }
        }
        return r;
    }

    private static String getPortTypeName(int portType) {
        switch (portType) {
        case CommPortIdentifier.PORT_I2C:
            return "I2C";
        case CommPortIdentifier.PORT_PARALLEL:
            return "Parallel";
        case CommPortIdentifier.PORT_RAW:
            return "Raw";
        case CommPortIdentifier.PORT_RS485:
            return "RS485";
        case CommPortIdentifier.PORT_SERIAL:
            return "Serial";
        default:
            return "unknown type";
        }
    }

}
