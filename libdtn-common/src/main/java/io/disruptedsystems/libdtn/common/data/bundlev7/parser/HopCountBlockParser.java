package io.disruptedsystems.libdtn.common.data.bundlev7.parser;


import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborParser;
import io.disruptedsystems.libdtn.common.data.HopCountBlock;

/**
 * ScopeControlHopLimitBlockParser parses the data-specific of a ScopeControlHopLimitBlock.
 *
 * @author Lucien Loiseau on 04/11/18.
 */
public class HopCountBlockParser {

    static CborParser getParser(HopCountBlock block) {
        return CBOR.parser()
                .cbor_open_array(2)
                .cbor_parse_int((p, t, i) -> block.count = i)
                .cbor_parse_int((p, t, i) -> block.limit = i);
    }

}
