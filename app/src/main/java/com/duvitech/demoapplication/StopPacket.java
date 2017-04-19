package com.duvitech.demoapplication;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by gvige on 4/19/2017.
 */

public class StopPacket {
    private static final String TAG = "StopPacket";

    protected ByteBuffer mData = ByteBuffer.allocate(2);

    public StopPacket(){
        mData.put(0, (byte)0xCA); // Stop Byte
        mData.put(1, (byte)0xCA); // reserved
    }

    public byte[] getBytes(){
        return mData.array();
    }
}
