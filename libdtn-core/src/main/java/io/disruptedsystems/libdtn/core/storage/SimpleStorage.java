package io.disruptedsystems.libdtn.core.storage;

import static io.disruptedsystems.libdtn.common.utils.FileUtil.createFile;
import static io.disruptedsystems.libdtn.common.utils.FileUtil.createNewFile;
import static io.disruptedsystems.libdtn.common.utils.FileUtil.spaceLeft;

import io.disruptedsystems.libdtn.core.api.ConfigurationApi;
import io.disruptedsystems.libdtn.core.api.CoreApi;
import io.disruptedsystems.libdtn.core.api.StorageApi;
import io.disruptedsystems.libdtn.core.events.BundleIndexed;
import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborEncoder;
import io.marlinski.libcbor.CborParser;
import io.marlinski.libcbor.rxparser.RxParserException;
import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.BundleId;
import io.disruptedsystems.libdtn.common.data.CanonicalBlock;
import io.disruptedsystems.libdtn.common.data.MetaBundle;
import io.disruptedsystems.libdtn.common.data.blob.Blob;
import io.disruptedsystems.libdtn.common.data.blob.FileBlob;
import io.disruptedsystems.libdtn.common.data.blob.NullBlob;
import io.disruptedsystems.libdtn.common.data.bundlev7.parser.BundleV7Item;
import io.disruptedsystems.libdtn.common.data.bundlev7.parser.PrimaryBlockItem;
import io.disruptedsystems.libdtn.common.data.bundlev7.processor.BlockProcessorFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.processor.ProcessingException;
import io.disruptedsystems.libdtn.common.data.bundlev7.serializer.BundleV7Serializer;
import io.disruptedsystems.libdtn.common.utils.Log;
import io.disruptedsystems.libdtn.core.CoreComponent;
import io.marlinski.librxbus.RxBus;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SimpleStorage stores bundle in files but keep an index in memory of all the bundles in storage.
 * Each entry in the index contains a filesystem path to the bundle as well as a "MetaBundle" that
 * holds some information about the bundle that can be used for routing without having to pull the
 * entire bundle from storage until the very last moment.
 *
 * <p>If the payload of the Bundle is already store in a FileBlob, the index will keep a reference
 * to it and will not serialize it within the bundle file. By so doing, a payload FileBlob need not
 * be copied multiple time.
 *
 * <p>The SimpleStorage is configurable through {@link ConfigurationApi} by updating two values:
 * <ul>
 * <li>COMPONENT_ENABLE_SIMPLE_STORAGE: enable/disable SimpleStorage</li>
 * <li>SIMPLE_STORAGE_PATH: update the list of path to be used as storage.
 * StorageApi priority follows the list order</li>
 * </ul>
 *
 * @author Lucien Loiseau on 20/09/18.
 */
public class SimpleStorage extends CoreComponent {

    private static final String TAG = "SimpleStorage";

    private static final String TMP_FOLDER = File.separator + "tmp" + File.separator;
    private static final String BLOB_FOLDER = File.separator + "blob" + File.separator;
    private static final String BUNDLE_FOLDER = File.separator + "bundle" + File.separator;

    private Storage metaStorage;
    private CoreApi core;

    public SimpleStorage(Storage metaStorage,
                         CoreApi core) {
        this.metaStorage = metaStorage;
        this.core = core;
    }

    @Override
    public String getComponentName() {
        return TAG;
    }

    @Override
    public void initComponent(ConfigurationApi conf, ConfigurationApi.CoreEntry entry, Log logger) {
        super.initComponent(conf, entry, logger);
        core.getConf().<Set<String>>get(ConfigurationApi.CoreEntry.SIMPLE_STORAGE_PATH).observe()
                .subscribe(
                        updated_paths -> {
                            /* remove obsolete path */
                            LinkedList<String> pathsToRemove = new LinkedList<>();
                            storagePaths.stream()
                                    .filter(p -> !updated_paths.contains(p))
                                    .map(pathsToRemove::add)
                                    .count();
                            pathsToRemove.stream().map(this::removePath).count();

                            /* add new path */
                            LinkedList<String> pathsToAdd = new LinkedList<>();
                            updated_paths.stream()
                                    .filter(p -> !storagePaths.contains(p))
                                    .map(pathsToAdd::add)
                                    .count();
                            pathsToAdd.stream().map(this::addPath).count();
                        });
    }

    @Override
    protected void componentUp() {
    }

    @Override
    protected void componentDown() {
    }

    private LinkedList<String> storagePaths = new LinkedList<>();

    /**
     * Count the number of Persistent Bundle in Storage. This method iterates over the entire index.
     *
     * @return number of Persistent bundle in storage
     */
    public int count() {
        return (int) metaStorage.index.values().stream().filter(e -> e.isPersistent).count();
    }

    private boolean removePath(String path) {
        if (storagePaths.contains(path)) {
            metaStorage.index.forEach(
                    (bid, entry) -> {
                        if (entry.isPersistent && entry.bundlePath.startsWith(path)) {
                            entry.isPersistent = false;
                            if (!entry.isVolatile) {
                                metaStorage.removeEntry(bid, entry);
                            }
                        }
                    });
            storagePaths.remove(path);
            return true;
        }
        return false;
    }

    private boolean addPath(String path) {
        if (!storagePaths.contains(path)) {
            File f = new File(path);
            if (f.exists() && f.canRead() && f.canWrite()) {
                File ftmp = new File(path + TMP_FOLDER);
                File fblob = new File(path + BLOB_FOLDER);
                File fbundle = new File(path + BUNDLE_FOLDER);
                if (!ftmp.exists() && !ftmp.mkdir()) {
                    return false;
                }
                if (!fblob.exists() && !fblob.mkdir()) {
                    return false;
                }
                if (!fbundle.exists() && !fbundle.mkdir()) {
                    return false;
                }
                indexBundlesFromPath(fbundle);
                storagePaths.add(path);
                return true;
            }
        }
        return false;
    }

    private void indexBundlesFromPath(File folder) {
        if (!isEnabled()) {
            return;
        }

        for (final File file : folder.listFiles()) {
            /*
             * preparing the parser. We just parse the file header and the primary block of
             * the bundle and then build a MetaBundle that will be use for processing
             */
            BundleV7Item bundleParser = new BundleV7Item(
                    core.getLogger(),
                    core.getExtensionManager(),
                    null);
            CborParser parser = CBOR.parser()
                    .cbor_open_array(2)
                    .cbor_parse_custom_item(
                            FileHeaderItem::new,
                            (p, t, item) -> {
                                p.setReg(0, item);
                            })
                    .cbor_open_array((p, t, s) -> {
                    }) /* we are just parsing the primary block */
                    .cbor_parse_custom_item(
                            () -> new PrimaryBlockItem(
                                    core.getExtensionManager().getEidFactory(),
                                    core.getLogger()),
                            (p, t, item) -> {
                                MetaBundle meta = new MetaBundle(item.bundle);
                                Storage.IndexEntry entry
                                        = metaStorage.getEntryOrCreate(meta.bid, meta);
                                entry.bundlePath = file.getAbsolutePath();
                                entry.hasBlob = p.<FileHeaderItem>getReg(0).hasBlob;
                                entry.blobPath = p.<FileHeaderItem>getReg(0).blobPath;
                                entry.isPersistent = true;
                                RxBus.post(new BundleIndexed(meta));
                            });

            ByteBuffer buffer = ByteBuffer.allocate(500);
            FileChannel in;
            try {
                in = new FileInputStream(file).getChannel();
            } catch (FileNotFoundException fnfe) {
                continue; /* cannot happen */
            }

            /* extracting meta */
            boolean done = false;
            try {
                while ((in.read(buffer) > 0) && !done) {
                    buffer.flip();
                    done = parser.read(buffer);
                    buffer.clear();
                }
                in.close();
            } catch (RxParserException | IOException rpe) {
                continue;
            }
        }
    }

    /**
     * Create a new {@link FileBlob}.
     *
     * @param expectedSize expected size of the Blob to create
     * @return a new FileBlob with capacity of expectedSize
     * @throws StorageApi.StorageFullException        if there isn't enough space in SimpleStorage
     * @throws StorageApi.StorageUnavailableException if SimpleStorage is disabled
     */
    FileBlob createBlob(long expectedSize)
            throws StorageApi.StorageUnavailableException, StorageApi.StorageFullException {
        if (!isEnabled()) {
            throw new StorageApi.StorageUnavailableException();
        }

        for (String path : storagePaths) {
            if (spaceLeft(path + BLOB_FOLDER) > expectedSize) {
                try {
                    File fblob = createNewFile(
                            "blob-",
                            ".blob",
                            path + BLOB_FOLDER);
                    return new FileBlob(fblob);
                } catch (IOException io) {
                    // ignore and try next path
                }
            }
        }
        throw new StorageApi.StorageFullException();
    }

    /**
     * Create a new {@link FileBlob} with indefinite size. In that case it will take the storage
     * path with the most available space.
     *
     * @return a new FileBlob
     * @throws StorageApi.StorageFullException        if there isn't enough space in SimpleStorage
     * @throws StorageApi.StorageUnavailableException if SimpleStorage is disabled
     */
    FileBlob createBlob() throws StorageApi.StorageUnavailableException, StorageApi.StorageFullException {
        if (!isEnabled()) {
            throw new StorageApi.StorageUnavailableException();
        }

        LinkedList<String> copy = new LinkedList<>();
        copy.addAll(storagePaths);
        copy.sort(Comparator.comparing(p -> spaceLeft(p + BLOB_FOLDER)).reversed());
        for (String path : copy) {
            try {
                File fblob = createNewFile("blob-", ".blob", path + BLOB_FOLDER);
                return new FileBlob(fblob);
            } catch (IOException io) {
                // ignore and try next path
            }
        }
        throw new StorageApi.StorageFullException();
    }

    private File createBundleFile(BundleId bid, long expectedSize) throws StorageApi.StorageFullException {
        for (String path : storagePaths) {
            if (spaceLeft(path + BUNDLE_FOLDER) > expectedSize) {
                try {
                    String safeBid = bid.getBidString().replaceAll("/", "_");
                    return createFile(
                            "bundle-" + safeBid + ".bundle",
                            path + BUNDLE_FOLDER);
                } catch (IOException io) {
                    System.out.println("IOException createNewFile: " + io.getMessage() + " : "
                            + path + BUNDLE_FOLDER + "bid=" + bid.getBidString() + ".bundle");
                }
            }
        }
        throw new StorageApi.StorageFullException();
    }

    /**
     * store a bundle into persistent storage. This operation can take time so it is done in
     * a different thread and returns a Completable.
     *
     * @param bundle to store
     * @return Single of the MetaBundle
     */
    Single<Bundle> store(Bundle bundle) {
        if (!isEnabled()) {
            return Single.error(new StorageApi.StorageUnavailableException());
        }

        if (metaStorage.containsPersistent(bundle.bid)) {
            return Single.error(new StorageApi.BundleAlreadyExistsException());
        }

        return Single.<Bundle>create(
                s -> {
                    /* prepare bundle: we do not serialize the payload if it is a fileBLOB */
                    boolean hasBlob = false;
                    String blobPath = "";
                    Blob blob = new NullBlob();
                    if (bundle.getPayloadBlock().data.isFileBlob()) {
                        blob = bundle.getPayloadBlock().data;
                        hasBlob = true;
                        blobPath = blob.getFilePath();

                        /* temporary remove the blob from bundle for serialization */
                        bundle.getPayloadBlock().data = new NullBlob();
                    }

                    /* prepare metabundle */
                    final MetaBundle meta = new MetaBundle(bundle);

                    /*
                     * the bundle will be serialized in the file as a CBOR array containing
                     * two item, the file header and the bundle
                     */
                    CborEncoder enc = CBOR.encoder()
                            .cbor_start_array(2)  /* File = {header , bundle} */
                            .cbor_start_array(2)  /* File Header = { boolean, String }*/
                            .cbor_encode_boolean(hasBlob)
                            .cbor_encode_text_string(blobPath)
                            .merge(BundleV7Serializer.encode(bundle,
                                    core.getExtensionManager().getBlockDataSerializerFactory()));

                    /* assess file size */
                    AtomicLong size = new AtomicLong();
                    BundleV7Serializer.encode(bundle,
                            core.getExtensionManager().getBlockDataSerializerFactory()).observe()
                            .subscribe(
                                    buffer -> size.set(size.get() + buffer.remaining()),
                                    e -> size.set(-1),
                                    () -> { /* ignore */ });

                    /* create file */
                    File fbundle;
                    try {
                        fbundle = createBundleFile(bundle.bid, size.get());
                    } catch (StorageApi.StorageFullException sfe) {
                        if (hasBlob) {
                            bundle.getPayloadBlock().data = blob;
                        }
                        s.onError(new Throwable("storage is full"));
                        return;
                    }

                    /* actual serialization of the bundle */
                    OutputStream out = new BufferedOutputStream(new FileOutputStream(fbundle));
                    enc.observe().toObservable().subscribe(
                            new DisposableObserver<ByteBuffer>() {
                                @Override
                                public void onNext(ByteBuffer buffer) {
                                    try {
                                        while (buffer.hasRemaining()) {
                                            out.write(buffer.get());
                                        }
                                    } catch (IOException io) {
                                        dispose();
                                        closeSilently(out);
                                        meta.tag("serialization_failed");
                                    }
                                }

                                @Override
                                public void onError(Throwable t) {
                                    fbundle.delete();
                                    closeSilently(out);
                                    meta.tag("serialization_failed");
                                }

                                @Override
                                public void onComplete() {
                                    closeSilently(out);
                                }
                            });

                    /* post-serialization: we put back the blob into the bundle */
                    if (hasBlob) {
                        bundle.getPayloadBlock().data = blob;
                    }

                    if (!meta.isTagged("serialization_failed")) {
                        final Storage.IndexEntry entry
                                = metaStorage.getEntryOrCreate(meta.bid, meta);
                        entry.isPersistent = true;
                        entry.bundlePath = fbundle.getAbsolutePath();
                        entry.hasBlob = hasBlob;
                        entry.blobPath = blobPath;
                        bundle.tag("in_storage");
                        s.onSuccess(meta);
                    } else {
                        s.onError(new Throwable("bundle failed to serialize into file"));
                    }
                }
        ).subscribeOn(Schedulers.io());
    }

    static void closeSilently(OutputStream s) {
        try {
            s.flush();
        } catch (IOException io) {
            /* ignore */
        }

        try {
            s.close();
        } catch (IOException io) {
            /* ignore */
        }
    }

    /**
     * Pull a bundle from storage. This operation can take some time so it is done in a different
     * thread and returns a Single RxJava object.
     *
     * @param id of the bundle
     * @return Single completes with the bundle on success, throw an error otherwise
     */
    public Single<Bundle> get(BundleId id) {
        if (!isEnabled()) {
            return Single.error(new StorageApi.StorageUnavailableException());
        }

        return Single.<Bundle>create(s -> {
            if (!metaStorage.containsPersistent(id)) {
                s.onError(new StorageApi.BundleNotFoundException(id));
                return;
            }

            /* pulling entry from index */
            Storage.IndexEntry entry = metaStorage.index.get(id);
            File fbundle = new File(entry.bundlePath);
            if (!fbundle.exists() || !fbundle.canRead()) {
                s.onError(new StorageApi.StorageFailedException("can't read bundle file in storage: "
                        + entry.bundlePath));
                return;
            }

            /* preparing file and parser */
            CborParser parser = CBOR.parser()
                    .cbor_open_array(2)
                    .cbor_parse_custom_item(
                            FileHeaderItem::new,
                            (p, t, item) -> p.setReg(0, item))
                    .cbor_parse_custom_item(
                            () -> new BundleV7Item(
                                    core.getLogger(),
                                    core.getExtensionManager(),
                                    metaStorage.getBlobFactory()),
                            (p, t, item) -> {
                                if (p.<FileHeaderItem>getReg(0).hasBlob) {
                                    String path = p.<FileHeaderItem>getReg(0).blobPath;
                                    try {
                                        item.bundle.getPayloadBlock().data = new FileBlob(path);
                                    } catch (IOException io) {
                                        throw new RxParserException("can't retrieve payload blob");
                                    }
                                }
                                item.bundle.tag("in_storage");
                                p.setReg(1, item.bundle); // ret value
                            });

            /* extracting bundle from file */
            ByteBuffer buffer = ByteBuffer.allocate(2048);
            FileChannel in = new FileInputStream(entry.bundlePath).getChannel();
            try {
                boolean done = false;
                while ((in.read(buffer) > 0) && !done) { // read buffer from file
                    buffer.flip();
                    done = parser.read(buffer);
                    buffer.clear();
                }
                in.close();
            } catch (RxParserException | IOException rpe) {
                /* should not happen */
                s.onError(rpe);
                return;
            }

            Bundle ret = parser.getReg(1);
            parser.reset();

            if (ret != null) {
                /* call block specific routine when bundle is pulled from storage */
                try {
                    for (CanonicalBlock block : ret.getBlocks()) {
                        try {
                            core.getExtensionManager().getBlockProcessorFactory().create(block.type)
                                    .onPullFromStorage(block, ret, core.getLogger());
                        } catch (BlockProcessorFactory.ProcessorNotFoundException pe) {
                            /* ignore */
                        }
                    }
                    s.onSuccess(ret);
                } catch (ProcessingException e) {
                    s.onError(e);
                }
            } else {
                s.onError(new StorageApi.StorageFailedException("can't retrieve bundle from file"));
            }
        }).subscribeOn(Schedulers.io());
    }

    /**
     * Remove a bundle from persistent storage. Removing implies deleting the file, deleting
     * the payload blob (if any) and updating the entry in Storage. If the entry has no volatile
     * copy, it deletes the entry from the index. This operation can take some time so it is done
     * in a different thread and returns a Completable.
     *
     * @param id of the bundle to delete
     * @return Completable
     */
    public Completable remove(BundleId id) {
        if (!isEnabled()) {
            return Completable.error(StorageApi.StorageUnavailableException::new);
        }

        return Completable.create(s -> {
            if (!metaStorage.containsPersistent(id)) {
                s.onError(new StorageApi.BundleNotFoundException());
                return;
            }

            String error = "";
            Storage.IndexEntry entry = metaStorage.index.get(id);

            File fbundle = new File(entry.bundlePath);
            core.getLogger().v(TAG, "deleting " + id.getBidString()
                    + " bundle file: "
                    + fbundle.getAbsolutePath());
            if (fbundle.exists() && !fbundle.canWrite()) {
                error += "can't access bundle file for deletion";
            } else {
                fbundle.delete();
            }

            if (entry.hasBlob) {
                File fblob = new File(entry.blobPath);
                core.getLogger().v(TAG, "deleting  " + id.getBidString()
                        + " blob file: "
                        + fblob.getAbsolutePath());
                if (fblob.exists() && !fblob.canWrite()) {
                    error += "can't access payload blob file for deletion";
                } else {
                    fblob.delete();
                }
            }
            entry.hasBlob = false;
            entry.bundlePath = "";
            entry.blobPath = "";
            entry.isPersistent = false;

            if (!entry.isVolatile) {
                metaStorage.removeEntry(id, entry);
            }

            if (error.length() > 0) {
                s.onError(new Throwable(error));
            } else {
                s.onComplete();
            }
        });
    }

    /**
     * Clear the entire persistent storage. Delete all bundles files and related blob (if any) and
     * clear the index.
     *
     * @return completable that completes once the database is wiped.
     */
    public Completable clear() {
        if (!isEnabled()) {
            return Completable.error(new StorageApi.StorageUnavailableException());
        }

        return Observable.fromIterable(metaStorage.index.keySet())
                .flatMapCompletable(metaStorage::remove)
                .onErrorComplete();
    }

    private static class FileHeaderItem implements CborParser.ParseableItem {

        boolean hasBlob;
        String blobPath;

        @Override
        public CborParser getItemParser() {
            return CBOR.parser()
                    .cbor_open_array(2)
                    .cbor_parse_boolean((p, b) -> hasBlob = b)
                    .cbor_parse_text_string_full((p, str) -> blobPath = str);
        }
    }
}
