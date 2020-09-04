package io.disruptedsystems.libdtn.common.data.eid;

import java.net.URI;
import java.net.URISyntaxException;

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
        return a.getScheme().equals(b.getScheme());
    }

    static boolean hasSameAuthority(URI a, URI b) {
        return a.getAuthority().equals(b.getAuthority());
    }

    static boolean hasSamePath(URI a, URI b) {
        return a.getPath().equals(b.getPath());
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
