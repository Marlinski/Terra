package io.disruptedsystems.libdtn.common.data;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertTrue;

/**
 * @author Lucien Loiseau on 08/02/21.
 */
public class CRCTest {
    
    public void testCRC() {
        Crc crc = Crc.init(Crc.CrcType.CRC32);
        ByteBuffer bb = ByteBuffer.allocate(8*4);
        for(int i = 0; i < 32; i++) {
            bb.put((byte)0xff);
        }
        bb.flip();
        crc.read(bb);
        ByteBuffer check = ByteBuffer.allocate(4);
        check.put((byte)0x43).put((byte)0xab).put((byte)0xa8).put((byte)0x62);
        check.flip();
        assertTrue(crc.doneAndValidate(check));
    }
}
