package io.disruptedsystems.libdtn.common.data.blob;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Interface to write data into a Blob.
 *
 * @author Lucien Loiseau on 30/07/18.
 */
public interface WritableBlob {

    /**
     * Clear the content of this blob.
     */
    void dispose();

    /**
     * Read size bytes from the InputStream and store it in the VolatileBlob.
     * Note that there is a natural limit to the size of a Blob because we can read
     * up to {@link Integer#MAX_VALUE} bytes.
     *
     * @param stream read the data from
     * @return int number of bytes read
     * @throws IOException if low-level reading the data or writing to the blob failed
     */
    int write(InputStream stream) throws IOException;

    /**
     * Read size bytes from the InputStream and store it in the VolatileBlob.
     * Note that there is a natural limit to the size of a VolatileBlob because
     * we can read up to {@link Integer#MAX_VALUE} bytes.
     *
     * @param stream read the data from
     * @param size   of the data to read
     * @return int number of byte read
     * @throws IOException if low-level reading the data or writing to the blob failed
     */
    int write(InputStream stream, int size) throws IOException;

    /**
     * copy one byte to the VolatileBlob.
     *
     * @param b the byte
     * @return 1
     * @throws IOException if low-level reading the data or writing to the blob faile
     */
    int write(byte b) throws IOException;

    /**
     * read all the bytes from the array and copy them to the VolatileBlob.
     *
     * @param a the byte array to write to the VolatileBlob
     * @return number of bytes read
     * @throws IOException if low-level reading the data or writing to the blob failed
     */
    int write(byte[] a) throws IOException;

    /**
     * read all the bytes from the ByteBuffer and copy them to the VolatileBlob.
     *
     * @param buffer the bytebyffer to write to the VolatileBlob
     * @return number of bytes read
     * @throws IOException if low-level reading the data or writing to the blob failed
     */
    int write(ByteBuffer buffer) throws IOException;

    /**
     * After close() is called, no further write call is possible.
     */
    void close();
}
