package io.disruptedsystems.libdtn.common.data.bundlev7.serializer;

import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborEncoder;
import io.disruptedsystems.libdtn.common.data.ManifestBlock;

/**
 * ManifestBlockSerializer serializes a {@link ManifestBlock}.
 *
 * @author Lucien Loiseau on 04/11/18.
 */
public class ManifestBlockSerializer {

    /**
     * serializes a {@link ManifestBlock}.
     *
     * @param block to serialize.
     * @return a Cbor-encoded serialized ManifestBlock.
     */
    static CborEncoder encode(ManifestBlock block) {
        return CBOR.encoder();
    }

}
