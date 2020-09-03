package io.disruptedsystems.libdtn.common.data.bundlev7.parser;

import java.net.URI;
import java.net.URISyntaxException;

import io.disruptedsystems.libdtn.common.data.eid.Dtn;
import io.disruptedsystems.libdtn.common.utils.Log;
import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborParser;
import io.marlinski.libcbor.rxparser.RxParserException;

import static io.disruptedsystems.libdtn.common.data.eid.Dtn.EID_DTN_IANA_VALUE;
import static io.disruptedsystems.libdtn.common.data.eid.Ipn.EID_IPN_IANA_VALUE;

/**
 * EidItem is a CborParser.ParseableItem for an eid
 *
 * @author Lucien Loiseau on 04/11/18.
 */
public class EidItem implements CborParser.ParseableItem {

    public EidItem(Log logger) {
        this.logger = logger;
    }

    private Log logger;

    public URI eid;
    public int ianaNumber;
    private long node;

    @Override
    public CborParser getItemParser() {
        return CBOR.parser()
                .cbor_open_array(2)
                .cbor_parse_int((p, t, i) -> {
                    logger.v(BundleV7Item.TAG, ".. iana_value=" + i);
                    this.ianaNumber = (int) i;
                    switch ((int) i) {
                        case EID_IPN_IANA_VALUE:
                            p.insert_now(parseIpn);
                            break;
                        case EID_DTN_IANA_VALUE:
                            p.insert_now(parseDtn);
                            break;
                        default:
                            p.insert_now(parseEid);
                    }
                });
    }

    private CborParser parseIpn = CBOR.parser()
            .cbor_open_array(2)
            .cbor_parse_int((p, t, node) -> {
                logger.v(BundleV7Item.TAG, ".. ipn_node =" + node);
                this.node = node;
            })
            .cbor_parse_int((p, t, service) -> {
                logger.v(BundleV7Item.TAG, ".. ipn_service =" + service);
                eid = URI.create("ipn:" + node + "." + service);
            });

    private CborParser parseDtn = CBOR.parser()
            .cbor_or(
                    CBOR.parser().cbor_parse_int((p, t, i) -> {
                        eid = Dtn.nullEid();
                    }),
                    CBOR.parser().cbor_parse_text_string_full(
                            (p, t, size) -> {
                                if (size > 1024) {
                                    throw new RxParserException("Eid size should not exceed 1024");
                                }
                            },
                            (p, str) -> {
                                logger.v(BundleV7Item.TAG, ".. dtn_ssp=" + str);
                                try {
                                    eid = new URI("dtn", str, null);
                                } catch (URISyntaxException efe) {
                                    throw new RxParserException("invalid eid: " + efe);
                                }
                            }));

    private CborParser parseEid = CBOR.parser()
            .cbor_parse_text_string_full(
                    (p, t, size) -> {
                        if (size > 1024) {
                            throw new RxParserException("Eid size should not exceed 1024");
                        }
                    },
                    (p, ssp) -> {
                        try {
                            eid = new URI(ianaNumber+":"+ssp);
                            logger.v(BundleV7Item.TAG, ".. eid scheme=" + ianaNumber + " ssp=" + ssp);
                        } catch (URISyntaxException efe) {
                            throw new RxParserException("error parsing eid: " + efe.getMessage());
                        }
                    });
}
