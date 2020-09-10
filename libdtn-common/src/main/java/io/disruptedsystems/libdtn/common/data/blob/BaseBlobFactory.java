package io.disruptedsystems.libdtn.common.data.blob;

import java.io.File;
import java.io.IOException;

import io.disruptedsystems.libdtn.common.utils.FileUtil;

/**
 * BaseBlobFactory is a Blob factory that can creates volatile or persistent {@link Blob}.
 *
 * @author Lucien Loiseau on 28/10/18.
 */
public class BaseBlobFactory implements BlobFactory {

    private static final String TAG = "BaseBlobFactory";

    private int volatileMaxSize = 0;
    private String blobPath = null;

    /**
     * enable volatile Blob to be created.
     *
     * @param limit is the maximum size of a single volatile blob.
     * @return the current BaseBlobFactory.
     */
    public BaseBlobFactory setVolatileMaxSize(int limit) {
        volatileMaxSize = limit;
        return this;
    }

    /**
     * Enable persistent Blob to be created. A directory must be supplied that will hold
     * the persistent blob in independent files.
     *
     * @param path of the directory where persistent Blob will be created.
     * @return the current BaseBlobFactory.
     */
    public BaseBlobFactory setPersistentPath(String path) {
        this.blobPath = path;
        return this;
    }

    /**
     * Check wether volatile blob is enabled.
     *
     * @return true if volatile blob are enabled, false otherwise.
     */
    public boolean isVolatileEnabled() {
        return volatileMaxSize > 0;
    }

    /**
     * Check wether persistent blob is enabled.
     *
     * @return true if persistent blob are enabled, false otherwise.
     */
    public boolean isPersistentEnabled() {
        return blobPath != null;
    }

    // ----- definite size blob -------

    /**
     * Creates a volatile {@link Blob}.
     *
     * @param expectedSize of the {@link Blob}
     * @return a new {@link VolatileBlob}
     * @throws IOException if volatile {@link Blob} are disabled or if there is no memory.
     */
    public Blob createVolatileBlob(int expectedSize) throws IOException {
        // create in volatile memory
        if (!isVolatileEnabled()) {
            throw new IOException("volatile storage is not enabled");
        }
        if (expectedSize < 0 || expectedSize > volatileMaxSize) {
            throw new IOException("size exceeds maximum volatile blob limit");
        }
        return new VolatileBlob(expectedSize);
    }

    /**
     * Creates a persistent {@link Blob}.
     *
     * @param expectedSize of the {@link Blob}
     * @return a new {@link FileBlob}
     * @throws IOException if persistent {@link Blob} are disabled or if disk is full.
     */
    public Blob createFileBlob(int expectedSize) throws IOException {
        // create in persistent memory
        if (!isPersistentEnabled()) {
            throw new IOException("persistent storage not enabled");
        }

        if (FileUtil.spaceLeft(blobPath) < expectedSize) {
            throw new IOException("not enough size left on device");
        }

        try {
            File fblob = FileUtil.createNewFile("blob-", ".blob", blobPath);
            return new FileBlob(fblob);
        } catch (IOException io) {
            throw new IOException("could not create file in directory: "+ blobPath);
        }
    }

    @Override
    public Blob createBlob(int expectedSize) throws IOException {
        try {
            return createVolatileBlob(expectedSize);
        } catch (IOException e) {
            /* ignore */
        }

        return createFileBlob(expectedSize);
    }
}
