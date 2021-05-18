package com.squorpikkor.nautiz2;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import com.handheldgroup.serialport.SerialPort;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.Arrays;

import static com.squorpikkor.nautiz2.mbs.ADDRESS;
import static com.squorpikkor.nautiz2.mbs.CHANGE_STATE_CONTROL_REGISTER;
import static com.squorpikkor.nautiz2.mbs.CHANGE_STATE_CONTROL_REGISTERS;
import static com.squorpikkor.nautiz2.mbs.DEFAULT_MESSAGE_LENGTH;
import static com.squorpikkor.nautiz2.mbs.DIAGNOSTICS;
import static com.squorpikkor.nautiz2.mbs.MESSAGE_DEFAULT_LENGTH;
import static com.squorpikkor.nautiz2.mbs.MESSAGE_LONG_LENGTH;
import static com.squorpikkor.nautiz2.mbs.MESSAGE_MID_LENGTH;
import static com.squorpikkor.nautiz2.mbs.READ_ACCUMULATED_SPECTRUM;
import static com.squorpikkor.nautiz2.mbs.READ_ACCUMULATED_SPECTRUM_COMPRESSED;
import static com.squorpikkor.nautiz2.mbs.READ_ACCUMULATED_SPECTRUM_COMPRESSED_REBOOT;
import static com.squorpikkor.nautiz2.mbs.READ_DEVICE_ID;
import static com.squorpikkor.nautiz2.mbs.READ_STATUS_WORD;
import static com.squorpikkor.nautiz2.mbs.SEND_CONTROL_SIGNAL;
import static com.squorpikkor.nautiz2.mbs.TIMEOUT_DEFAULT;
import static com.squorpikkor.nautiz2.mbs.getCRCOrder;
import static com.squorpikkor.nautiz2.mbs.getMessageWithCRC16;

public class MainActivity extends AppCompatActivity {

    SerialPort mSerialPort;
    OutputStream mOutputStream;
    InputStream mInputStream;

    private final byte[] buffer = new byte[3084];

    private static final String TAG = "tag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.get_id).setOnClickListener(v -> {
           getId();
        });

        initSerialPort();
    }

    private void getId() {
        method();
    }

    void initSerialPort() {
        Log.e(TAG, "♦ -----initSerialPort-----");
        try {
            mSerialPort = new SerialPort(new File("/dev/ttyHSL1"), 230400, 0);
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();

        } catch (SecurityException e) {
            //DisplayError(R.string.error_security);
        } catch (IOException e) {
            //DisplayError(R.string.error_unknown);
        } catch (InvalidParameterException e) {
            //DisplayError(R.string.error_configuration);
        }
    }

    public static ModbusMessage createModbusMessage(int messageLength, byte address, byte command,
                                                    byte[] data, boolean crcOrder) {
        ByteBuffer buffer = ByteBuffer.allocate(messageLength)
                .put(address)
                .put(command);
        if (data != null) {
            buffer.put(data);
        }

        ModbusMessage message = new ModbusMessage(getMessageWithCRC16(buffer.array(), crcOrder));
        message.mIntegrity = true;
        message.mException = false;
        return message;
    }

    void method() {
        ModbusMessage response = sendMessageWithResponse(createModbusMessage(DEFAULT_MESSAGE_LENGTH, ADDRESS, READ_DEVICE_ID,
                null, getCRCOrder()));
//        if (response!=null&&response.getBuffer()!=null&&response.getBuffer().length!=0)Log.e(TAG, "--method: "+response.getBuffer()[0]);
//        if (response!=null&&response.getBuffer()!=null&&response.getBuffer().length!=0)Log.e(TAG, "--method: "+response.getBuffer()[1]);
    }

    public synchronized ModbusMessage sendMessageWithResponse(ModbusMessage message) {
        sendMessage(message);
        Log.e(TAG, "sendMessageWithResponse: "+ Arrays.toString(message.getBuffer()));
        return receiveMessage();
    }

    ModbusMessage receiveMessage() {
        ModbusMessage responseMessage = new ModbusMessage(receiveByteMessage());
        Log.e(TAG, "Response " + Arrays.toString(responseMessage.getBuffer()));
        return responseMessage;
    }

    public void sendMessage(ModbusMessage message){
        try {
            sendMessage2(message.getBuffer());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Connection lost while executing command "+message.getBuffer()[1]);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    void sendMessage2(byte[] message) throws IOException {
        ////mSerialPort.getInputStream().skip(mSerialPort.getInputStream().available());
        mInputStream.skip(mInputStream.available());//Permission denied
        mSerialPort.getOutputStream().write(message);
    }

    public byte[] receiveByteMessage_(){
        byte[] buffer = this.buffer;
        int currentPosition = 0;
        long startTime = System.currentTimeMillis();
        int totalByte = MESSAGE_DEFAULT_LENGTH;

        //until time passes or we get all the bytes
        while ((System.currentTimeMillis() - startTime < TIMEOUT_DEFAULT) && currentPosition != totalByte) {
            try {
                if (mInputStream.available() > 0) {
                    int x = mInputStream.read();
                    buffer[currentPosition] = (byte) x;
                    currentPosition++;
                }
            } catch (Exception e) {
                Log.e(TAG, "!!!!!"+e.getMessage());
                return null;
            }
        }
        return Arrays.copyOf(buffer, currentPosition);
    }

    public byte[] receiveByteMessage() {
        byte[] buffer = this.buffer;
        int currentPosition = 0;
        long startTime = System.currentTimeMillis();
        int totalByte = MESSAGE_DEFAULT_LENGTH;

        //until time passes or we get all the bytes
        while ((System.currentTimeMillis() - startTime < TIMEOUT_DEFAULT) && currentPosition != totalByte) {
            try {
                if (mInputStream.available() > 0) {
                    int x = mInputStream.read();
                    buffer[currentPosition] = (byte) x;
                    currentPosition++;
                    //position 1 contains contains command byte
                    //We can figure out the number of bytes in message according to command byte
                    if (currentPosition == 3) {
                        if (buffer[1] == DIAGNOSTICS
                                || buffer[1] == SEND_CONTROL_SIGNAL
                                || buffer[1] == CHANGE_STATE_CONTROL_REGISTER
                                || buffer[1] == CHANGE_STATE_CONTROL_REGISTERS) {
                            totalByte = MESSAGE_LONG_LENGTH;
                        } else if (buffer[1] == READ_STATUS_WORD) {
                            totalByte = MESSAGE_DEFAULT_LENGTH;
                        } else {
                            totalByte = (buffer[2] & 255) + MESSAGE_DEFAULT_LENGTH;
                        }
                    } else if (currentPosition == 4 &&
                            (buffer[1] == READ_ACCUMULATED_SPECTRUM
                                    || buffer[1] == READ_ACCUMULATED_SPECTRUM_COMPRESSED_REBOOT
                                    || buffer[1] == READ_ACCUMULATED_SPECTRUM_COMPRESSED)) {
                        //in these command 3 and 4 bytes shows number of data bytes in message
                        int lengthData = BitConverter.toInt16(new byte[]{buffer[3], buffer[2]}, 0);
                        totalByte = lengthData + MESSAGE_MID_LENGTH;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "!!!!!"+e.getMessage());
                return null;
            }
        }
        return Arrays.copyOf(buffer, currentPosition);
    }
}