package io.disruptedsystems.libdtn.core.storage.simple;

import org.apache.commons.collections4.trie.PatriciaTrie;

import java.io.File;
import java.util.Set;

import io.disruptedsystems.libdtn.common.data.blob.BaseBlobFactory;
import io.disruptedsystems.libdtn.common.data.blob.BlobFactory;
import io.disruptedsystems.libdtn.common.data.blob.NullBlob;
import io.disruptedsystems.libdtn.core.CoreComponent;
import io.disruptedsystems.libdtn.core.api.ConfigurationApi;
import io.disruptedsystems.libdtn.core.api.CoreApi;
import io.disruptedsystems.libdtn.core.api.StorageApi;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

/**
 * Storage class implements StorageAPI and provides both volatile and peristent storage operation
 * based on the node configuration.
 *
 * @author Lucien Loiseau on 29/09/18.
 */
public abstract class SimpleStorage<T> extends CoreComponent implements StorageApi {

    protected static final String TAG = "SimpleStorage";

    protected CoreApi core;
    protected BaseBlobFactory blobFactory;
    protected StorageIndex<T> index;

    public static StorageApi create(CoreApi core) {
        ConfigurationApi.EntryInterface<String> e =
                core.getConf().get(ConfigurationApi.CoreEntry.PERSISTENCE_STORAGE_PATH);

        if (e != null && e.value() != null && !e.value().equals("@DISABLED")) {
            File path = new File(e.value());
            if (path.exists() && path.isDirectory() && path.canWrite()) {
                return new FileStorage(core, path);
            }
        }

        return new VolatileStorage(core);
    }

    protected SimpleStorage(CoreApi core) {
        this.core = core;
        index = new StorageIndex<>();
        blobFactory = new BaseBlobFactory();
        blobFactory.setVolatileMaxSize(core.getConf().<Integer>get(ConfigurationApi.CoreEntry.BLOB_VOLATILE_MAX_SIZE).value());
    }

    @Override
    public BlobFactory getBlobFactory() {
        if (!isEnabled()) {
            return NullBlob::new;
        }

        return blobFactory;
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
        if (index != null) {
            index.teardown();
        }
    }

    @Override
    public Single<Integer> count() {
        if (!isEnabled()) {
            return Single.just(0);
        }

        return Single.just(index.size());
    }

    @Override
    public Single<Boolean> contains(String bid) {
        if (!isEnabled()) {
            return Single.error(new StorageUnavailableException());
        }

        return Single.just(index.contains(bid));
    }

    @Override
    public Completable clear() {
        if (!isEnabled()) {
            return Completable.error(new StorageUnavailableException());
        }

        return Observable.fromIterable(index.allBid())
                .flatMapCompletable(this::remove)
                .onErrorComplete();
    }

    @Override
    public Observable<String> findBundlesToForward(String destination) {
        return findBundlesFromTrie(index.forwardingTrie, destination);
    }

    @Override
    public Observable<String> findBundlesToDeliver(String destination) {
        return findBundlesFromTrie(index.deliveryTrie, destination);
    }

    private Observable<String> findBundlesFromTrie(PatriciaTrie<Set<StorageIndex.IndexEntry<T>>> trie,
                                                   String destination) {
        Set<StorageIndex.IndexEntry<T>> set = trie.get(destination);
        if (set == null) {
            return Observable.empty();
        }
        return Observable.fromStream(set.stream())
                .map(entry -> entry.bundle.bid);
    }

    @Override
    public String print() {
        if (!isEnabled()) {
            return "storage is not available";
        }

        StringBuilder sb = new StringBuilder("current cache:\n");
        sb.append("--------------\n\n");
        sb.append("\n");
        return sb.toString();
    }

    /* call block specific routine for storage
     * not useful now
    protected void onPut(Bundle bundle) {
        try {
            for (CanonicalBlock block : bundle.getBlocks()) {
                try {
                    core.getExtensionManager().getBlockProcessorFactory()
                            .create(block.type).onPutOnStorage(block, bundle, core.getLogger());
                } catch (BlockProcessorFactory.ProcessorNotFoundException pe) {
                    // ignore
                }
            }
        } catch (ProcessingException e) {
            // ignore?
        }
    }
    */

    /* call block specific routine for storage
     * not useful now
    protected void onPull(Bundle bundle) {
        try {
            for (CanonicalBlock block : vb.getBlocks()) {
                try {
                    processorFactory.create(block.type).onPullFromStorage(block, vb, logger);
                } catch (BlockProcessorFactory.ProcessorNotFoundException pe) {
                    // ignore
                }
            }
        } catch (ProcessingException e) {
            // ignore ?
        }
    }
    */
}
