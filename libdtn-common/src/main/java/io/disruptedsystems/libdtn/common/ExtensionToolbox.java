package io.disruptedsystems.libdtn.common;

import io.disruptedsystems.libdtn.common.data.bundlev7.parser.BlockDataParserFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.processor.BlockProcessorFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.serializer.BlockDataSerializerFactory;
import io.disruptedsystems.libdtn.common.data.eid.EidFactory;
import io.disruptedsystems.libdtn.common.data.BlockFactory;

/**
 * <p> The DtnEid protocol can be extended by introducing new ExtensionBlock or new Eid scheme.
 * Introducing a new Block thus requires the following:
 *
 * <ul>
 * <li>Being able to instantiate a new block given its PAYLOAD_BLOCK_TYPE</li>
 * <li>Being able to parse the block data specific part</li>
 * <li>Being able to serialize the block data specific part</li>
 * <li>Being able to process the block during the bundle lifecycle</li>
 * </ul>
 * <p>
 * The ExtensionToolbox provides an easy access to all of the above. It is intended to be used
 * by modules or core component. Similarly, introducing a new Eid scheme requires the same
 * ability to instantiate a new Eid based on the scheme specific part. ExtensionToolbox also
 * provides an easy access to the Eid factory.
 *
 * @author Lucien Loiseau on 28/11/18.
 */
public interface ExtensionToolbox {

    /**
     * get the block factory to instantiate a new Block.
     *
     * @return BlockFactory
     */
    BlockFactory getBlockFactory();

    /**
     * get the block-specific data parser factory.
     *
     * @return BlockDataParserFactory
     */
    BlockDataParserFactory getBlockDataParserFactory();

    /**
     * get the block-specific data serializer factory.
     *
     * @return BlockDataSerializerFactory
     */
    BlockDataSerializerFactory getBlockDataSerializerFactory();

    /**
     * get the block-specific processor factory.
     *
     * @return BlockProcessorFactory
     */
    BlockProcessorFactory getBlockProcessorFactory();

    /**
     * get the Eid factory.
     *
     * @return EidFactory
     */
    EidFactory getEidFactory();

}
