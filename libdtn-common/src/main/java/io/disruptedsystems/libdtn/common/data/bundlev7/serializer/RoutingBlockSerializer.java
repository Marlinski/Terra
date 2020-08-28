package io.disruptedsystems.libdtn.common.data.bundlev7.serializer;

import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborEncoder;
import io.disruptedsystems.libdtn.common.data.RoutingBlock;

/**
 * RoutingBlockSerializer serializes a {@link RoutingBlock}.
 * @author Lucien Loiseau on 19/01/19.
 */
public class RoutingBlockSerializer {

    /**
     * serializes a {@link RoutingBlock}.
     *
     * @param block to serialize.
     * @return a Cbor-encoded serialized ManifestBlock.
     */
    static CborEncoder encode(RoutingBlock block) {
        return CBOR.encoder();
    }

}
