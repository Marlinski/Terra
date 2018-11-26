package io.left.rightmesh.libdtn.core.storage;

import io.left.rightmesh.libdtn.common.utils.Log;
import io.left.rightmesh.libdtn.core.BaseComponent;
import io.left.rightmesh.libdtn.common.data.Bundle;
import io.left.rightmesh.libdtn.common.data.BundleID;
import io.left.rightmesh.libdtn.common.data.MetaBundle;
import io.left.rightmesh.libdtn.core.api.ConfigurationAPI;
import io.left.rightmesh.libdtn.core.api.CoreAPI;
import io.left.rightmesh.libdtn.core.api.StorageAPI.BundleAlreadyExistsException;
import io.left.rightmesh.libdtn.core.api.StorageAPI.StorageUnavailableException;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;

import static io.left.rightmesh.libdtn.core.api.ConfigurationAPI.CoreEntry.COMPONENT_ENABLE_VOLATILE_STORAGE;

/**
 * VolatileStorage holds all the Bundle in memory.
 *
 * @author Lucien Loiseau on 26/07/18.
 */
public class VolatileStorage extends BaseComponent {

    private static final String TAG = "VolatileStorage";

    private Storage metaStorage;
    private CoreAPI core;

    public VolatileStorage(Storage metaStorage, CoreAPI core) {
        this.metaStorage = metaStorage;
        this.core = core;
        initComponent(core.getConf(), COMPONENT_ENABLE_VOLATILE_STORAGE, core.getLogger());
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
        return (int)metaStorage.index.values().stream().filter(e -> e.isVolatile).count();
    }

    /**
     * Stores a bundle into VolatileStorage
     *
     * @param bundle to store
     * @return Completable that completes once it is done
     */
    Single<Bundle> store(Bundle bundle) {
        if (!isEnabled()) {
            return Single.error(new StorageUnavailableException());
        }

        if (metaStorage.containsVolatile(bundle.bid)) {
            return Single.error(new BundleAlreadyExistsException());
        } else {
            Storage.IndexEntry entry = metaStorage.getEntryOrCreate(bundle.bid, bundle);
            entry.isVolatile = true;
            return Single.just(bundle);
        }
    }


    private Completable remove(BundleID bid, Storage.IndexEntry entry) {
        return Completable.create(s -> {
            if(!entry.isPersistent) {
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
    Completable remove(BundleID bid) {
        Storage.IndexEntry entry = metaStorage.index.get(bid);
        return remove(bid, entry);
    }

    /**
     * Remove all volatile bundle. If the bundle has a persistent copy, replace the bundle with
     * the MetaBundle, otherwise delete from index.
     */
    Completable clear() {
        if (!isEnabled()) {
            return Completable.error(new StorageUnavailableException());
        }

        return Observable.fromIterable(metaStorage.index.entrySet())
                .flatMapCompletable(e -> remove(e.getKey(), e.getValue()))
                .onErrorComplete();
    }
}
