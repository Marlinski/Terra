package io.disruptedsystems.libdtn.common.data.bundlev7.serializer;

import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborEncoder;
import io.disruptedsystems.libdtn.common.data.HopCountBlock;

/**
 * ScopeControlHopLimitBlockSerializer serializes a {@link HopCountBlock}.
 *
 * @author Lucien Loiseau on 04/11/18.
 */
public class HopCountBlockSerializer {

    /**
     * serializes a {@link HopCountBlock}.
     *
     * @param block to serialize.
     * @return a Cbor-encoded serialized ScopeControlHopLimitBlock.
     */
    static CborEncoder encode(HopCountBlock block) {
        return CBOR.encoder()
                .cbor_start_array(2)
                .cbor_encode_int(block.count)
                .cbor_encode_int(block.limit);
    }

}
