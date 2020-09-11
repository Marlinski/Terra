package io.disruptedsystems.libdtn.core.storage.simple;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.MetaBundle;
import io.disruptedsystems.libdtn.common.data.blob.Blob;
import io.disruptedsystems.libdtn.common.data.blob.FileBlob;
import io.disruptedsystems.libdtn.common.data.blob.NullBlob;
import io.disruptedsystems.libdtn.common.utils.FileUtil;
import io.disruptedsystems.libdtn.core.api.ConfigurationApi;
import io.disruptedsystems.libdtn.core.api.CoreApi;
import io.disruptedsystems.libdtn.core.api.StorageApi;
import io.disruptedsystems.libdtn.core.events.BundleIndexed;
import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborParser;
import io.marlinski.libcbor.rxparser.RxParserException;
import io.marlinski.librxbus.RxBus;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

import static io.disruptedsystems.libdtn.core.storage.simple.FileStorageUtils.createBundleFile;

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
public class FileStorage extends SimpleStorage<FileStorageUtils.BundleInfo> {

    protected static final String BLOB_FOLDER = "blob";
    protected static final String BUNDLE_FOLDER = "bundle";
    ReadWriteLock lock = new ReentrantReadWriteLock();

    private File storageDir;
    private File bundleDir;
    private File blobDir;

    public FileStorage(CoreApi core, File path) {
        super(core);
        this.storageDir = path;
    }

    private void setupStorage(File path) {
        File blobDir = new File(path, BLOB_FOLDER);
        blobDir.mkdirs();
        File bundleDir = new File(path, BUNDLE_FOLDER);
        bundleDir.mkdirs();
        this.bundleDir = new File(path, BUNDLE_FOLDER);
        this.blobDir = new File(path, BLOB_FOLDER);
        blobFactory.setPersistentPath(blobDir.getAbsolutePath());
    }

    @Override
    protected void componentUp() {
        setupStorage(storageDir);
        indexBundlesFromPath(bundleDir);
    }

    /* ========= INDEXING FILES =========== */

    private void indexBundlesFromPath(File folder) {
        if (!isEnabled()) {
            return;
        }

        core.getLogger().i(TAG, "indexing bundles.. " + folder.getAbsolutePath());
        lock.writeLock().lock();
        try {
            for (final File file : folder.listFiles()) {
                // prepare input stream
                ByteBuffer buffer = ByteBuffer.allocate(500);
                FileChannel in;
                try {
                    in = new FileInputStream(file).getChannel();
                } catch (FileNotFoundException fnfe) {
                    continue; /* cannot happen */
                }

                // prepare parser
                boolean done = false;
                CborParser parser = CBOR.parser().cbor_parse_custom_item(
                        () -> new FileStorageUtils.MetaBundleFileItem(core.getLogger()),
                        (p, t, item) -> {
                            item.info.bundlePath = file.getAbsolutePath();
                            index.put(item.meta.bid, item.meta, item.info);
                        });

                // parse file
                try {
                    core.getLogger().v(TAG, "parsing file < " + file.getAbsolutePath());
                    while ((in.read(buffer) > 0) && !done) {
                        buffer.flip();
                        done = parser.read(buffer);
                        buffer.clear();
                    }
                    if(!done) {
                        throw new RxParserException("file is corrupted");
                    }
                } catch (RxParserException | IOException rpe) {
                    //rpe.printStackTrace();
                    core.getLogger().w(TAG, "deleting corrupted bundle: " + file.getName());
                    file.delete();
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /* ========= STORING FILES =========== */

    private static class SerializationContext {
        public File fbundle;
        public BufferedOutputStream out;
        public FileBlob blob;
    }

    @Override
    public Completable store(Bundle bundle) {
        if (!isEnabled()) {
            return Completable.error(new StorageApi.StorageUnavailableException());
        }

        if (index.contains(bundle.bid)) {
            return Completable.error(new StorageApi.BundleAlreadyExistsException());
        }

        return Single.just(bundle)
                .map(b -> {
                    // we remove the payload from the bundle if it is a file blob
                    SerializationContext ctx = new SerializationContext();
                    ctx.fbundle = createBundleFile(bundleDir, b.bid);

                    // serialize the payload in a file blob (unless it is one already)
                    ctx.blob = serializeBlob(b);
                    b.getPayloadBlock().data = new NullBlob();

                    ctx.out = new BufferedOutputStream(new FileOutputStream(ctx.fbundle));
                    return ctx;
                })
                .flatMapCompletable(ctx -> Flowable.using(
                        () -> {
                            lock.writeLock().lock();
                            return true;
                        },
                        b -> FileStorageUtils.bundleFileEncoder(bundle, ctx.blob.getFilePath(), core.getExtensionManager().getBlockDataSerializerFactory())
                                .observe()
                                .map(buffer -> {
                                    // serialization into the file
                                    while (buffer.hasRemaining()) {
                                        ctx.out.write(buffer.get());
                                    }
                                    return buffer;
                                })
                                .doOnTerminate(() -> closeSilently(ctx.out))
                                .doOnError(e -> {
                                    bundle.tag("serialization_failed");
                                    ctx.fbundle.delete();
                                })
                                .doOnComplete(() -> {
                                    // reattach the blob
                                    bundle.getPayloadBlock().data = ctx.blob;

                                    // index the new file
                                    MetaBundle meta = new MetaBundle(bundle);
                                    index.put(meta.bid, meta, new FileStorageUtils.BundleInfo(
                                            ctx.fbundle.getAbsolutePath(),
                                            ctx.blob.getFilePath()));
                                }),
                        b -> lock.writeLock().unlock()).ignoreElements());
    }

    private FileBlob serializeBlob(Bundle bundle) throws IOException {
        lock.writeLock().lock();
        try {
            if (bundle.getPayloadBlock().data.isFileBlob()) {
                // payload is already a file, just needs to be move/copy in the right location
                FileBlob fblob = (FileBlob) bundle.getPayloadBlock().data;
                File parent = new File(fblob.getFilePath()).getParentFile();
                if (parent.equals(blobDir)) {
                    // the blob is in the right folder
                    return (FileBlob) bundle.getPayloadBlock().data;
                }
                // it needs to be moved to the blob folder
                File newfblob = FileUtil.createNewFile("blob-", ".blob", blobDir);
                fblob.moveToFile(newfblob.getAbsolutePath());
                return fblob;
            }

            // payload is not a file and needs to be serialized
            Blob vblob = bundle.getPayloadBlock().data;
            File newFile = FileUtil.createNewFile("blob-", ".blob", blobDir);
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(newFile));
            vblob.observe()
                    .map(buf -> {
                        out.write(buf.array(), buf.position(), buf.limit());
                        return buf;
                    })
                    .doOnTerminate(out::close)
                    .doOnError(e -> {
                        core.getLogger().w(TAG, "could not serialize payload in file blob: "
                                + newFile.getAbsolutePath());
                        newFile.delete();
                    })
                    .ignoreElements()
                    .blockingAwait();
            return new FileBlob(newFile);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /* ========= PULLING FILES =========== */

    private static class DeserializationContext {
        public File fbundle;
        public FileChannel in;
    }

    public Single<Bundle> get(String bid) {
        if (!isEnabled()) {
            return Single.error(new StorageApi.StorageUnavailableException());
        }

        if (!index.contains(bid)) {
            return Single.error(new StorageApi.BundleNotFoundException(bid));
        }

        return Single.just(bid)
                .map(i -> {
                    // prepare deserialization context
                    StorageIndex.IndexEntry<FileStorageUtils.BundleInfo> entry = index.pullEntry(bid);
                    File fbundle = new File(entry.attached.bundlePath);
                    if (!fbundle.exists() || !fbundle.canRead()) {
                        throw new StorageApi.StorageFailedException("can't read bundle file in storage: "
                                + entry.attached.bundlePath);
                    }
                    DeserializationContext ctx = new DeserializationContext();
                    ctx.fbundle = fbundle;
                    ctx.in = new FileInputStream(fbundle).getChannel();
                    return ctx;
                })
                .flatMap(ctx -> Single.just(FileStorageUtils.createBundleParser(core.getExtensionManager(), blobFactory, core.getLogger()))
                        .map(p -> {
                            lock.readLock().lock();
                            try {
                                ByteBuffer buffer = ByteBuffer.allocate(2048);
                                boolean done = false;
                                while ((ctx.in.read(buffer) > 0) && !done) { // read buffer from file
                                    buffer.flip();
                                    done = p.read(buffer);
                                    buffer.clear();
                                }
                                return p.<Bundle>getReg(1);
                            } finally {
                                lock.readLock().unlock();
                            }
                        })
                        .doOnTerminate(ctx.in::close)
                        .doOnError(e -> {
                            // todo: should delete the file
                        }));
    }


    /* ========= DELETING FILES =========== */

    public Completable remove(String bid) {
        if (!isEnabled()) {
            return Completable.error(StorageApi.StorageUnavailableException::new);
        }

        if (!index.contains(bid)) {
            return Completable.error(new StorageApi.BundleNotFoundException(bid));
        }

        return Single.just(bid)
                .map(i -> {
                    // prepare deserialization context
                    lock.writeLock().lock();
                    try {
                        StorageIndex.IndexEntry<FileStorageUtils.BundleInfo> entry = index.pullEntry(bid);
                        File fbundle = new File(entry.attached.bundlePath);

                        if (fbundle.exists() && !fbundle.canWrite()) {
                            throw new StorageFailedException("can't access bundle file for deletion: " + entry.attached.bundlePath);
                        }

                        core.getLogger().i(TAG, "deleting  " + bid
                                + " bundle file: "
                                + fbundle.getAbsolutePath());
                        fbundle.delete();
                        index.remove(i);

                        // we remove the blob if needed
                        File fblob = new File(entry.attached.blobPath);
                        if (fblob.exists() && !fblob.canWrite()) {
                            throw new StorageFailedException("can't access payload blob file for deletion: " + entry.attached.bundlePath);
                        }
                        core.getLogger().i(TAG, "deleting  " + bid
                                + " blob file: "
                                + fblob.getAbsolutePath());
                        fblob.delete();
                        return i;
                    } finally {
                        lock.writeLock().unlock();
                    }
                }).ignoreElement();
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
}
