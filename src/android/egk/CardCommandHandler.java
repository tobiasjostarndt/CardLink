package com.appdinx.cardlink.egk;

import android.icu.text.MessagePattern;
import android.util.Log;

import androidx.annotation.NonNull;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.bind.DatatypeConverter;

import de.gematik.ti.cardreader.provider.api.card.CardException;
import de.gematik.ti.cardreader.provider.api.card.ICard;
import de.gematik.ti.cardreader.provider.api.card.ICardChannel;
import de.gematik.ti.cardreader.provider.api.command.AbstractApdu;
import de.gematik.ti.cardreader.provider.api.command.CommandApdu;
import de.gematik.ti.cardreader.provider.api.command.IResponseApdu;
import de.gematik.ti.cardreader.provider.api.command.ResponseApdu;
import de.gematik.ti.healthcard.control.common.pace.TrustedChannelPaceKeyExchange;
import de.gematik.ti.healthcardaccess.HealthCard;
import de.gematik.ti.healthcardaccess.exceptions.runtime.BasicChannelException;
import de.gematik.ti.healthcardaccess.operation.Subscriber;
import de.gematik.ti.openhealthcard.events.request.RequestPaceKeyEvent;
import de.gematik.ti.openhealthcard.events.response.entities.PaceKey;
import com.appdinx.cardlink.activities.CardInsertionActivity;
import com.appdinx.cardlink.artemis.ClientManager;
import com.appdinx.cardlink.codec.Message;
import com.appdinx.cardlink.util.DeviceUtils;

public class CardCommandHandler {

    private static final Logger log = Logger.getLogger(CardCommandHandler.class.getName());

    private static final String TAG = "PaceKey";

    private static HealthCard healthCard;

    private final String canNumber;

    private static CardInsertionActivity cardInsertionActivity;

    static Map<String, List<APDUTiming>> apduTimings = new HashMap<>();

    public CardCommandHandler(String canNumber, CardInsertionActivity cardInsertionActivity) {
        this.canNumber = canNumber;
        CardCommandHandler.cardInsertionActivity = cardInsertionActivity;
    }

    @Subscribe
    public void onPaceKeyEvent(RequestPaceKeyEvent event) {

        ICard card = event.getCard();
        try {
            healthCard = new HealthCard(card);

            cardInsertionActivity.setStatusText("Karte gefunden");

            new TrustedChannelPaceKeyExchange(healthCard, canNumber).negotiatePaceKey().subscribe(
                    new Subscriber<PaceKey>() {
                        @Override
                        public void onSuccess(final PaceKey paceKey) {
                            CardCommandHandler.cardInsertionActivity.runOnUiThread(() -> {
                                CardCommandHandler.cardInsertionActivity.setStatusText("Verschlüsselung aktiviert");
                            });
                            event.getResponseListener()
                                    .handlePaceKey(paceKey);
                            try {
                                ClientManager clientManager = ClientManager.getInstance();

                                byte[] fcp = sendCommand("SELECT, AID, first occurrence, Antwortdaten mit FCP", parseHexBinary("00a40404000000")).getData();

                                // short file identifier 0x1D
                                byte[] atr = sendCommand("Read Binary EF.ATR", parseHexBinary("00b09d00000000")).getData();
                                // short file identifier 0x02
                                byte[] gdo = sendCommand("Read Binary EF.GDO", parseHexBinary("00b08200000000")).getData();
                                // short file identifier 0x11
                                byte[] cardVersion = sendCommand("Read EF.Version2", parseHexBinary("00b09100000000")).getData();


                                sendCommand("SELECT FILE EF ESIGN", parseHexBinary("00a4040c0aa000000167455349474e"));

                                byte[] x509AuthRSA1 = sendCommand("Read Binary MF / DF.ESIGN / EF.C.CH.AUT.R2048 (first)", parseHexBinary("00b08100000000")).getData();
                                byte[] x509AuthRSA2 = sendCommand("Read Binary MF / DF.ESIGN / EF.C.CH.AUT.R2048 (second)", parseHexBinary("00b004cf1b")).getData();
                                byte[] x509AuthRSA = new byte[x509AuthRSA1.length + x509AuthRSA2.length];
                                ByteBuffer buffer = ByteBuffer.wrap(x509AuthRSA);
                                buffer.put(x509AuthRSA1);
                                buffer.put(x509AuthRSA2);
                                x509AuthRSA = buffer.array();

                                byte[] x509AuthECC = sendCommand("Read Binary MF / DF.ESIGN / EF.C.CH.AUT.E256", parseHexBinary("00b08400000000")).getData();

                                sendCommand("SELECT FILE MF", parseHexBinary("00a4040c07d2760001448000"));
                                byte[] cvcAuth = sendCommand("READ BINARY MF / EF.C.eGK.AUT_CVC.E256", parseHexBinary("00b08600000000")).getData();
                                byte[] cvcCA = sendCommand("READ BINARY MF / EF.C.CA.CS.E256", parseHexBinary("00b08700000000")).getData();


                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("cardSessionId", clientManager.getCardSessionId());
                                jsonObject.put("gdo", printBase64Binary(gdo));
                                jsonObject.put("atr", printBase64Binary(atr));
                                jsonObject.put("cardVersion", printBase64Binary(cardVersion));
                                jsonObject.put("x509AuthRSA", printBase64Binary(x509AuthRSA));
                                jsonObject.put("x509AuthECC", printBase64Binary(x509AuthECC));
                                jsonObject.put("cvcAuth", printBase64Binary(cvcAuth));
                                jsonObject.put("cvcCA", printBase64Binary(cvcCA));
                                jsonObject.put("client", "COM");
                                String json = jsonObject.toString();

                                Log.d("registerEgk", json);

                                clientManager.sendCardInsertedMessage(DeviceUtils.getDeviceId(cardInsertionActivity), json);
                            } catch (Exception e) {
                                log.log(Level.SEVERE, "Error communicating with the server: ", e);
                                cardInsertionActivity.setStatusText("Kann Karte nicht an Server senden");
                            }
                        }

                        @Override
                        public void onError(final Throwable t) throws RuntimeException {
                            Log.e(TAG, "PaceKey negotiation failed! " + t.getMessage());
                            if (Objects.requireNonNull(t.getMessage()).contains("AUTHENTICATION_FAILURE")) {
                                CardCommandHandler.cardInsertionActivity.runOnUiThread(() -> {
                                    CardCommandHandler.cardInsertionActivity.setStatusText("Falsche PIN");
                                });
                            } else {
                                CardCommandHandler.cardInsertionActivity.runOnUiThread(() -> {
                                    CardCommandHandler.cardInsertionActivity.setStatusText("Verschlüsselung fehlgeschlagen");
                                });
                            }
                        }
                    });
        } catch (BasicChannelException bce) {
            Log.e(TAG, "Card can't be read", bce);
            CardCommandHandler.cardInsertionActivity.runOnUiThread(() -> {
                CardCommandHandler.cardInsertionActivity.setStatusText("Karte nicht lesbar");
            });
        }
    }

    // Taken from https://github.com/javaee/jaxb-spec/blob/master/jaxb-api/src/main/java/javax/xml/bind/DatatypeConverterImpl.java#L76
    public byte[] parseHexBinary(String s) {
        final int len = s.length();

        // "111" is not a valid hex encoding.
        if (len % 2 != 0) {
            throw new IllegalArgumentException("hexBinary needs to be even-length: " + s);
        }

        byte[] out = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            int h = hexToBin(s.charAt(i));
            int l = hexToBin(s.charAt(i + 1));
            if (h == -1 || l == -1) {
                throw new IllegalArgumentException("contains illegal character for hexBinary: " + s);
            }

            out[i / 2] = (byte) (h * 16 + l);
        }

        return out;
    }

    private static int hexToBin(char ch) {
        if ('0' <= ch && ch <= '9') {
            return ch - '0';
        }
        if ('A' <= ch && ch <= 'F') {
            return ch - 'A' + 10;
        }
        if ('a' <= ch && ch <= 'f') {
            return ch - 'a' + 10;
        }
        return -1;
    }

    public static String printBase64Binary(byte[] input) {
        return _printBase64Binary(input);
    }

    public static String _printBase64Binary(byte[] input) {
        return _printBase64Binary(input, 0, input.length);
    }

    public static String _printBase64Binary(byte[] input, int offset, int len) {
        char[] buf = new char[((len + 2) / 3) * 4];
        int ptr = _printBase64Binary(input, offset, len, buf, 0);
        assert ptr == buf.length;
        return new String(buf);
    }

    /**
     * Encodes a byte array into a char array by doing base64 encoding.
     *
     * The caller must supply a big enough buffer.
     *
     * @return
     *      the value of {@code ptr+((len+2)/3)*4}, which is the new offset
     *      in the output buffer where the further bytes should be placed.
     */
    public static int _printBase64Binary(byte[] input, int offset, int len, char[] buf, int ptr) {
        // encode elements until only 1 or 2 elements are left to encode
        int remaining = len;
        int i;
        for (i = offset;remaining >= 3; remaining -= 3, i += 3) {
            buf[ptr++] = encode(input[i] >> 2);
            buf[ptr++] = encode(
                    ((input[i] & 0x3) << 4)
                            | ((input[i + 1] >> 4) & 0xF));
            buf[ptr++] = encode(
                    ((input[i + 1] & 0xF) << 2)
                            | ((input[i + 2] >> 6) & 0x3));
            buf[ptr++] = encode(input[i + 2] & 0x3F);
        }
        // encode when exactly 1 element (left) to encode
        if (remaining == 1) {
            buf[ptr++] = encode(input[i] >> 2);
            buf[ptr++] = encode(((input[i]) & 0x3) << 4);
            buf[ptr++] = '=';
            buf[ptr++] = '=';
        }
        // encode when exactly 2 elements (left) to encode
        if (remaining == 2) {
            buf[ptr++] = encode(input[i] >> 2);
            buf[ptr++] = encode(((input[i] & 0x3) << 4)
                    | ((input[i + 1] >> 4) & 0xF));
            buf[ptr++] = encode((input[i + 1] & 0xF) << 2);
            buf[ptr++] = '=';
        }
        return ptr;
    }

    private static final char[] encodeMap = initEncodeMap();

    private static char[] initEncodeMap() {
        char[] map = new char[64];
        int i;
        for (i = 0; i < 26; i++) {
            map[i] = (char) ('A' + i);
        }
        for (i = 26; i < 52; i++) {
            map[i] = (char) ('a' + (i - 26));
        }
        for (i = 52; i < 62; i++) {
            map[i] = (char) ('0' + (i - 52));
        }
        map[62] = '+';
        map[63] = '/';

        return map;
    }

    public static char encode(int i) {
        return encodeMap[i & 0x3F];
    }

    private static final char[] hexCode = "0123456789ABCDEF".toCharArray();

    public static String printHexBinary(byte[] data) {
        StringBuilder r = new StringBuilder(data.length * 2);
        for (byte b : data) {
            r.append(hexCode[(b >> 4) & 0xF]);
            r.append(hexCode[(b & 0xF)]);
        }
        return r.toString();
    }

    public static IResponseApdu sendCommand(byte[] sicctWithApduMessage) {
        try {
            if (healthCard == null) {
                Log.w(TAG, "healthCard is null");
                cardInsertionActivity.runOnUiThread(() -> {
                    cardInsertionActivity.setStatusText("Karte wurde entfernt");
                });
                return new ResponseApdu(new byte[]{0x62, 0x00});
            }


            CommandApdu command = unwrapCommandApdu(sicctWithApduMessage);

            IResponseApdu response = sendCommand(command);

            if(response.getSW1() != 0x90 && response.getSW2() != 0x00) {
                Log.w("Smartcard", "Could not execute command "+printHexBinary(sicctWithApduMessage));
            }

            return response;

        } catch (Exception e) {
            // Handle card connection or APDU transmission errors
            Log.e(TAG, "Could not communicate with card", e);
            cardInsertionActivity.runOnUiThread(() -> {
                cardInsertionActivity.setStatusText("Fehler bei Karte lesen");
            });
            return new ResponseApdu(new byte[]{0x62, 0x00});
        }
    }

    private static IResponseApdu sendCommand(String s, byte[] command) throws CardException {
        CommandApdu commandApdu = unwrapCommandApdu(command);
        IResponseApdu responseApdu = sendCommand(commandApdu);
        return responseApdu;
    }

    private static IResponseApdu sendCommand(CommandApdu command) throws CardException {
        // Open a basic channel
        ICardChannel channel = healthCard.getCurrentCardChannel();
        byte CLA = (byte) command.getCla();
        byte INS = (byte) command.getIns();
        byte P1 = (byte) command.getP1();
        byte P2 = (byte) command.getP2();

        String apduPrefix = printHexBinary(new byte[]{
                CLA, INS, P1, P2
        });
        APDUTiming apduTiming = new APDUTiming(apduPrefix);

        apduTimings.computeIfAbsent(apduPrefix, key -> new ArrayList<>()).add(apduTiming);

        // Transmit the command and receive the response
        IResponseApdu response = channel.transmit(command);
        apduTiming.stop();
        return response;
    }

    public static String outputTimings() {
        return apduTimings.entrySet().stream()
                .flatMap(e -> e.getValue().stream())
                .map(APDUTiming::toString).collect(Collectors.joining("\n"));
    }

    private static CommandApdu unwrapCommandApdu(byte[] apduMessage) {
        CommandApdu commandApdu;
        byte[] apduData = Message.getAPDUData(apduMessage);
        if (apduData != null && apduData.length > 0) {
            Integer ne = (apduMessage[0] == (byte) 0x00 && apduMessage[1] == (byte) 0x88 && (apduMessage.length - 6) == Message.getAPDULength(apduMessage)) ? (int) apduMessage[apduMessage.length - 1] : null;
            commandApdu = new CommandApdu(apduMessage[0] & 0xFF, apduMessage[1] & 0xFF, apduMessage[2] & 0xFF, apduMessage[3] & 0xFF, apduData, ne);
        } else if(apduMessage.length == 4) {
            commandApdu = new CommandApdu(apduMessage[0] & 0xFF, apduMessage[1] & 0xFF, apduMessage[2] & 0xFF, apduMessage[3] & 0xFF);
        } else {
            commandApdu = new CommandApdu(apduMessage[0] & 0xFF, apduMessage[1] & 0xFF, apduMessage[2] & 0xFF, apduMessage[3] & 0xFF, apduMessage.length == 5 ? Message.expectedLength(apduMessage) : AbstractApdu.EXPECTED_LENGTH_WILDCARD_EXTENDED);
        }
        return commandApdu;
    }

}

