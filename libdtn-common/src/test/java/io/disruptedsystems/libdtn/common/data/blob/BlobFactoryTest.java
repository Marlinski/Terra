package io.disruptedsystems.libdtn.common.data.blob;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Lucien Loiseau on 08/09/20.
 */
public class BlobFactoryTest {

    @Test
    public void testVolatileBlobFactory() {
        BlobFactory factory = new BaseBlobFactory().setVolatileMaxSize(100);

        try {
            Blob vblob = factory.createBlob(50);
            assertFalse(vblob.isFileBlob());
        } catch(Exception e) {
            fail();
        }

        try {
            Blob vblob = factory.createBlob(250);
            fail();
        } catch(Exception e) {
        }
    }

    @Test
    public void testPersistentBlobFactory() {
        BlobFactory factory = new BaseBlobFactory().setPersistentPath("/tmp");

        try {
            Blob fblob = factory.createBlob(50);
            assertTrue(fblob.isFileBlob());

            File file = new File(fblob.getFilePath());
            assertTrue(file.exists());
            fblob.getWritableBlob().dispose();
            assertFalse(file.exists());
        } catch(Exception e) {
            fail();
        }
    }

    @Test
    public void testVolatilePersistentBlobFactory() {
        BlobFactory factory = new BaseBlobFactory().setVolatileMaxSize(100).setPersistentPath("/tmp");

        try {
            Blob vblob = factory.createBlob(50);
            assertFalse(vblob.isFileBlob());

            Blob fblob = factory.createBlob(150);
            assertTrue(fblob.isFileBlob());

            File file = new File(fblob.getFilePath());
            assertTrue(file.exists());
            fblob.getWritableBlob().dispose();
            assertFalse(file.exists());
        } catch(Exception e) {
            fail();
        }
    }

}
