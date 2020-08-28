package io.disruptedsystems.libdtn.common.data.bundlev7.parser;

import io.disruptedsystems.libdtn.common.utils.Log;
import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborParser;
import io.disruptedsystems.libdtn.common.data.ScopeControlHopLimitBlock;

/**
 * ScopeControlHopLimitBlockParser parses the data-specific of a ScopeControlHopLimitBlock.
 *
 * @author Lucien Loiseau on 04/11/18.
 */
public class ScopeControlHopLimitBlockParser {

    static CborParser getParser(ScopeControlHopLimitBlock block, Log logger) {
        return CBOR.parser()
                .cbor_open_array(2)
                .cbor_parse_int((p, t, i) -> block.count = i)
                .cbor_parse_int((p, t, i) -> block.limit = i);
    }

}
