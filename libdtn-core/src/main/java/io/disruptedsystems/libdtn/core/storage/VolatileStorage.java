package io.disruptedsystems.libdtn.core.storage;

import io.disruptedsystems.libdtn.core.api.CoreApi;
import io.disruptedsystems.libdtn.core.api.StorageApi;
import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.BundleId;
import io.disruptedsystems.libdtn.common.data.MetaBundle;
import io.disruptedsystems.libdtn.core.CoreComponent;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

/**
 * VolatileStorage holds all the Bundle in memory.
 *
 * @author Lucien Loiseau on 26/07/18.
 */
public class VolatileStorage extends CoreComponent {

    private static final String TAG = "VolatileStorage";

    private Storage metaStorage;
    private CoreApi core;

    public VolatileStorage(Storage metaStorage, CoreApi core) {
        this.metaStorage = metaStorage;
        this.core = core;
    }

    @Override
    public String getComponentName() {
        return TAG;
    }

    @Override
    protected void componentUp() {
    }

    @Override
    protected void componentDown() {
        clear();
    }

    /**
     * Count the number of VolatileBundle in Storage. This method iterates over the entire index.
     *
     * @return number of volatile bundle in storage
     */
    int count() {
        return (int) metaStorage.index.values().stream().filter(e -> e.isVolatile).count();
    }

    /**
     * Stores a bundle into VolatileStorage.
     *
     * @param bundle to store
     * @return Completable that completes once it is done
     */
    Single<Bundle> store(Bundle bundle) {
        if (!isEnabled()) {
            return Single.error(new StorageApi.StorageUnavailableException());
        }

        if (metaStorage.containsVolatile(bundle.bid)) {
            return Single.error(new StorageApi.BundleAlreadyExistsException());
        } else {
            Storage.IndexEntry entry = metaStorage.getEntryOrCreate(bundle.bid, bundle);
            entry.isVolatile = true;
            return Single.just(bundle);
        }
    }


    private Completable remove(BundleId bid, Storage.IndexEntry entry) {
        return Completable.create(s -> {
            if (!entry.isPersistent) {
                metaStorage.removeEntry(bid, entry);
            } else {
                entry.bundle = new MetaBundle(entry.bundle);
            }
            s.onComplete();
        });
    }

    /**
     * Remove a volatile bundle. If the bundle has a persistent copy, replace the bundle with
     * the MetaBundle, otherwise delete from index.
     *
     * @param bid bundle id of te bundle to remove
     * @return Completable that completes once the bundle is removed
     */
    Completable remove(BundleId bid) {
        Storage.IndexEntry entry = metaStorage.index.get(bid);
        return remove(bid, entry);
    }

    /**
     * Remove all volatile bundle. If the bundle has a persistent copy, replace the bundle with
     * the MetaBundle, otherwise delete from index.
     */
    Completable clear() {
        if (!isEnabled()) {
            return Completable.error(new StorageApi.StorageUnavailableException());
        }

        return Observable.fromIterable(metaStorage.index.entrySet())
                .flatMapCompletable(e -> remove(e.getKey(), e.getValue()))
                .onErrorComplete();
    }
}
