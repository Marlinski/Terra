package io.disruptedsystems.libdtn.common.data.eid;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for manipulating Bundle Protocol Endpoint ID. EIDs are made of two parts:
 * a scheme and a scheme specific part (ssp).
 *
 * @author Lucien Loiseau on 20/07/18.
 */
public interface Eid {

    String RFC3986_URI_REGEXP = "^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?";

    class OperationNotSupported extends Exception {
    }

    /**
     * return the ianaNumber number associated with this Eid scheme.
     *
     * @return an int
     */
    int ianaNumber();

    /**
     * returns the scheme part of this Eid.
     *
     * @return String
     */
    String getScheme();

    /**
     * returns the scheme-specific part of this Eid.
     *
     * @return String
     */
    String getSsp();

    /**
     * returns the entire Eid string URI.
     *
     * @return String
     */
    String getEidString();

    /**
     * An EID is said to be "authoritative" if it describes a namespace under which
     * other EIDs may belongs. Usually, an authoritative Eid authorize that a service part
     * or sink be appended/encoded within.
     * <p>
     * Example of authoritative EIDs:
     * ------------------------------
     * <p>
     * dtn://local-node/
     * this is because this dtn-eid has a null demux and may act as an administrative endpoint.
     * such endpoint will match every eid that shares this prefix such as dtn://local-node/service1
     * <p>
     * ipn:50.0
     * this is because ipn eid encodes a node-id and a service-id. a service-id of 0
     * is often understood as the administrative agent of the bundle node.
     * <p>
     * cla:stcp:127.0.0.1:1234
     * this is because a cla-eid are singleton and unique to a channel. CLA eids often has a
     * concept of "sink" to denote the application layer registration.
     * <p>
     * example of non-authoritative EIDs
     * ------------------------------------
     * <p>
     * dtn://local-node/1
     * ipn:50.1
     */
    boolean isAuthoritativeEid();

    /**
     * returns true if current EID is an endpoint that has authority over another EID.
     * This usually return false if the Eid is not authoritative unless they match exactly.
     *
     * @param other eid
     * @return true if current eid has authority over the other eid.
     */
    boolean isAuthoritativeOver(Eid other);

    /**
     * Eid copy.
     *
     * @return a copy of this Eid
     */
    Eid copy();

    /**
     * Check that the Eid is a URI as defined in <a href="https://tools.ietf.org/html/rfc3986#appendix-B">RFC 3986</a>.
     *
     * <p>The Eid is considered valid if there is at least a scheme and a scheme-specific part.
     *
     * @param eid to check validity
     * @return true if valid, false otherwise
     */
    static boolean isValidEid(String eid) {
        Pattern r = Pattern.compile(RFC3986_URI_REGEXP);
        Matcher m = r.matcher(eid);
        if (m.find()) {
            String scheme = m.group(2);
            String authority = m.group(4) == null ? "" : m.group(4);
            String path = m.group(5) == null ? "" : m.group(5);
            String query = m.group(7) == null ? "" : m.group(7);
            String fragment = m.group(9) == null ? "" : m.group(9);
            String ssp = authority + path + query + fragment;
            return (scheme != null) && (!scheme.equals("")) && (!ssp.equals(""));
        } else {
            return false;
        }
    }
}
