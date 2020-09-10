package io.disruptedsystems.libdtn.core.storage;

import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.disruptedsystems.libdtn.common.data.BaseBlockFactory;
import io.disruptedsystems.libdtn.common.data.BlockFactory;
import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.bundlev7.parser.BaseBlockDataParserFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.parser.BlockDataParserFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.processor.BaseBlockProcessorFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.processor.BlockProcessorFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.serializer.BaseBlockDataSerializerFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.serializer.BlockDataSerializerFactory;
import io.disruptedsystems.libdtn.common.utils.Log;
import io.disruptedsystems.libdtn.common.utils.SimpleLogger;
import io.disruptedsystems.libdtn.core.CoreConfiguration;
import io.disruptedsystems.libdtn.core.MockCore;
import io.disruptedsystems.libdtn.core.MockExtensionManager;
import io.disruptedsystems.libdtn.core.api.ConfigurationApi;
import io.disruptedsystems.libdtn.core.api.CoreApi;
import io.disruptedsystems.libdtn.core.api.ExtensionManagerApi;
import io.disruptedsystems.libdtn.core.api.StorageApi;
import io.disruptedsystems.libdtn.core.storage.simple.SimpleStorage;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static io.disruptedsystems.libdtn.core.api.ConfigurationApi.CoreEntry.BLOB_VOLATILE_MAX_SIZE;
import static io.disruptedsystems.libdtn.core.api.ConfigurationApi.CoreEntry.COMPONENT_ENABLE_STORAGE;
import static io.disruptedsystems.libdtn.core.api.ConfigurationApi.CoreEntry.PERSISTENCE_STORAGE_PATH;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test class for SimpleStorage.
 *
 * @author Lucien Loiseau on 04/10/18.
 */
public class FileStorageTest {

    private CoreConfiguration conf = new CoreConfiguration();
    private File dir = new File("/tmp/bundle/");
    private StorageApi storage;
    private CoreApi mockCore = mockCore();


    private void cleanup() {
        File file = new File("/tmp/bundle/");
        file.delete();
        File file2 = new File("/tmp/blob/");
        file2.delete();
    }

    /* mocking the core */
    private CoreApi mockCore() {
        return new MockCore() {
            @Override
            public ConfigurationApi getConf() {
                return conf;
            }

            @Override
            public ExtensionManagerApi getExtensionManager() {
                return new MockExtensionManager() {
                    @Override
                    public BlockDataParserFactory getBlockDataParserFactory() {
                        return new BaseBlockDataParserFactory();
                    }

                    @Override
                    public BlockFactory getBlockFactory() {
                        return new BaseBlockFactory();
                    }

                    @Override
                    public BlockDataSerializerFactory getBlockDataSerializerFactory() {
                        return new BaseBlockDataSerializerFactory();
                    }

                    @Override
                    public BlockProcessorFactory getBlockProcessorFactory() {
                        return new BaseBlockProcessorFactory();
                    }
                };
            }

            @Override
            public Log getLogger() {
                return new SimpleLogger().set(Log.LogLevel.DEBUG);
            }
        };
    }

    @Test
    public void testFileStorage() {
        conf.<String>get(PERSISTENCE_STORAGE_PATH).update("/tmp/");
        conf.get(BLOB_VOLATILE_MAX_SIZE).update(50000);
        conf.<Boolean>get(COMPONENT_ENABLE_STORAGE).update(true);
        testCRUD();
        conf.<Boolean>get(COMPONENT_ENABLE_STORAGE).update(false);

        conf.get(BLOB_VOLATILE_MAX_SIZE).update(0);
        conf.<Boolean>get(COMPONENT_ENABLE_STORAGE).update(true);
        testCRUD();
        conf.<Boolean>get(COMPONENT_ENABLE_STORAGE).update(false);
    }

    public void testCRUD() {
        cleanup();

        try {
            ///////////// SETUP
            System.out.println("[+] SimpleStorage");
            storage = SimpleStorage.create(mockCore);
            storage.initComponent(mockCore.getConf().get(COMPONENT_ENABLE_STORAGE), mockCore.getLogger());

            System.out.println("[.] clear SimpleStorage");
            storage.clear().blockingAwait();
            assertStorageSize(0);
            assertFileStorageSize(0, dir);

            Bundle[] bundles = {
                    TestBundle.testBundle1(),
                    TestBundle.testBundle2(),
                    TestBundle.testBundle3(),
                    TestBundle.testBundle4(),
                    TestBundle.testBundle5(),
                    TestBundle.testBundle6()
            };

            ///////////// STORE

            /* store the bundles in storage */
            System.out.println("[.] store in SimpleStorage");

            Observable.fromArray(bundles).flatMapCompletable(
                    b -> storage.store(b))
                    .blockingAwait();

            // check it was correctly stored
            assertStorageSize(bundles.length);
            assertFileStorageSize(bundles.length, dir);


            ///////////// PULL
            /* pull the bundles from storage  */
            System.out.println("[.] pull from SimpleStorage");
            final ConcurrentLinkedQueue<Bundle> pulledBundles = new ConcurrentLinkedQueue<>();

            Observable.fromArray(bundles)
                    .doOnNext(b -> System.out.println("pulling: "+b.bid))
                    .flatMapCompletable(
                            b -> storage.get(b.bid)
                                    .map(pulledBundles::add)
                                    .doOnError(e -> {
                                        e.printStackTrace();
                                        System.out.println("error pulling bundle bid=" + b.bid + " reason=" + e.getMessage());
                                    })
                                    .ignoreElement())
                    .blockingAwait();

            // check it looks fine
            assertEquals(bundles.length, pulledBundles.size());
            assertFileStorageSize(bundles.length, dir);

            /* check that they are the same */
            for (Bundle bundle : pulledBundles) {
                boolean found = false;
                for (Bundle value : bundles) {
                    if (value.bid.equals(bundle.bid)) {
                        found = true;
                        assertArrayEquals(
                                flowableToByteArray(value.getPayloadBlock().data.observe()),
                                flowableToByteArray(bundle.getPayloadBlock().data.observe()));
                    }
                }
                assertTrue(found);
            }

            ///////////// INDEX THE BUNDLES (AND CLEAR CORRUPTED FILES)
            System.out.println("[.] turning component off and on");
            mockCore.getConf().get(COMPONENT_ENABLE_STORAGE).update(false);
            assertFileStorageSize(6, dir);
            assertEquals(0, (int) storage.count().blockingGet());

            // add a corupted file
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream("/tmp/bundle/bundle-1.bundle"), "utf-8"))) {
                writer.write("something");
            }
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream("/tmp/bundle/bundle-2.bundle"), "utf-8"))) {
                writer.write("else");
            }
            assertFileStorageSize(8, dir);

            // the correct files must have been indexed, corrupted one deleted
            mockCore.getConf().get(COMPONENT_ENABLE_STORAGE).update(true);
            assertEquals(6, (int) storage.count().blockingGet());
            assertFileStorageSize(6, dir);

            ///////////// CLEAR STORAGE
            System.out.println("[.] clear SimpleStorage");
            storage.clear().blockingAwait();
            assertStorageSize(0);
            assertFileStorageSize(0, dir);

            /*
            for (Bundle bundle : bundles) {
                bundle.clearBundle();
            }
            */
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            cleanup();
        }
    }

    private byte[] flowableToByteArray(Flowable<ByteBuffer> f) {
        AtomicInteger size = new AtomicInteger();
        f.subscribe(b -> size.addAndGet(b.remaining()));
        ByteBuffer ret = ByteBuffer.allocate(size.get());
        f.subscribe(ret::put);
        return ret.array();
    }

    void assertStorageSize(int expectedSize) {
        assertEquals(expectedSize, (int) storage.count().blockingGet());
    }

    void assertFileStorageSize(int expectedSize, File dir) {
        if (dir.listFiles() != null) {
            assertEquals(expectedSize, dir.listFiles().length);
        } else {
            if (expectedSize != 0) {
                fail();
            }
        }

    }


}
