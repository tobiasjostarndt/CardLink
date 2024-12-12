/*
Generated using https://github.com/yafred/asn1-tool v0.0.32
CardTerminalMessage ::= SEQUENCE {
   deviceId UTF8String,
   message OCTET STRING,
   canNumber UTF8String OPTIONAL
}
*/
package com.appdinx.cardlink.artemis;

public class CardTerminalMessage {
    private String _deviceId;

    public String getDeviceId() {
        return _deviceId;
    }

    public void setDeviceId(String _deviceId) {
        this._deviceId = _deviceId;
    }

    private byte[] _message;

    public byte[] getMessage() {
        return _message;
    }

    public void setMessage(byte[] _message) {
        this._message = _message;
    }

    private boolean apdu = true;

    public boolean isApdu() {
        return apdu;
    }

    public void setApdu(boolean apdu) {
        this.apdu = apdu;
    }

    private String _canNumber;

    public String getCanNumber() {
        return _canNumber;
    }

    public void setCanNumber(String _canNumber) {
        this._canNumber = _canNumber;
    }

    public static CardTerminalMessage readPdu(com.yafred.asn1.runtime.BERReader reader) throws Exception {
        reader.readTag();
        reader.mustMatchTag(new byte[]{48 /*0x30*/}); /* CONSTRUCTED_UNIVERSAL_16 */
        reader.readLength();
        CardTerminalMessage ret = new CardTerminalMessage();
        read(ret, reader, reader.getLengthValue());
        return ret;
    }

    public static void writePdu(CardTerminalMessage pdu, com.yafred.asn1.runtime.BERWriter writer) throws Exception {
        int componentLength = write(pdu, writer);
        componentLength += writer.writeLength(componentLength);
        componentLength += writer.writeOctetString(new byte[]{48 /*0x30*/}); /* CONSTRUCTED_UNIVERSAL_16 */
        writer.flush();
    }

    public static int write(CardTerminalMessage instance, com.yafred.asn1.runtime.BERWriter writer) throws Exception {
        int length = 0;
        if (instance.getCanNumber() != null) {
            int componentLength = 0;
            componentLength = writer.writeRestrictedCharacterString(instance.getCanNumber());
            componentLength += writer.writeLength(componentLength);
            componentLength += writer.writeOctetString(new byte[]{12 /*0x0c*/}); /* PRIMITIVE_UNIVERSAL_12 */
            length += componentLength;
        }
        if (instance.getMessage() != null) {
            int componentLength = 0;
            componentLength = writer.writeOctetString(instance.getMessage());
            componentLength += writer.writeLength(componentLength);
            componentLength += writer.writeOctetString(new byte[]{4 /*0x04*/}); /* PRIMITIVE_UNIVERSAL_4 */
            length += componentLength;
        }
        if (instance.getDeviceId() != null) {
            int componentLength = 0;
            componentLength = writer.writeRestrictedCharacterString(instance.getDeviceId());
            componentLength += writer.writeLength(componentLength);
            componentLength += writer.writeOctetString(new byte[]{12 /*0x0c*/}); /* PRIMITIVE_UNIVERSAL_12 */
            length += componentLength;
        }
        return length;
    }

    public static void read(CardTerminalMessage instance, com.yafred.asn1.runtime.BERReader reader, int length) throws Exception {
        int componentLength = 0;
        if (length == 0) return;
        reader.readTag();
        if (length != -1) length -= reader.getTagLength();
        reader.mustMatchTag(new byte[]{12 /*0x0c*/}); /* PRIMITIVE_UNIVERSAL_12 */
        reader.readLength();
        if (length != -1) length -= reader.getLengthLength();
        componentLength = reader.getLengthValue();
        instance.setDeviceId(reader.readRestrictedCharacterString(componentLength));
        if (length != -1) length -= componentLength;
        if (length == 0) return;
        if (reader.isTagMatched()) {
            reader.readTag();
            if (length != -1) length -= reader.getTagLength();
        }
        reader.mustMatchTag(new byte[]{4 /*0x04*/}); /* PRIMITIVE_UNIVERSAL_4 */
        reader.readLength();
        if (length != -1) length -= reader.getLengthLength();
        componentLength = reader.getLengthValue();
        instance.setMessage(reader.readOctetString(componentLength));
        if (length != -1) length -= componentLength;
        if (length == 0) return;
        if (reader.isTagMatched()) {
            reader.readTag();
            if (length != -1) length -= reader.getTagLength();
        }
        if (length == -1 && reader.matchTag(new byte[]{0})) {
            reader.mustReadZeroLength();
            return;
        }
        reader.matchTag(new byte[]{12 /*0x0c*/}); /* PRIMITIVE_UNIVERSAL_12 */
        if (reader.isTagMatched()) {
            reader.readLength();
            if (length != -1) length -= reader.getLengthLength();
        }
        if (reader.isTagMatched()) {
            componentLength = reader.getLengthValue();
            instance.setCanNumber(reader.readRestrictedCharacterString(componentLength));
            if (length != -1) length -= componentLength;
        }
        if (length == -1) {
            reader.readTag();
            reader.mustMatchTag(new byte[]{0});
            reader.mustReadZeroLength();
        } else if (length != 0) throw new Exception("length should be 0, not " + length);
        return;
    }

    public static CardTerminalMessage readPdu(com.yafred.asn1.runtime.ASNValueReader reader) throws Exception {
        CardTerminalMessage ret = new CardTerminalMessage();
        read(ret, reader);
        return ret;
    }

    public static void writePdu(CardTerminalMessage pdu, com.yafred.asn1.runtime.ASNValueWriter writer) throws Exception {
        write(pdu, writer);
    }

    public static void write(CardTerminalMessage instance, com.yafred.asn1.runtime.ASNValueWriter writer) throws Exception {
        writer.beginSequence();
        if (instance.getDeviceId() != null) {
            writer.writeComponent("deviceId");
            writer.writeRestrictedCharacterString(instance.getDeviceId());
        }
        if (instance.getMessage() != null) {
            writer.writeComponent("message");
            writer.writeOctetString(instance.getMessage());
        }
        if (instance.getCanNumber() != null) {
            writer.writeComponent("canNumber");
            writer.writeRestrictedCharacterString(instance.getCanNumber());
        }
        writer.endSequence();
    }

    public static void read(CardTerminalMessage instance, com.yafred.asn1.runtime.ASNValueReader reader) throws Exception {
        String componentName = null;
        reader.readToken(); // read '{'
        if ("}".equals(reader.lookAheadToken())) { // empty sequence
            reader.readToken();
            return;
        }
        if (componentName == null) componentName = reader.readIdentifier();
        if (componentName.equals("deviceId")) {
            instance.setDeviceId(reader.readRestrictedCharacterString());
            if ("}".equals(reader.readToken())) { // read ',' or '}'
                return;
            }
            componentName = null;
        } else {
            throw new Exception("Expecting deviceId (not OPTIONAL)");
        }
        if (componentName == null) componentName = reader.readIdentifier();
        if (componentName.equals("message")) {
            instance.setMessage(reader.readOctetString());
            if ("}".equals(reader.readToken())) { // read ',' or '}'
                return;
            }
            componentName = null;
        } else {
            throw new Exception("Expecting message (not OPTIONAL)");
        }
        if (componentName == null) componentName = reader.readIdentifier();
        if (componentName.equals("canNumber")) {
            instance.setCanNumber(reader.readRestrictedCharacterString());
            if ("}".equals(reader.readToken())) { // read ',' or '}'
                return;
            }
            componentName = null;
        }
        if (componentName != null) throw new Exception("Unexpected component " + componentName);
    }

    public static void validate(CardTerminalMessage instance) throws Exception {
    }
}
