package com.squorpikkor.nautiz2;

import java.nio.ByteBuffer;

public class mbs {

    public static byte[] getMessageWithCRC16(byte[] bytes, boolean crcOrder) {
        short crc = (short) calcCRC(bytes);
        if (crcOrder) {
            crc = ByteSwapper.swap(crc);
        }
        byte[] bytesCRC = BitConverter.getBytes(crc);
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length + bytesCRC.length);
        buffer.put(bytes);
        buffer.put(bytesCRC);
        return buffer.array();
    }


    public static int calcCRC(byte[] dataBuffer) {
        int sum = 0xffff;
        for (byte aDataBuffer : dataBuffer) {
            sum = (sum ^ (aDataBuffer & 255));
            for (int j = 0; j < 8; j++) {
                if ((sum & 0x1) == 1) {
                    sum >>>= 1;
                    sum = (sum ^ 0xA001);
                } else {
                    sum >>>= 1;
                }
            }
        }
        return sum;
    }



    public static final byte READ_DEVICE_ID = 0x11;
    public static final byte READ_STATE_DATA_REGISTERS = 0x07;
    public static final byte ADDRESS = 0x02;
    public static final int DEFAULT_MESSAGE_LENGTH = 2;
    public static final int MESSAGE_DEFAULT_LENGTH = 5;
    public static final int TIMEOUT_DEFAULT = 1500;

    public static final byte DIAGNOSTICS = 0x08;
    public static final byte SEND_CONTROL_SIGNAL = 0x05;
    public static final byte CHANGE_STATE_CONTROL_REGISTER = 0x06;
    public static final byte CHANGE_STATE_CONTROL_REGISTERS = 0x10;
    public static final int MESSAGE_LONG_LENGTH = 8;
    public static final byte READ_STATUS_WORD = 0x07;
    public static final byte READ_ACCUMULATED_SPECTRUM = 0x40;
    public static final byte READ_ACCUMULATED_SPECTRUM_COMPRESSED_REBOOT = 0x41;
    public static final byte READ_ACCUMULATED_SPECTRUM_COMPRESSED = 0x42;
    public static final int MESSAGE_MID_LENGTH = 6;

    public static boolean getCRCOrder() {
        return true;
    }


}
