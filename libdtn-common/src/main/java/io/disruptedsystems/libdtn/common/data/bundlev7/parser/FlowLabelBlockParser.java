package io.disruptedsystems.libdtn.common.data.bundlev7.parser;

import io.disruptedsystems.libdtn.common.utils.Log;
import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborParser;
import io.disruptedsystems.libdtn.common.data.FlowLabelBlock;

/**
 * FlowLabelBlockParser parses the data-specific part of the FlowLabelBlock block.
 *
 * @author Lucien Loiseau on 04/11/18.
 */
public class FlowLabelBlockParser {

    static CborParser getParser(FlowLabelBlock block,  Log logger) {
        return CBOR.parser(); //todo
    }

}
