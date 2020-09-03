package io.disruptedsystems.libdtn.common.data.eid;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * DtnEid is a class of Eid whose scheme is "dtn".
 *
 * @author Lucien Loiseau on 17/10/18.
 */
public class BaseDtnEid extends BaseEid implements DtnEid {

    public BaseDtnEid(String nodeName) throws EidFormatException {
        try {
            uri = new URI(nodeName);
        } catch(URISyntaxException e) {
            throw new EidFormatException(e.getMessage());
        }
    }

    @Override
    public int ianaNumber() {
        return EID_DTN_IANA_VALUE;
    }

    @Override
    public boolean isNullEndPoint() {
        return ssp.equals("none");
    }

    @Override
    public boolean isSingleton() {
        return !path.startsWith("~");
    }
}