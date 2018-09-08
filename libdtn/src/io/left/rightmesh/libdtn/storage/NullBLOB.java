package io.left.rightmesh.libdtn.storage;

import io.reactivex.Flowable;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * NullBlob is a BLOB of size zero and that contains no data.
 *
 * @author Lucien Loiseau on 04/09/18.
 */
public class NullBLOB extends BLOB {

    @Override
    public Flowable<ByteBuffer> observe() {
        return Flowable.empty();
    }

    @Override
    public long size() {
        return 0;
    }

    @Override
    protected InnerWritableBLOB writableBLOBFactory() throws BLOBStateException {
        return new InnerWritableBLOB() {
            @Override
            public void clear() {
            }

            @Override
            public int write(InputStream stream, int size) {
                return 0;
            }

            @Override
            public int write(byte b) {
                return 0;
            }

            @Override
            public int write(byte[] a)  {
                return 0;
            }
        };
    }

    @Override
    protected InnerReadableBLOB readableBLOBFactory() throws BLOBStateException {
        return new InnerReadableBLOB() {
            @Override
            public void read(OutputStream stream) {
            }
        };
    }

}
