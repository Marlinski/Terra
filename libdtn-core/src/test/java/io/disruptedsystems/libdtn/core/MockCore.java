package io.disruptedsystems.libdtn.core;

import java.util.function.Supplier;

import io.disruptedsystems.libdtn.core.api.BundleProtocolApi;
import io.disruptedsystems.libdtn.core.api.ConfigurationApi;
import io.disruptedsystems.libdtn.core.api.CoreApi;
import io.disruptedsystems.libdtn.core.api.DeliveryApi;
import io.disruptedsystems.libdtn.core.api.ExtensionManagerApi;
import io.disruptedsystems.libdtn.core.api.LinkLocalTableApi;
import io.disruptedsystems.libdtn.core.api.LocalEidApi;
import io.disruptedsystems.libdtn.core.api.ModuleLoaderApi;
import io.disruptedsystems.libdtn.core.api.RegistrarApi;
import io.disruptedsystems.libdtn.core.api.RoutingEngineApi;
import io.disruptedsystems.libdtn.core.api.RoutingTableApi;
import io.disruptedsystems.libdtn.core.api.StorageApi;
import io.marlinski.libcbor.CborEncoder;
import io.marlinski.libcbor.CborParser;
import io.disruptedsystems.libdtn.common.data.BlockFactory;
import io.disruptedsystems.libdtn.common.data.CanonicalBlock;
import io.disruptedsystems.libdtn.common.data.bundlev7.parser.BlockDataParserFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.processor.BaseBlockProcessorFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.processor.BlockProcessor;
import io.disruptedsystems.libdtn.common.data.bundlev7.processor.BlockProcessorFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.serializer.BlockDataSerializerFactory;
import io.disruptedsystems.libdtn.common.data.eid.BaseEidFactory;
import io.disruptedsystems.libdtn.common.data.eid.ClaEidParser;
import io.disruptedsystems.libdtn.common.data.eid.EidFactory;
import io.disruptedsystems.libdtn.common.data.eid.EidSspParser;
import io.disruptedsystems.libdtn.common.utils.Log;
import io.disruptedsystems.libdtn.common.utils.SimpleLogger;
import io.disruptedsystems.libdtn.core.api.ClaManagerApi;

/**
 * @author Lucien Loiseau on 26/11/18.
 */
public class MockCore implements CoreApi {
    @Override
    public void init() {
    }

    @Override
    public ConfigurationApi getConf() {
        return null;
    }

    @Override
    public Log getLogger() {
        return new SimpleLogger();
    }

    @Override
    public LocalEidApi getLocalEid() {
        return null;
    }

    @Override
    public ExtensionManagerApi getExtensionManager() {
        return new ExtensionManagerApi() {
            @Override
            public EidFactory getEidFactory() {
                return new BaseEidFactory();
            }

            @Override
            public BlockFactory getBlockFactory() {
                return null;
            }

            @Override
            public BlockDataParserFactory getBlockDataParserFactory() {
                return null;
            }

            @Override
            public BlockDataSerializerFactory getBlockDataSerializerFactory() {
                return null;
            }

            @Override
            public BlockProcessorFactory getBlockProcessorFactory() {
                return new BaseBlockProcessorFactory();
            }

            @Override
            public void addExtensionBlock(int type, Supplier<CanonicalBlock> block, Supplier<CborParser> parser, Supplier<CborEncoder> serializer, Supplier<BlockProcessor> processor) throws BlockTypeAlreadyManaged {
            }

            @Override
            public void addExtensionClaEid(String clName, ClaEidParser parser) {
            }

            @Override
            public void addExtensionEid(int schemeId, String schemeStr, EidSspParser ssPparser) {
            }
        };
    }

    @Override
    public RoutingEngineApi getRoutingEngine() {
        return null;
    }

    @Override
    public RegistrarApi getRegistrar() {
        return null;
    }

    @Override
    public DeliveryApi getDelivery() {
        return null;
    }

    @Override
    public BundleProtocolApi getBundleProtocol() {
        return null;
    }

    @Override
    public StorageApi getStorage() {
        return null;
    }

    @Override
    public ClaManagerApi getClaManager() {
        return null;
    }

    @Override
    public LinkLocalTableApi getLinkLocalTable() {
        return null;
    }

    @Override
    public RoutingTableApi getRoutingTable() {
        return null;
    }

    @Override
    public ModuleLoaderApi getModuleLoader() {
        return null;
    }
}
