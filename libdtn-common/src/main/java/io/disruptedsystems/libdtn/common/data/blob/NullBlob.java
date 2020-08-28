package io.disruptedsystems.libdtn.common.data.blob;

import io.disruptedsystems.libdtn.common.utils.Function;
import io.disruptedsystems.libdtn.common.utils.Supplier;
import io.disruptedsystems.libdtn.common.data.Tag;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * NullBlob is a VolatileBlob of size zero and that contains no data. It acts as a sink and so
 * any data written to it raises no error but goes nowhere.
 *
 * @author Lucien Loiseau on 04/09/18.
 */
public class NullBlob extends Tag implements Blob {

    public NullBlob() {
    }
    
    public NullBlob(int expectedSize) {
    }

    @Override
    public Flowable<ByteBuffer> observe() {
        return Flowable.empty();
    }

    @Override
    public void map(Supplier<ByteBuffer> open,
                    Function<ByteBuffer, ByteBuffer> function,
                    Supplier<ByteBuffer> close) {
    }

    @Override
    public long size() {
        return 0;
    }

    public class NullReadableBlob implements ReadableBlob {
        @Override
        public void read(OutputStream stream) throws IOException {
        }

        @Override
        public void close() {
        }
    }

    public class NullWritableBlob implements WritableBlob {
        @Override
        public void clear() {
        }

        @Override
        public int write(InputStream stream) {
            return 0;
        }

        @Override
        public int write(InputStream stream, int size) {
            return 0;
        }

        @Override
        public int write(byte b)  {
            return 0;
        }

        @Override
        public int write(byte[] a)  {
            return 0;
        }

        @Override
        public int write(ByteBuffer buffer)  {
            return 0;
        }

        @Override
        public void close() {
        }
    }

    @Override
    public WritableBlob getWritableBlob() {
        return new NullWritableBlob();
    }

    @Override
    public boolean isFileBlob() {
        return false;
    }

    @Override
    public String getFilePath() throws NotFileBlob {
        throw new NotFileBlob();
    }

    @Override
    public Completable moveToFile(String path) {
        return Completable.complete();
    }
}
