/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.root1.ekeydecoder;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author achristian
 */
public class EkeyDecoderPortierung implements Runnable {

    /**
     * Puffergröße für empfangene RS485 Frames
     */
    private static final int DEC_BUFFER_MAX = 200;

    @Override
    public void run() {
        
        try {
            int intValue;
            while ((intValue = inputstream.read()) != -1) {
                ekeyDec_receivedByte(intValue);
            }
            System.out.println("Stream finished");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
    }

    private enum DecoderState {

        WaitingForStart, WaitingForLength, WaitingForLengthExtension, WaitingForContent
    }

    private DecoderState nDecoderState = DecoderState.WaitingForStart;
    int iDecoder;
    int DEC_Length;
    int[] DEC_Buffer = new int[DEC_BUFFER_MAX];
    int ekey_nFingerCounter;
    int ekey_nFrameCounter;
    int ekeyFingerOkTimer;

    private final InputStream inputstream;

    public EkeyDecoderPortierung(InputStream inputstream) {
        this.inputstream = inputstream;
        new Thread(this, "EkeyDecoder").start();
    }

    private void DEC_Unstuffing() {

        /*
         Byte-Stuffing rückgängig machen. Zum Transport wurden die Daten aufgeblasen, um das Auftreten der Beginn- und Endekennung
         innerhalb der Nutzerdaten zu vermeiden:
        
         02 --> 3F 41
         03 --> 3F 81
         3F --> 3F C1
         */

        /*
         Gehen wir mal davon aus, dass im Addressierungsteil (Index 0 bis 14) kein Byte-Stuffing verwendet wird. Das spart hier
         etwas Zeit zum Durchlaufen.
         */
        if (DEC_Length <= 15) {
            return;
        }

        boolean blMove;
        int k = 15;

        while (true) {
            if (DEC_Buffer[k] == 0x3F) {
                blMove = false;
                switch (DEC_Buffer[k + 1]) {
                    case 0x41:
                        DEC_Buffer[k] = (byte) 0x02;
                        blMove = true;
                        break;
                    case 0x81:
                        DEC_Buffer[k] = (byte) 0x03;
                        blMove = true;
                        break;
                    case 0xC1:
                        DEC_Buffer[k] = (byte) 0x3F;
                        blMove = true;
                        break;
                    /* Default: Falls irgend etwas anderes der 3F folgt, dann scheint die 3F ein normales Datenbyted zu sein --> kein Unstuffing. */
                }
                if (blMove) {
                    for (int i = k + 1; i < DEC_Length; i++) {
                        DEC_Buffer[i] = DEC_Buffer[i + 1];
                    }
                    DEC_Length--;
                }
            }
            k++;
            if (k >= DEC_Length) {
                return;
            }
        }

    }

    private void DEC_frameReceived() {
        
        /* Plausibilisierungen */
        if ((DEC_Buffer[0]!=0x02)) {
            System.out.println("Anfang falsch");
        }
        if (DEC_Buffer[DEC_Length-1]!=0x03) {
            System.out.println("Ende falsch");
        }
        
        DEC_Unstuffing();
        
        if (DEC_Length== 47){
            ekeyFingerOkTimer = 200; /* 200*5ms = 1sek "aktiv" melden */
            System.out.println("Fingerhash: "+String.format("0x%02x", DEC_Buffer[17]));
        }

    }

    // Hier kommen die empfangenen Bytes einzeln rein
    private void ekeyDec_receivedByte(int n) {
        switch (nDecoderState) {
            case WaitingForStart:
                if (n == 2) {
                    nDecoderState = DecoderState.WaitingForLength;
                    DEC_Buffer[0] = n;
                    System.out.println("Waiting for length");
                }
                break;
            case WaitingForLength:
                /* Längenbyte ist angekommen. Die eigentliche Länge der Nutzerdaten
                 ist (n-1)/4. Weitere Teule der Längeninformation stecken im nächsten Byte.
                 */
                DEC_Length = (n - 1) >> 2;
                /* Zusätzlich zur Nutzdatenlänge wollen wir noch "StartOfFrame" 
                 und Längenbyte im Trace haben --> zwei byte mehr.
                 */
                DEC_Length = DEC_Length + 2;
                nDecoderState = DecoderState.WaitingForLengthExtension;
                DEC_Buffer[1] = n;
                System.out.println("Waiting for LengthExtension");
                break;
            case WaitingForLengthExtension:
                /* Zweites Längenbyte ist angekommen. Bit 0 bedeutet 64.
                 Ob Bit 1 dann 128 bedeutet ist noch unklar.
                 */
                if ((n & 1) == 1) {
                    DEC_Length = DEC_Length + 64;
                }
                nDecoderState = DecoderState.WaitingForContent;
                DEC_Buffer[2] = n;
                System.out.println("Waiting for " + DEC_Length + " Bytes");
                iDecoder = 3;
                break;
            case WaitingForContent:
                DEC_Buffer[iDecoder] = n;
                iDecoder++;
                System.out.println("Received " + iDecoder + " bytes");
                if (iDecoder == DEC_Length) {
                    DEC_frameReceived();
                    nDecoderState = DecoderState.WaitingForStart;
                }
                break;
            default:
                System.out.println("Wrong state");
                nDecoderState = DecoderState.WaitingForStart;

        }
    }

}
