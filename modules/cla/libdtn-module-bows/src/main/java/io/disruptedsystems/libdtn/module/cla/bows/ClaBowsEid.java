package io.disruptedsystems.libdtn.module.cla.bows;

import java.net.URI;
import java.net.URISyntaxException;

import io.disruptedsystems.libdtn.common.data.eid.BaseClaEid;
import io.disruptedsystems.libdtn.common.data.eid.Eid;
import io.disruptedsystems.libdtn.common.data.eid.EidFormatException;

/**
 * ClaStcpEid is a special Eid whose scheme is "cla:stcp".
 * It is used to identify ConvergenceLayerStcp Channel.
 *
 * @author Lucien Loiseau on 17/10/18.
 */
public class ClaBowsEid extends BaseClaEid {

    private URI uri;

    // unsafe constructor
    private ClaBowsEid(URI uri) {
        super("bows", uri.toASCIIString());
        this.uri = uri;
    }

    static ClaBowsEid unsafe(URI uri) {
        return new ClaBowsEid(uri);
    }

    /**
     * Constructor. A bows eid follows the following pattern "cla:bows:<URL><[?&]>sink=<SINK>
     * that is, "cla:bows" followed by a valid URL with the sink being part of the query parameter.
     *
     * @param url  websocket url
     * @param sink eid registration
     * @throws EidFormatException if the supplied parameters are not valid
     */
    public ClaBowsEid(String url, String sink) throws EidFormatException {
        super("bows", checkURI(url).toASCIIString(), sink);
    }

    public ClaBowsEid(URI url, String sink) throws EidFormatException {
        super("bows", url.toASCIIString(), sink);
    }

    @Override
    public String getClaSpecificPart() {
        URI uri = URI.create(claParameters);
        if(claSink == null) {
            return uri.toASCIIString();
        }

        try {
            return appendUri(uri, "sink=" + claSink).toASCIIString();
        } catch(URISyntaxException e) {
            return uri.toASCIIString();
        }
    }

    @Override
    public int ianaNumber() {
        return EID_CLA_IANA_VALUE;
    }

    @Override
    public ClaBowsEid copy() {
        ClaBowsEid ret = new ClaBowsEid(uri);
        ret.claSink = this.claSink;
        return ret;
    }

    protected static URI appendUri(URI uri, String appendQuery) throws URISyntaxException {
        if (appendQuery == null) {
            return uri;
        }

        String newQuery = uri.getQuery();
        if (newQuery == null || newQuery.equals("")) {
            newQuery = appendQuery;
        } else {
            newQuery += "&" + appendQuery;
        }

        return new URI(uri.getScheme(), uri.getAuthority(),
                uri.getPath(), newQuery, uri.getFragment());
    }

    protected static URI checkURI(String uri) throws EidFormatException {
        try {
            return new URI(uri);
        } catch(URISyntaxException e) {
            throw new EidFormatException(e.getMessage());
        }
    }
}
