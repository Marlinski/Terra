package io.disruptedsystems.libdtn.core.storage;

import org.junit.Test;

import io.disruptedsystems.libdtn.common.data.Bundle;
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

import static io.disruptedsystems.libdtn.core.api.ConfigurationApi.CoreEntry.COMPONENT_ENABLE_STORAGE;
import static io.disruptedsystems.libdtn.core.api.ConfigurationApi.CoreEntry.PERSISTENCE_STORAGE_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test class for VolatileStorage.
 *
 * @author Lucien Loiseau on 27/09/18.
 */
public class VolatileStorageTest {

    CoreConfiguration conf = new CoreConfiguration();
    private CoreApi mockCore = mockCore();

    /* mocking the core */
    public CoreApi mockCore() {
        return new MockCore() {
            {{
                conf.<Boolean>get(COMPONENT_ENABLE_STORAGE).update(true);
                conf.<String>get(PERSISTENCE_STORAGE_PATH).update("@DISABLED");
            }}

            @Override
            public ConfigurationApi getConf() {
                return conf;
            }

            @Override
            public ExtensionManagerApi getExtensionManager() {
                return new MockExtensionManager() {
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
                return new SimpleLogger();
            }
        };
    }

    @Test
    public void testVolatileStoreBundle() {
        System.out.println("[+] Volatile Storage");
        StorageApi storage = SimpleStorage.create(mockCore);
        storage.initComponent(
                mockCore.getConf().get(COMPONENT_ENABLE_STORAGE),
                mockCore.getLogger());

        System.out.println("[.] clear VolatileStorage");
        storage.clear().subscribe();
        assertEquals(0, (int)storage.count().blockingGet());

        Bundle[] bundles = {
                TestBundle.testBundle1(),
                TestBundle.testBundle2(),
                TestBundle.testBundle3(),
                TestBundle.testBundle4(),
                TestBundle.testBundle5(),
                TestBundle.testBundle6()
        };

        System.out.println("[.] store bundle in VolatileStorage");
        for (int i = 0; i < bundles.length; i++) {
            final int j = i;
            try {
                storage.store(bundles[j]).blockingAwait();
                assertEquals(j + 1, (int)storage.count().blockingGet());
                assertEquals(true, storage.contains(bundles[j].bid).blockingGet());
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
        }
        assertEquals(bundles.length, (int)storage.count().blockingGet());

        System.out.println("[.] clear VolatileStorage");
        storage.clear().subscribe();
        assertEquals(0, (int)storage.count().blockingGet());
    }

}
