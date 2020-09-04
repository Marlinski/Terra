package io.disruptedsystems.libdtn.common.data.bundlev7.serializer;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import io.disruptedsystems.libdtn.common.data.eid.Dtn;
import io.disruptedsystems.libdtn.common.data.eid.Ipn;
import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborEncoder;

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
        if (Ipn.isIpnEid(eid)) {
            return CBOR.encoder()
                    .cbor_start_array(2)
                    .cbor_encode_int(Ipn.EID_IPN_IANA_VALUE)
                    .cbor_start_array(2)
                    .cbor_encode_int(Ipn.getNodeNumberUnsafe(eid))
                    .cbor_encode_int(Ipn.getServiceNumberUnsafe(eid));
        }
        if (eid.equals(Dtn.nullEid())) {
            return CBOR.encoder()
                    .cbor_start_array(2)
                    .cbor_encode_int(Dtn.EID_DTN_IANA_VALUE)
                    .cbor_encode_int(0);
        }
        if (Dtn.isDtnEid(eid)) {
            return CBOR.encoder()
                    .cbor_start_array(2)
                    .cbor_encode_int(Dtn.EID_DTN_IANA_VALUE)
                    .cbor_encode_text_string(eid.getSchemeSpecificPart());
        }

        // todo should probably return an error instead..
        return CBOR.encoder()
                .cbor_start_array(2)
                .cbor_encode_int(encodeSchemeToLong(eid.getScheme())) // should encode it in a long
                .cbor_encode_text_string(eid.getSchemeSpecificPart());
    }

    private static long encodeSchemeToLong(String str) {
        byte[] buf = new byte[8];
        for (int i = 0; i < str.length() && i < 8; i++) {
            buf[i] = (byte) str.charAt(i);
        }
        return (ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).getLong());
    }

}
