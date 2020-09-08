package io.disruptedsystems.libdtn.common.data.blob;

import org.junit.Test;

import java.io.File;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Lucien Loiseau on 08/09/20.
 */
public class BlobTest {

    private void cleanUp() {
        File file1 = new File("/tmp/testVolatileAndFileBlob");
        File file2 = new File("/tmp/testVolatileAndFileBlob2");
        file1.delete();
        file2.delete();
        assertFalse(file1.exists());
        assertFalse(file2.exists());
    }

    @Test
    public void testVolatileAndFileBlob() {
        cleanUp();

        VolatileBlob vblob = new VolatileBlob(15000);
        assertFalse(vblob.isFileBlob());

        // check writing into blob
        WritableBlob wblob = vblob.getWritableBlob();
        try {
            for (int i = 0; i < 1000; i++) {
                wblob.write(("coucou " + i + "-").getBytes());
            }
            wblob.close();

            // check buffer was correctly written
            assertEquals(10890, vblob.size());
            ByteBuffer bb = vblob.observe().blockingFirst();
            for (int i = 0; i < 1000; i++) {
                String s = "coucou " + i + "-";
                byte[] read = new byte[s.length()];
                bb.get(read);
                assertEquals(s, new String(read));
            }

            // check move to file
            vblob.moveToFile("/tmp/testVolatileAndFileBlob").blockingAwait();
            FileBlob fblob = new FileBlob("/tmp/testVolatileAndFileBlob");
            assertTrue(fblob.isFileBlob());
            assertEquals("/tmp/testVolatileAndFileBlob", fblob.getFilePath());
            assertEquals(10890, fblob.size());

            // coollect the whole file in a bytebuffer
            bb = fblob.observe().reduce(ByteBuffer.allocate(0), (c, b) -> {
                //System.out.println("\n" + new String(b.array()) + "\n -> " + b.toString());
                ByteBuffer b3 = ByteBuffer.allocate(c.limit() + b.limit());
                b3.put(c);
                b3.put(b);
                b3.flip();
                return b3;
            }).blockingGet();

            // check its content is equal
            for (int i = 0; i < 1000; i++) {
                String s = "coucou " + i + "-";
                byte[] read = new byte[s.length()];
                bb.get(read);
                assertEquals(s, new String(read));
            }

            // check moving files
            File file1 = new File("/tmp/testVolatileAndFileBlob");
            File file2 = new File("/tmp/testVolatileAndFileBlob2");
            assertTrue(file1.exists());
            assertFalse(file2.exists());

            fblob.moveToFile("/tmp/testVolatileAndFileBlob2").blockingAwait();
            assertFalse(file1.exists());
            assertTrue(file2.exists());
            assertEquals("/tmp/testVolatileAndFileBlob2", fblob.getFilePath());

            // perform a transformation on the file
            fblob.map(
                    () -> ByteBuffer.wrap("HEADER".getBytes()),
                    b -> b,
                    () -> ByteBuffer.wrap("TAIL".getBytes()));

            // check that it was done in place
            assertEquals(10900, fblob.size());
            assertTrue(file2.exists());

            // check new file content
            // coollect the whole file in a bytebuffer
            bb = fblob.observe().reduce(ByteBuffer.allocate(0), (c, b) -> {
                //System.out.println("\n" + new String(b.array()) + "\n -> " + b.toString());
                ByteBuffer b3 = ByteBuffer.allocate(c.limit() + b.limit());
                b3.put(c);
                b3.put(b);
                b3.flip();
                return b3;
            }).blockingGet();

            String s = "HEADER";
            byte[] read = new byte[s.length()];
            bb.get(read);
            assertEquals(s, new String(read));

            for (int i = 0; i < 1000; i++) {
                s = "coucou " + i + "-";
                read = new byte[s.length()];
                bb.get(read);
                assertEquals(s, new String(read));
            }

            s = "TAIL";
            read = new byte[s.length()];
            bb.get(read);
            assertEquals(s, new String(read));

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            cleanUp();
        }
    }

    @Test
    public void testFileBlob() {
        cleanUp();

        File file1 = new File("/tmp/testVolatileAndFileBlob");
        File file2 = new File("/tmp/testVolatileAndFileBlob2");
        assertFalse(file1.exists());
        assertFalse(file2.exists());

        try {
            FileBlob fblob = new FileBlob(file1);
            fail();
        } catch (Exception e) {
            // it is supposed to throw an exception if file does not exists
        }

        try {
            file1.createNewFile();
            FileBlob fblob = new FileBlob(file1);
            WritableBlob wblob = fblob.getWritableBlob();

            // try with different write
            for (int i = 0; i < 1000; i++) {
                switch(i%2) {
                    case 0:
                        wblob.write(("coucou " + i + "-").getBytes());
                        break;
                    case 1:
                        wblob.write(ByteBuffer.wrap(("coucou " + i + "-").getBytes()));
                        break;
                }
            }
            wblob.close();

            // check buffer was correctly written
            assertEquals(10890, fblob.size());
            ByteBuffer bb = fblob.observe().reduce(ByteBuffer.allocate(0), (c, b) -> {
                //System.out.println("\n" + new String(b.array()) + "\n -> " + b.toString());
                ByteBuffer b3 = ByteBuffer.allocate(c.limit() + b.limit());
                b3.put(c);
                b3.put(b);
                b3.flip();
                return b3;
            }).blockingGet();

            for (int i = 0; i < 1000; i++) {
                String s = "coucou " + i + "-";
                byte[] read = new byte[s.length()];
                bb.get(read);
                assertEquals(s, new String(read));
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            cleanUp();
        }
    }

}
