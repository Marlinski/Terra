package io.disruptedsystems.libdtn.module.cla.bows;

import java.util.Base64;

import io.disruptedsystems.libdtn.common.data.eid.BaseClaEid;
import io.disruptedsystems.libdtn.common.data.eid.EidFormatException;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * ClaStcpEid is a special Eid whose scheme is "cla:stcp".
 * It is used to identify ConvergenceLayerStcp Channel.
 *
 * @author Lucien Loiseau on 17/10/18.
 */
public class ClaBowsEid extends BaseClaEid {

    public static String EID_CLA_BOWS_SCHEME = "bows";

    private URI uri;

    public URI getUri() {
        return uri;
    }

    private static String encode(URI in) {
        return Base64.getEncoder().encodeToString(in.toASCIIString().getBytes())
                .replaceAll("\\+", ".")
                .replaceAll("/", "_")
                .replaceAll("=", "-");
    }

    private static URI decode(String in) throws EidFormatException {
        try {
            return new URI(new String(Base64.getDecoder().decode(in
                    .replaceAll("\\.", "+")
                    .replaceAll("_", "/")
                    .replaceAll("-", "="))));
        } catch(URISyntaxException e) {
            throw new EidFormatException(e.getMessage());
        }
    }

    /**
     * Constructor. A bows eid follows the following pattern "cla:bows:URL
     * that is, "cla:bows" followed by a valid URL with the sink being part of the query parameter.
     *
     * @param encodedUrl   websocket url encoded in Base64
     * @param demux        dtn demux part
     * @throws EidFormatException if the supplied parameters are not valid
     */
    public ClaBowsEid(String encodedUrl, String demux) throws EidFormatException {
        super(EID_CLA_BOWS_SCHEME, encodedUrl, demux);
        this.uri = decode(encodedUrl);
    }

    public ClaBowsEid(String encodedUrl) throws EidFormatException {
        this(encodedUrl,"");
    }

    public ClaBowsEid(URI uri, String demux) throws EidFormatException {
        super(EID_CLA_BOWS_SCHEME, encode(uri),demux);
        this.uri = uri;
    }

    public ClaBowsEid(URI uri) throws EidFormatException {
        this(uri,"");
    }
}
