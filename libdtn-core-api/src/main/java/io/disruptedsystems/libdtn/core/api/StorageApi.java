package io.disruptedsystems.libdtn.core.api;

import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.blob.BlobFactory;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

/**
 * API for storage component.
 *
 * @author Lucien Loiseau on 24/10/18.
 */
public interface StorageApi extends CoreComponentApi {

    class StorageException extends Exception {
        public StorageException(String msg) {
            super(msg);
        }
    }

    class StorageFailedException extends StorageException {
        public StorageFailedException() {
            super("storage failed");
        }

        public StorageFailedException(String msg) {
            super(msg);
        }
    }

    class StorageCorruptedException extends StorageException {
        public StorageCorruptedException() {
            super("storage corrupted");
        }
    }

    class StorageUnavailableException extends StorageException {
        public StorageUnavailableException() {
            super("storage unavailable");
        }
    }

    class StorageFullException extends StorageException {
        public StorageFullException() {
            super("storage full");
        }
    }

    class BundleAlreadyExistsException extends StorageException {
        public BundleAlreadyExistsException() {
            super("bundle already exists");
        }

        public BundleAlreadyExistsException(String bid) {
            super("bundle already exists: " + bid);
        }
    }

    class BundleNotFoundException extends StorageException {
        public BundleNotFoundException() {
            super("bundle not found");
        }

        public BundleNotFoundException(String bid) {
            super("bundle not found: " + bid);
        }
    }

    /**
     * Return a Blob factory.
     *
     * @return BlobFactory
     */
    BlobFactory getBlobFactory();

    /**
     * count the total number of bundle indexed, whether in persistant or volatile storage.
     *
     * @return total number of bundle indexed
     */
    Single<Integer> count();

    /**
     * check if a Bundle is in storage.
     *
     * @param bid of the bundle
     * @return true if the Bundle is stored in volatile storage, false otherwise
     */
    Single<Boolean> contains(String bid);

    /**
     * Store a bundle into storage.
     *
     * <p>Whenever the Single completes, the caller can expect that no further operations is needed
     * in background to store the bundle.
     *
     * @param bundle to store
     * @return Completable that complete whenever the bundle is stored, error otherwise
     */
    Completable store(Bundle bundle);

    /**
     * Pull a Bundle from StorageApi. It will try to pull it from Volatile if it exists, or from
     * SimpleStorage otherwise.
     *
     * @param id of the bundle to pull from storage
     * @return a Single that completes if the Bundle was successfully pulled, onError otherwise
     */
    Single<Bundle> get(String id);

    /**
     * Delete a Bundle from all storage and removes all event registration to it.
     *
     * @param id of the bundle to delete
     * @return Completable
     */
    Completable remove(String id);

    /**
     * Clear all bundles.
     *
     * @return Completable
     */
    Completable clear();

    // todo remove this
    String print();

}
