package com.official_yun.default_nfc;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Locale;

public class NfcProcess {

    public static final String CHARS = "0123456789ABCDEF";
    public static final int TYPE_TEXT = 1;
    public static final int TYPE_URI = 2;

    String byteArrayToHexString(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.length; ++i) {
            sb.append(CHARS.charAt((b[i] >> 4) & 0x0F)).append(CHARS.charAt(b[i] & 0x0F));
        }
        return sb.toString();
    }

    String getReadTagData(NdefMessage ndefmsg) {
        if(ndefmsg != null ) {
            NdefRecord[] records = ndefmsg.getRecords();
            for(NdefRecord rec : records) {
                byte [] payload = rec.getPayload() ;
                String textEncoding = "UTF-8" ;
                if(payload.length > 0){
                    textEncoding = ( payload[0] & 0200 ) == 0 ? "UTF-8" : "UTF-16";
                }

                Short tnf = rec.getTnf();
                String type = String.valueOf(rec.getType());
                String payloadStr = new String(rec.getPayload(), Charset.forName(textEncoding));
                return payloadStr;
            }
        }
        return "";
    }

    boolean writeTag(NdefMessage message, Tag tag) {
        int size = message.toByteArray().length;
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                if (!ndef.isWritable()) {
                    return false;
                }
                if (ndef.getMaxSize() < size) {
                    return false;
                }
                ndef.writeNdefMessage(message);
            } else {
                NdefFormatable formatable = NdefFormatable.get(tag);
                if (formatable != null) {
                    formatable.connect();
                    formatable.format(message);
                }
            }
        } catch (FormatException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    NdefMessage createTagMessage(String msg, int type) {
        NdefRecord[] records = new NdefRecord[1];
        if (type == TYPE_TEXT) {
            records[0] = createTextRecord(msg, Locale.KOREAN, true);
        } else if (type == TYPE_URI) {
            records[0] = createUriRecord(msg.getBytes());
        }
        NdefMessage mMessage = new NdefMessage(records);
        return mMessage;
    }

    NdefRecord createTextRecord(String text, Locale locale, boolean encodeInUtf8) {
        final byte[] langBytes = locale.getLanguage().getBytes(Charset.forName("US-ASCII"));
        final Charset utfEncoding = encodeInUtf8 ? Charset.forName("UTF-8") : Charset.forName("UTF-16");
        final byte[] textBytes = text.getBytes(utfEncoding);
        final int utfBit = encodeInUtf8 ? 0 : (1 << 7);
        final char status = (char) (utfBit + langBytes.length);
        byte[] data = new byte[1 + langBytes.length + textBytes.length];
        data[0] = (byte) status;
        System.arraycopy(langBytes, 0, data, 1, langBytes.length);
        System.arraycopy(textBytes, 0, data, 1 + langBytes.length, textBytes.length);
        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], data);
    }

    NdefRecord createUriRecord(byte[] data) {
        return new NdefRecord(NdefRecord.TNF_ABSOLUTE_URI, NdefRecord.RTD_URI,
                new byte[0], data);
    }
}
