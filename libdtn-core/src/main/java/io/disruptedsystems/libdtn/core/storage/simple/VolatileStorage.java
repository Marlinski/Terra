package io.disruptedsystems.libdtn.core.storage.simple;

import io.disruptedsystems.libdtn.core.api.CoreApi;
import io.disruptedsystems.libdtn.core.api.StorageApi;
import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.MetaBundle;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

/**
 * VolatileStorage holds all the Bundle in memory.
 *
 * @author Lucien Loiseau on 26/07/18.
 */
public class VolatileStorage extends SimpleStorage<Object> {

    private static final String TAG = "VolatileStorage";

    public VolatileStorage(CoreApi core) {
        super(core);
    }

    @Override
    public Completable store(Bundle bundle) {
        if (!isEnabled()) {
            return Completable.error(new StorageApi.StorageUnavailableException());
        }

        if (index.contains(bundle.bid)) {
            return Completable.error(new StorageApi.BundleAlreadyExistsException());
        } else {
            index.put(bundle.bid, bundle);
            return Completable.complete();
        }
    }

    @Override
    public Single<Bundle> get(String bid) {
        if (!isEnabled()) {
            return Single.error(new StorageApi.StorageUnavailableException());
        }

        if (!index.contains(bid)) {
            return Single.error(new StorageApi.BundleAlreadyExistsException());
        }

        return Single.just(index.pullBundle(bid));
    }

    @Override
    public Completable remove(String bid) {
        index.remove(bid);
        return Completable.complete();
    }

    @Override
    protected void componentUp() {
        // nothing to do
    }
}
