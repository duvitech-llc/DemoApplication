package com.duvitech.demoapplication;


import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

/**
 * Created by gvigelet on 10/19/16.
 */

public class STM32CRC {

    private static final String TAG = "STM32CRC" ;

    public static int GenerateCrc(ByteBuffer bb, int size)
    {
        int Crc = 0;
        int Hold = 0;
        int index = 0;
        int i = 0;
        int len = size;

        Crc = (int)0xFFFFFFFF; // Initial state

        for (i = 0; i < len/4; i++, index += 4)
        {
            // Hold = data[i+0] + (data[i+1] << 8) + (data[i+2] << 16) + (data[i+3] << 24);
            Hold = bb.order(ByteOrder.LITTLE_ENDIAN).getInt(index);
            Crc = Crc32Fast(Crc, Hold); // 4-bytes at a time
        }


        if((len%4) != 0) {       // alignment required
            Hold = 0x0;
            index = 4 * i;
            switch (len % 4) {
                case 1:
                    Hold = bb.get(index) & 0xFF;
                    break;
                case 2:
                    Hold = (int) (0x00000000 | (bb.get(index + 1)  << 8) & 0x0000ff00 | bb.get(index)  & 0x000000ff);
                    break;
                case 3:
                    Hold = (int) (0x00000000 | (bb.get(index + 2)  << 16) & 0x00ff0000 |  (bb.get(index + 1)  << 8) & 0x0000ff00 | bb.get(index)  & 0x000000ff);
                    break;
                default:

                    Log.e(TAG, "STM32CRC alignment issue");
                    break;
            }

            Crc = Crc32Fast(Crc, Hold); // 4-bytes at a time

        }

        return Crc;

    }

    public static int GenerateCrc(byte data[])
    {
        int Crc;
        int i;
        int Hold;
        ByteBuffer bb = ByteBuffer.wrap(data);
        return GenerateCrc(bb, data.length);
    }

    public static int GenerateCrc(File file)
    {
        int Crc = 0;
        int len = 0;

        try {
            FileInputStream inputStream = new FileInputStream(file);
            FileChannel fileChannel = inputStream.getChannel();
            len = (int) fileChannel.size();
            MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, len);

            Crc = GenerateCrc(buffer, len);

            Log.d(TAG, String.format("File CRC: %08X", Crc));
            return Crc;
        }catch (Exception ex){
            Log.e(TAG, "Error opening file and generating CRC value for file Exception: " + ex.getMessage());
        }

        return 0x0;
    }

    public static int GenerateZipCrcFast(File filename) {
        final int SIZE = 16 * 1024;
        try (FileInputStream in = new FileInputStream(filename);) {
            FileChannel channel = in .getChannel();
            CRC32 crc = new CRC32();
            int length = (int) channel.size();
            MappedByteBuffer mb = channel.map(FileChannel.MapMode.READ_ONLY, 0, length);
            byte[] bytes = new byte[SIZE];
            int nGet;
            while (mb.hasRemaining()) {
                nGet = Math.min(mb.remaining(), SIZE);
                mb.get(bytes, 0, nGet);
                crc.update(bytes, 0, nGet);
            }

            return (int)crc.getValue();

        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        throw new RuntimeException("unknown IO error occurred ");
    }

    public static int GenerateZipCrc(File file)
    {
        int Crc = 0;
        int len = 0;

        try {
            CheckedInputStream cis = null;
            long fileSize = 0;
            // Computer CRC32 checksum
            cis = new CheckedInputStream(
                    new FileInputStream(file), new CRC32());

            fileSize = file.length();
            byte[] buf = new byte[16384];
            while(cis.read(buf) >= 0) {
            }
            long checksum = cis.getChecksum().getValue();
            return (int)checksum;
        }
        catch (FileNotFoundException e) {
            Log.e(TAG, "File not found.");
        }
        catch (Exception ex){
            Log.e(TAG, "Error opening file and generating CRC value for file Exception: " + ex.getMessage());
        }

        return 0x0;
    }


    public static int GenerateZipCrc(byte data[])
    {
        long val;
        CRC32 crc32 = new CRC32();
        crc32.reset();
        crc32.update(data, 0, data.length);
        val = crc32.getValue();
        Log.d(TAG, String.format("ZIPCRC: %08X\n",val));
        return (int)val;
    }

    private static int Crc32(int Crc, int Data)
    {
        int i;
        Crc = Crc ^ Data;

        for (i = 0; i < 32; i++)
        {
            if ((Crc & 0x80000000) != 0)
            {
                Crc = (Crc << 1) ^ 0x04C11DB7; // Polynomial used in STM32
            }
            else
            {
                Crc = (Crc << 1);
            }
        }
        return (Crc);
    }

    private static int Crc32Fast(int Crc, int Data)
    {
        int[] CrcTable = {0x00000000,0x04C11DB7,0x09823B6E,0x0D4326D9,0x130476DC,0x17C56B6B,
                0x1A864DB2,0x1E475005, 0x2608EDB8,0x22C9F00F,0x2F8AD6D6,0x2B4BCB61,0x350C9B64,
                0x31CD86D3,0x3C8EA00A,0x384FBDBD}; // Nibble lookup table for 0x04C11DB7 polynomial

        Crc = Crc ^ Data; // Apply all 32-bits

        // Process 32-bits, 4 at a time, or 8 rounds

        Crc = (Crc << 4) ^ CrcTable[Crc >>> 28]; // Assumes 32-bit reg, masking index to 4-bits
        Crc = (Crc << 4) ^ CrcTable[Crc >>> 28]; //  0x04C11DB7 Polynomial used in STM32
        Crc = (Crc << 4) ^ CrcTable[Crc >>> 28];
        Crc = (Crc << 4) ^ CrcTable[Crc >>> 28];
        Crc = (Crc << 4) ^ CrcTable[Crc >>> 28];
        Crc = (Crc << 4) ^ CrcTable[Crc >>> 28];
        Crc = (Crc << 4) ^ CrcTable[Crc >>> 28];
        Crc = (Crc << 4) ^ CrcTable[Crc >>> 28];

        return (Crc);
    }
}
