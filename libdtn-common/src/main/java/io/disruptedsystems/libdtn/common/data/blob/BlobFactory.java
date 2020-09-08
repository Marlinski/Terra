package io.disruptedsystems.libdtn.common.data.blob;

import java.io.IOException;

/**
 * A {@link Blob} factory creates a new Blob structure on demand.
 *
 * @author Lucien Loiseau on 21/10/18.
 */
public interface BlobFactory {
    /**
     * creates a new {@link Blob} of expected size.
     *
     * @param size expected
     * @return a new Blob instance
     * @throws IOException if the Blob could not be created.
     */
    Blob createBlob(int size) throws IOException;
}
