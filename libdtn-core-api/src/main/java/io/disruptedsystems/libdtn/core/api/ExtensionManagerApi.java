package io.disruptedsystems.libdtn.core.api;

import io.disruptedsystems.libdtn.common.ExtensionToolbox;
import io.disruptedsystems.libdtn.common.data.CanonicalBlock;
import io.disruptedsystems.libdtn.common.data.bundlev7.processor.BlockProcessor;
import io.disruptedsystems.libdtn.common.data.eid.ClaEidParser;
import io.disruptedsystems.libdtn.common.data.eid.EidSspParser;
import io.marlinski.libcbor.CborEncoder;
import io.marlinski.libcbor.CborParser;

import java.util.function.Supplier;

/**
 * API for the extension manager. It enables adding a new extension Block or a new Eid.
 *
 * @author Lucien Loiseau on 22/11/18.
 */
public interface ExtensionManagerApi extends ExtensionToolbox {

    class BlockTypeAlreadyManaged extends Exception {
    }

    class EidSchemeAlreadyManaged extends Exception {
    }

    class ClaNameAlreadyManaged extends Exception {
    }

    /**
     * Add a new ExtensionBlock.
     *
     * @param type block PAYLOAD_BLOCK_TYPE
     * @param block block supplier
     * @param parser parser supplier
     * @param serializer serializer supplier
     * @param processor block processor supplier
     * @throws BlockTypeAlreadyManaged if the block is already managed
     */
    void addExtensionBlock(int type,
                           Supplier<CanonicalBlock> block,
                           Supplier<CborParser> parser,
                           Supplier<CborEncoder> serializer,
                           Supplier<BlockProcessor> processor) throws BlockTypeAlreadyManaged;

    /**
     * Add a new Eid family.
     *
     * @param schemeId the ianaNumber number for this Eid scheme
     * @param schemeStr Eid scheme
     * @param ssPparser scheme specific parser
     * @throws EidSchemeAlreadyManaged if the Eid is already managed
     */
    void addExtensionEid(int schemeId,
                         String schemeStr,
                         EidSspParser ssPparser) throws EidSchemeAlreadyManaged;

    /**
     * Add a new ClaEid family.
     *
     * @param clName Eid scheme
     * @param parser scheme specific parser
     * @throws ClaNameAlreadyManaged if the Eid is already managed
     */
    void addExtensionClaEid(String clName,
                            ClaEidParser parser) throws ClaNameAlreadyManaged;
}
