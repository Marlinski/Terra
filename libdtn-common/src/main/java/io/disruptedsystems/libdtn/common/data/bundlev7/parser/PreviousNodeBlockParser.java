package io.disruptedsystems.libdtn.common.data.bundlev7.parser;

import io.disruptedsystems.libdtn.common.utils.Log;
import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborParser;
import io.disruptedsystems.libdtn.common.data.PreviousNodeBlock;
import io.disruptedsystems.libdtn.common.data.eid.EidFactory;

/**
 * PreviousNodeBlockParser parses the data-specific part of the PreviousNode block.
 *
 * @author Lucien Loiseau on 04/11/18.
 */
public class PreviousNodeBlockParser {

    static CborParser getParser(PreviousNodeBlock block, EidFactory eidFactory, Log logger) {
        return CBOR.parser()
                .cbor_parse_custom_item(
                        () -> new EidItem(eidFactory, logger),
                        (p, t, item) -> block.previous = item.eid);
    }

}
