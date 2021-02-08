package io.disruptedsystems.libdtn.common.data.blob;

import io.disruptedsystems.libdtn.common.utils.Function;
import io.disruptedsystems.libdtn.common.utils.Supplier;
import io.disruptedsystems.libdtn.common.data.Taggable;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

import java.nio.ByteBuffer;

/**
 * A Blob is an abstract structure that holds a buffer that may be very large.
 *
 * @author Lucien Loiseau on 21/10/18.
 */
public interface Blob extends Taggable {

    class NotFileBlob extends Exception {
    }

    /**
     * Size of the current blob object.
     *
     * @return size
     */
    long size();

    /**
     * Return a cold Flowable, BackPressure-enabled, for the entire Blob. On subscription, it
     * opens a ReadableBlob and read it entirely.
     *
     * @return Flowable of ByteBuffer
     */
    Flowable<ByteBuffer> observe();

    /**
     * modify the content of the Blob in-place. If the function throws an Exception it will
     * not modify the Blob.
     *
     * @param open buffer to add when blob is open
     * @param update that maps a bytebuffer to its new value
     * @param close buffer to add on Blob closing
     * @throws Exception if the method throws an Exception
     */
    void map(Supplier<ByteBuffer> open,
             Function<ByteBuffer, ByteBuffer> update,
             Supplier<ByteBuffer> close) throws Exception;

    /**
     * new {@link WritableBlob} from this Blob. The WritableBlob will lock the
     * Blob for write-only operations. calling close() on the WritableBlob will unlock the
     * Blob. Only one WritableBlob can be acquired from this single Blob at any
     * given time.
     *
     * @return WritableBlob
     */
    WritableBlob getWritableBlob();

    /**
     * return true if the entire Blob is hold into a file. A FileBlob always returns true,
     *
     * @return true if blob is a file, false otherwise.
     */
    boolean isFileBlob();

    /**
     * returns the path to the file holding this blob.
     *
     * @return a string to the file holding that blob
     * @throws NotFileBlob exception if this blob is not a pure file based blob
     */
    String getFilePath() throws NotFileBlob;

    /**
     * Move the blob into a file. If the file does not exists it creates it. For volatile blob
     * this method simply serializes the blob into a file. For file-based Blob, this
     * operation simply moves the file to its new location and update the fileBLOB accordingly.
     *
     * @param path new path to move the Blob to.
     * @return Completable that completes when the task is finished.
     */
    Completable moveToFile(String path);
}
