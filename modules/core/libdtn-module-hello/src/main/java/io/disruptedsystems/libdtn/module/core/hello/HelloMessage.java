package io.disruptedsystems.libdtn.module.core.hello;

import io.disruptedsystems.libdtn.common.data.bundlev7.parser.EidItem;
import io.disruptedsystems.libdtn.common.data.bundlev7.serializer.EidSerializer;
import io.disruptedsystems.libdtn.common.data.eid.Eid;
import io.disruptedsystems.libdtn.common.utils.NullLogger;
import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborEncoder;
import io.marlinski.libcbor.CborParser;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;

/**
 * HelloMessage is a CborParser.ParseableItem that carries a list of Eids.
 *
 * @author Lucien Loiseau on 13/11/18.
 */
public class HelloMessage implements CborParser.ParseableItem {

    public List<URI> eids;

    HelloMessage() {
        eids = new LinkedList<>();
    }

    @Override
    public CborParser getItemParser() {
        return CBOR.parser()
                .cbor_parse_linear_array(
                        (pos) -> new EidItem(new NullLogger()),
                        (p, t, size) -> { },
                        (p, t, item) -> eids.add(item.eid),
                        (p, t, a) -> { });
    }

    /**
     * encode a HelloMessage as a CBOR stream.
     *
     * @return CborEncoder
     */
    public CborEncoder encode() {
        CborEncoder enc = CBOR.encoder()
                .cbor_start_array(eids.size());

        for (URI eid : eids) {
            enc.merge(EidSerializer.encode(eid));
        }

        return enc;
    }
}
