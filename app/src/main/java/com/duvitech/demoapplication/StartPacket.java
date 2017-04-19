package com.duvitech.demoapplication;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by gvige on 4/19/2017.
 */

public class StartPacket {

    private static final String TAG = "StartPacket";

    protected ByteBuffer mData = ByteBuffer.allocate(16);

    public StartPacket(){
        mData.put(0, (byte)0xCA); // Start Byte
        mData.put(1, (byte)0x00); // reserved
        mData.put(2, (byte)0x00); // reserved
        mData.put(3, (byte)0x00); // reserved
        mData.put(4, (byte)0x00); // reserved
        mData.put(5, (byte)0x00); // reserved
        mData.put(6, (byte)0x00); // reserved
        mData.put(7, (byte)0x00); // reserved
    }

    public int getDataLength() {
        return mData.order(ByteOrder.LITTLE_ENDIAN).getInt(8);
    }
    public void setDataLength(int val) {
        mData.order(ByteOrder.LITTLE_ENDIAN).putInt(8, val);
    }


    public void setCrcValue(int val) {
        mData.order(ByteOrder.LITTLE_ENDIAN).putInt(12, val);
    }

    public int getCrcValue() {
        return mData.order(ByteOrder.LITTLE_ENDIAN).getInt(12);
    }


    public void clearStructure(){
        mData.reset();

        mData.put(0, (byte)0xCA); // Start Byte
        mData.put(1, (byte)0x00); // reserved
        mData.put(2, (byte)0x00); // reserved
        mData.put(3, (byte)0x00); // reserved
        mData.put(4, (byte)0x00); // reserved
        mData.put(5, (byte)0x00); // reserved
        mData.put(6, (byte)0x00); // reserved
        mData.put(7, (byte)0x00); // reserved
    }

    public byte[] getBytes(){
        return mData.array();
    }

}
