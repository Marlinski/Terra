package io.disruptedsystems.libdtn.core.api;

import java.util.function.Supplier;

import io.disruptedsystems.libdtn.common.ExtensionToolbox;
import io.disruptedsystems.libdtn.common.data.CanonicalBlock;
import io.disruptedsystems.libdtn.common.data.bundlev7.processor.BlockProcessor;
import io.marlinski.libcbor.CborEncoder;
import io.marlinski.libcbor.CborParser;

/**
 * API for the extension manager. It enables adding a new extension Block or a new Eid.
 *
 * @author Lucien Loiseau on 22/11/18.
 */
public interface ExtensionManagerApi extends ExtensionToolbox {

    class BlockTypeAlreadyManaged extends Exception {
    }

    /**
     * Add a new ExtensionBlock.
     *
     * @param type       block PAYLOAD_BLOCK_TYPE
     * @param block      block supplier
     * @param parser     parser supplier
     * @param serializer serializer supplier
     * @param processor  block processor supplier
     * @throws BlockTypeAlreadyManaged if the block is already managed
     */
    void addExtensionBlock(int type,
                           Supplier<CanonicalBlock> block,
                           Supplier<CborParser> parser,
                           Supplier<CborEncoder> serializer,
                           Supplier<BlockProcessor> processor) throws BlockTypeAlreadyManaged;
}
