package io.disruptedsystems.libdtn.common.data.eid;

import java.net.URI;
import java.net.URISyntaxException;

import static io.disruptedsystems.libdtn.common.data.eid.Dtn.checkAuthorityNotNull;
import static io.disruptedsystems.libdtn.common.data.eid.Dtn.checkPathNotNull;
import static io.disruptedsystems.libdtn.common.data.eid.Dtn.checkSchemeNotNull;

/**
 * @author Lucien Loiseau on 04/09/20.
 */
public interface Eid {

    /**
     * return only the endpoint part of the URI, that is without the query string nor the fragment.
     *
     * @param a the URI
     * @return a copy of the URI reduced to the endpoint alone.
     */
    static URI getEndpoint(URI a) {
        try {
            return new URI(a.getScheme(), a.getAuthority(), a.getPath(), null, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException();
        }
    }

    static String getDemux(URI a) {
        return a.getPath()
                + ((a.getQuery() != null) ? "?" + a.getQuery() : "")
                + ((a.getFragment() != null) ? "#" + a.getFragment() : "");
    }


    static boolean hasSameScheme(URI a, URI b) {
        try {
            checkSchemeNotNull(a);
            checkSchemeNotNull(b);
            return a.getScheme().equals(b.getScheme());
        } catch (Dtn.InvalidDtnEid e) {
            return false;
        }
    }

    static boolean hasSameAuthority(URI a, URI b) {
        try {
            checkAuthorityNotNull(a);
            checkAuthorityNotNull(b);
            return a.getAuthority().equals(b.getAuthority());
        } catch (Dtn.InvalidDtnEid e) {
            return false;
        }
    }

    static boolean hasSamePath(URI a, URI b) {
        try {
            checkPathNotNull(a);
            checkPathNotNull(b);
            return a.getPath().equals(b.getPath());
        } catch (Dtn.InvalidDtnEid e) {
            return false;
        }
    }

    static boolean matchAuthority(URI a, URI b) {
        if (!hasSameScheme(a, b)) {
            return false;
        }
        if (!hasSameAuthority(a, b)) {
            return false;
        }
        return true;
    }

    static boolean matchEndpoint(URI a, URI b) {
        if (!hasSameScheme(a, b)) {
            return false;
        }
        if (!hasSameAuthority(a, b)) {
            return false;
        }
        if (!hasSamePath(a, b)) {
            return false;
        }
        return true;
    }


}
