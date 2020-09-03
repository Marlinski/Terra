package io.disruptedsystems.libdtn.common.data.bundlev7.serializer;

import java.net.URI;
import java.nio.ByteBuffer;

import io.disruptedsystems.libdtn.common.data.eid.Dtn;
import io.disruptedsystems.libdtn.common.data.eid.Ipn;
import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborEncoder;

import static java.lang.Integer.min;

/**
 * EidSerializer serializes an eid.
 *
 * @author Lucien Loiseau on 04/11/18.
 */
public class EidSerializer {

    /**
     * serializes an eid.
     *
     * @param eid to serialize.
     * @return a Cbor-encoded serialized Eid.
     */
    public static CborEncoder encode(URI eid) {
        if (eid.equals(Dtn.nullEid())) {
            return CBOR.encoder()
                    .cbor_start_array(2)
                    .cbor_encode_int(Dtn.EID_DTN_IANA_VALUE)
                    .cbor_encode_int(0);
        }
        if (Ipn.isIpnEid(eid)) {
            return CBOR.encoder()
                    .cbor_start_array(2)
                    .cbor_encode_int(Ipn.EID_IPN_IANA_VALUE)
                    .cbor_start_array(2)
                    .cbor_encode_int(Ipn.getNodeNumberUnsafe(eid))
                    .cbor_encode_int(Ipn.getServiceNumberUnsafe(eid));
        }
        return CBOR.encoder()
                .cbor_start_array(2)
                .cbor_encode_int(encodeSchemeToLong(eid.getScheme())) // should encode it in a long
                .cbor_encode_text_string(eid.getSchemeSpecificPart());
    }

    private static long encodeSchemeToLong(String str) {
        ByteBuffer bb = ByteBuffer.allocate(min(8,str.getBytes().length));
        bb.put(str.getBytes());
        bb.flip();
        return bb.getLong() + 65535;
    }

}
