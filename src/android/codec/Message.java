package com.appdinx.cardlink.codec;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.xml.bind.DatatypeConverter;


public class Message {
    private byte[] raw;

    public Message(byte[] raw) {
        this.raw = raw;
    }

    public byte[] getRaw() {
        return this.raw;
    }

    public void setRaw(byte[] raw) {
        this.raw = raw;
    }

    /**
     * 6B C-Kommando
     * 83 R-Kommando
     * 50 Ereignisnachricht
     * @return
     */
    public byte getMessageType() {
        return raw[0];
    }

    /**
     * Falls C-Kommando (´6B´): Zieladresse in network-
     * byte-order
     *   ´0000´ Falls SICCT Kommando APDU
     *   Falls ISO-7816 APDU:
     *   ´0001´ – ´00FF´ ICC 1 – ICC 255
     *   ´1001´ – ´10FF´ RFID 1 – RFID 255
     * Falls R-Kommando (´83´):Quelladresse in network-
     * byte-order
     *   ´0000´ Falls SICCT Kommando APDU
     *   Falls ISO-7816 APDU:
     *   ´0001´ – ´00FF´ ICC 1 – ICC 255
     *   ´1001´ – ´10FF´ RFID 1 – RFID 255
     * Falls Ereignisnachricht (´50´):
     *   Quelladresse in network-byte-order
     *   ´0000´ SICCT-Terminal Ereignis
     * @return
     */
    public byte[] getSrcOrDesAddr() {
        return new byte[] {raw[1], raw[2]};
    }

    /**
     * Sequenznummer des Kommandos in network-byte-
     * order
     * ´0000´ -´FCFF´ SICCT C- und R-Kommando
     * ´FD00´ -´FFFF´ SICCT Ereignis
     * @return
     */
    public byte[] getSeq() {
        return new byte[] {raw[3], raw[4]};
    }

    public int getLength() {
        return ByteBuffer.wrap(new byte[] {raw[6], raw[7], raw[8], raw[9]}).getInt();
    }

    public byte[] getBody() {
        byte[] body = Arrays.copyOfRange(raw, 10, 10+getLength());
        return body;
    }

    public byte getCLA() {
        return getBody()[0];
    }

    public byte getIns() {
        return getBody()[1];
    }

    public byte getP1() {
        return getBody()[2];
    }
    public byte getP2() {
        return getBody()[3];
    }
    public int getAPDULength() {
        byte[] apduData = getBody();
        return getAPDULength(apduData);
    }

    public static int getAPDULength(byte[] apduData) {
        if(apduData[4] != 0 && apduData.length > 5) {
            return apduData[4] & 0xff;
        } else if(apduData.length == 5) {
            return 0;
        } else if(apduData.length > 6) {
            byte[] body = apduData;

            return ByteBuffer.wrap(new byte[]{0x00, 0x00, body[5], body[6]}).getInt();
        } else {
            return 0;
        }
    }

    public byte[] getAPDUData() {
        byte[] body = getBody();
        return getAPDUData(body);
    }

    @NonNull
    public static byte[] getAPDUData(byte[] body) {
        if(body.length < 5) {
            return new byte[0];
        } else if(body[4] != 0 && body.length > 5) {
            return Arrays.copyOfRange(body, 5, getAPDULength(body) + 5);
        } else if(getAPDULength(body) > 0) {
            return Arrays.copyOfRange(body, 7, getAPDULength(body) + 7);
        } else {
            return new byte[0];
        }
    }

    /**
     * Inspired by https://github.com/openjdk-mirror/jdk7u-jdk/blob/master/src/share/classes/javax/smartcardio/CommandAPDU.java#L290 parse
     *
     * @return
     */
    public int getExpectedLength() {
        byte[] apdu = getBody();
        return expectedLength(apdu);
    }

    public static int expectedLength(byte[] apdu) {

        int l1 = apdu[4] & 0xff;

        if (apdu.length == 5) {
            // case 2s
            return (l1 == 0) ? 256 : l1;
        }

        if (l1 != 0) {
            if (apdu.length == 4 + 1 + l1) {
                return 0;
            } else if (apdu.length == 4 + 2 + l1) {
                int l2 = apdu[apdu.length - 1] & 0xff;
                return (l2 == 0) ? 256 : l2;
            } else {
                throw new IllegalArgumentException
                        ("Invalid APDU: length=" + apdu.length + ", b1=" + l1);
            }
        }
        int l2 = ((apdu[5] & 0xff) << 8) | (apdu[6] & 0xff);

        if (apdu.length == 7) {
            // case 2e
            return (l2 == 0) ? 65536 : l2;
        }

        if (l2 == 0) {
            throw new IllegalArgumentException("Invalid APDU: length="
                    + apdu.length + ", b1=" + l1 + ", b2||b3=" + l2);
        }

        if (apdu.length == 4 + 5 + l2) {
            int leOfs = apdu.length - 2;
            int l3 = ((apdu[leOfs] & 0xff) << 8) | (apdu[leOfs + 1] & 0xff);
            return (l3 == 0) ? 65536 : l3;
        } else {
            throw new IllegalArgumentException("Invalid APDU: length="
                    + apdu.length + ", b1=" + l1 + ", b2||b3=" + l2);
        }
    }


    public String toString() {
        return DatatypeConverter.printHexBinary(raw);
    }
}

