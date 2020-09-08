package io.disruptedsystems.libdtn.common.data.blob;

import io.disruptedsystems.libdtn.common.data.Tag;
import io.disruptedsystems.libdtn.common.utils.Function;
import io.disruptedsystems.libdtn.common.utils.Supplier;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

/**
 * @author Lucien Loiseau on 30/10/18.
 */
public class VolatileBlob extends Tag implements Blob {

    ByteBuffer data;

    /**
     * Constructor creates a VolatileBlob with an expected size.
     * @param expectedSize of the Blob
     */
    public VolatileBlob(int expectedSize) {
        this.data = ByteBuffer.allocate(expectedSize);
    }

    /**
     * Constructor creates an UntrackedByteBufferBlob from a byte array.
     * @param data array holding the buffer.
     */
    public VolatileBlob(byte[] data) {
        this.data = ByteBuffer.wrap(data);
    }

    /**
     * Constructor creates an UntrackedByteBufferBlob from a ByteBuffer.
     * @param data ByteBuffer holding the buffer.
     */
    public VolatileBlob(ByteBuffer data) {
        this.data = ByteBuffer.allocate(data.remaining());
        this.data.put(data);
        this.data.position(0);
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
        return Observable.using(
                () -> {
                    File file = new File(path);
                    if (file.exists()) {
                        file.delete();
                    }
                    file.createNewFile();
                    return new BufferedOutputStream(new FileOutputStream(file, true));
                },
                bos -> observe().map(b -> {
                    while (b.hasRemaining()) {
                        bos.write(b.get());
                    }
                    return b;
                }).toObservable(),
                BufferedOutputStream::close).ignoreElements();
    }

    @Override
    public long size() {
        return data.limit();
    }

    @Override
    public Flowable<ByteBuffer> observe() {
        return Flowable.create(s -> {
            ByteBuffer dup = data.duplicate();
            s.onNext(dup);
            s.onComplete();
        }, BackpressureStrategy.BUFFER);
    }

    @Override
    public void map(Supplier<ByteBuffer> open,
                    Function<ByteBuffer, ByteBuffer> function,
                    Supplier<ByteBuffer> close) throws Exception {
        ByteBuffer opened = open.get();
        ByteBuffer mapped = function.apply(data);
        ByteBuffer closed = close.get();
        ByteBuffer ret = ByteBuffer.allocate(opened.remaining()
                + mapped.remaining()
                + closed.remaining());
        ret.put(opened);
        ret.put(mapped);
        ret.put(closed);
        this.data = ret;
        data.position(0);
        data.mark();
    }

    @Override
    public WritableBlob getWritableBlob() {
        return new WritableBlob() {
            {{
                data.position(0);
                data.limit(data.capacity());
            }}

            @Override
            public void dispose() {
                data.clear();
            }

            @Override
            public int write(byte b) throws IOException {
                try {
                    data.put(b);
                } catch (BufferOverflowException boe) {
                    throw new IOException("write failed: capacity exceeded");
                }
                return 1;
            }

            @Override
            public int write(byte[] a) throws IOException {
                try {
                    data.put(a);
                } catch (BufferOverflowException boe) {
                    throw new IOException("write failed: capacity exceeded");
                }
                return a.length;
            }

            @Override
            public int write(ByteBuffer buffer) throws IOException {
                int size = buffer.remaining();
                try {
                    data.put(buffer);
                } catch (BufferOverflowException boe) {
                    throw new IOException("write failed: capacity exceeded");
                }
                return size;
            }

            @Override
            public int write(InputStream stream) throws IOException {
                int remaining = data.remaining();
                int size = remaining;
                int b;
                while (remaining > 0) {
                    if ((b = stream.read()) == -1) {
                        return (size - remaining);
                    }
                    data.put((byte) b);
                    remaining--;
                }
                throw new IOException("write failed: capacity exceeded");
            }

            @Override
            public int write(InputStream stream, int size) throws IOException {
                if (size > (data.remaining())) {
                    throw new IOException("write failed: capacity exceeded");
                }

                int remaining = size;
                int b;
                while (remaining > 0) {
                    if ((b = stream.read()) == -1) {
                        break;
                    }
                    data.put((byte) b);
                    remaining--;
                }
                return size-remaining;
            }

            @Override
            public void close() {
                data.flip();
                data.mark();
            }
        };
    }
}
