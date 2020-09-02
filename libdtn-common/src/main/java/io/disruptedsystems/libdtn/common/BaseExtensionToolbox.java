package io.disruptedsystems.libdtn.common;

import io.disruptedsystems.libdtn.common.data.BaseBlockFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.parser.BaseBlockDataParserFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.parser.BlockDataParserFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.processor.BaseBlockProcessorFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.processor.BlockProcessorFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.serializer.BaseBlockDataSerializerFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.serializer.BlockDataSerializerFactory;
import io.disruptedsystems.libdtn.common.data.eid.BaseClaEidFactory;
import io.disruptedsystems.libdtn.common.data.eid.BaseDtnEidFactory;
import io.disruptedsystems.libdtn.common.data.eid.BaseEidFactory;
import io.disruptedsystems.libdtn.common.data.eid.EidFactory;
import io.disruptedsystems.libdtn.common.data.BlockFactory;

/**
 * BaseExtensionToolbox implements the ExtensionToolbox ApiEid and provide
 * factory for all the base {@link Block} and {@link Eid} classes.
 *
 * @author Lucien Loiseau on 28/11/18.
 */
public class BaseExtensionToolbox implements ExtensionToolbox {

    @Override
    public EidFactory getEidFactory() {
        return new BaseEidFactory(new BaseDtnEidFactory(new BaseClaEidFactory()));
    }

    @Override
    public BlockFactory getBlockFactory() {
        return new BaseBlockFactory();
    }

    @Override
    public BlockDataParserFactory getBlockDataParserFactory() {
        return new BaseBlockDataParserFactory();
    }

    @Override
    public BlockProcessorFactory getBlockProcessorFactory() {
        return new BaseBlockProcessorFactory();
    }

    @Override
    public BlockDataSerializerFactory getBlockDataSerializerFactory() {
        return new BaseBlockDataSerializerFactory();
    }

}
