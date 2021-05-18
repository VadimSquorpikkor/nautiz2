package com.squorpikkor.nautiz2;

class ModbusMessage {

    private byte[] mBuffer;

    public boolean mIntegrity;
    public boolean mException;

    public ModbusMessage(byte[] buffer) {
        this.mBuffer = buffer;
        mIntegrity = true;
        mException = false;
    }

    public byte[] getBuffer() {
        return mBuffer;
    }

}
