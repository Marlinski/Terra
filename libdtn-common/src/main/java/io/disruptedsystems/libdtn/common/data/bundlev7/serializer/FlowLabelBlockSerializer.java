package io.disruptedsystems.libdtn.common.data.bundlev7.serializer;

import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborEncoder;
import io.disruptedsystems.libdtn.common.data.FlowLabelBlock;

/**
 * FlowLabelBlockSerializer serializes a {@link FlowLabelBlock}.
 *
 * @author Lucien Loiseau on 04/11/18.
 */
public class FlowLabelBlockSerializer {

    /**
     * serializes a {@link FlowLabelBlock}.
     *
     * @param block to serialize.
     * @return a Cbor-encoded serialized FlowLabelBlock.
     */
    static CborEncoder encode(FlowLabelBlock block) {
        return CBOR.encoder();
    }

}
