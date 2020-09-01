package io.disruptedsystems.libdtn.common.data.bundlev7.parser;

import static io.disruptedsystems.libdtn.common.data.eid.DtnEid.EID_DTN_IANA_VALUE;
import static io.disruptedsystems.libdtn.common.data.eid.IpnEid.EID_IPN_IANA_VALUE;

import io.disruptedsystems.libdtn.common.utils.Log;
import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborParser;
import io.marlinski.libcbor.rxparser.RxParserException;
import io.disruptedsystems.libdtn.common.data.eid.DtnEid;
import io.disruptedsystems.libdtn.common.data.eid.Eid;
import io.disruptedsystems.libdtn.common.data.eid.EidFactory;
import io.disruptedsystems.libdtn.common.data.eid.EidFormatException;
import io.disruptedsystems.libdtn.common.data.eid.IpnEid;
import io.disruptedsystems.libdtn.common.data.eid.UnknowEid;

/**
 * EidItem is a CborParser.ParseableItem for an {@link Eid}.
 *
 * @author Lucien Loiseau on 04/11/18.
 */
public class EidItem implements CborParser.ParseableItem {

    public EidItem(EidFactory eidFactory, Log logger) {
        this.eidFactory = eidFactory;
        this.logger = logger;
    }

    private EidFactory eidFactory;
    private Log logger;

    public Eid eid;
    public int ianaNumber;

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
            .cbor_parse_int((p, t, node) -> eid = new IpnEid((int) node, 0))
            .cbor_parse_int((p, t, service) -> ((IpnEid) eid).serviceNumber = (int) service);

    private CborParser parseDtn = CBOR.parser()
            .cbor_or(
                    CBOR.parser().cbor_parse_int((p, t, i) -> {
                        eid = DtnEid.nullEid();
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
                                    eid = new DtnEid(str);
                                } catch (EidFormatException efe) {
                                    throw new RxParserException("DtnEid is not an URI: " + efe);
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
                            String scheme = eidFactory.getIanaScheme(ianaNumber);
                            eid = eidFactory.create(scheme, ssp);
                            logger.v(BundleV7Item.TAG, ".. eid scheme=" + scheme + " ssp=" + ssp);
                        } catch (EidFactory.UnknownIanaNumber | EidFactory.UnknownEidScheme uin) {
                            logger.v(BundleV7Item.TAG, ".. unknown Eid=" + ianaNumber + " ssp=" + ssp);
                            try {
                                eid = new UnknowEid(ianaNumber, ssp);
                            } catch (EidFormatException efe) {
                                throw new RxParserException("error parsing Eid: "
                                        + efe.getMessage());
                            }
                        } catch (EidFormatException efe) {
                            throw new RxParserException("error parsing Eid: " + efe.getMessage());
                        }
                    });
}
