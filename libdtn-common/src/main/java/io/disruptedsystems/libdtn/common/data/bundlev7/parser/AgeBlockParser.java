package io.disruptedsystems.libdtn.common.data.bundlev7.parser;

import io.disruptedsystems.libdtn.common.utils.Log;
import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborParser;
import io.disruptedsystems.libdtn.common.data.AgeBlock;

/**
 * Parser for the {@link AgeBlock}.
 *
 * @author Lucien Loiseau on 04/11/18.
 */
public class AgeBlockParser {

    static CborParser getParser(AgeBlock block, Log logger) {
        return CBOR.parser()
                .cbor_parse_int((parser, tags, i) -> block.age = i)
                .do_here((parser) -> block.start());
    }

}
