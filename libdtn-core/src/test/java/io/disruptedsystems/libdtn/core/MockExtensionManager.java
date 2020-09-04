package io.disruptedsystems.libdtn.core;

import java.util.function.Supplier;

import io.disruptedsystems.libdtn.core.api.ExtensionManagerApi;
import io.marlinski.libcbor.CborEncoder;
import io.marlinski.libcbor.CborParser;
import io.disruptedsystems.libdtn.common.data.BlockFactory;
import io.disruptedsystems.libdtn.common.data.CanonicalBlock;
import io.disruptedsystems.libdtn.common.data.bundlev7.parser.BlockDataParserFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.processor.BlockProcessor;
import io.disruptedsystems.libdtn.common.data.bundlev7.processor.BlockProcessorFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.serializer.BlockDataSerializerFactory;

/**
 * @author Lucien Loiseau on 26/11/18.
 */
public class MockExtensionManager implements ExtensionManagerApi {
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
        return null;
    }

    @Override
    public void addExtensionBlock(int type,
                                  Supplier<CanonicalBlock> block,
                                  Supplier<CborParser> parser,
                                  Supplier<CborEncoder> serializer,
                                  Supplier<BlockProcessor> processor) throws BlockTypeAlreadyManaged {
    }
}
