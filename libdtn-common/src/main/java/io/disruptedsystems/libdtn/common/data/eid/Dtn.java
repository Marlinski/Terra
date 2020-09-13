package io.disruptedsystems.libdtn.common.data.eid;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

/**
 * @author Lucien Loiseau on 04/09/20.
 */
public interface Dtn {

    int EID_DTN_IANA_VALUE = 1;

    class InvalidDtnEid extends Exception {
    }

    static void checkAuthorityNotNull(URI uri) throws InvalidDtnEid {
        if(uri.getAuthority() == null) {
            throw new InvalidDtnEid();
        }
    }

    static void checkSchemeSpecificPartNotNull(URI uri) throws InvalidDtnEid {
        if(uri.getSchemeSpecificPart() == null) {
            throw new InvalidDtnEid();
        }
    }

    static void checkPathNotNull(URI uri) throws InvalidDtnEid {
        if(uri.getPath() == null) {
            throw new InvalidDtnEid();
        }
    }

    static void checkSchemeNotNull(URI uri) throws InvalidDtnEid {
        if(uri.getScheme() == null) {
            throw new InvalidDtnEid();
        }
    }


    /**
     * a dtn-eid is of the form:
     * <p>
     *  dtn-uri  = "dtn:" dtn-hier-part
     *          |= "dtn:none"
     *
     *  dtn-hier-part = "//" node-name name-delim demux ; a path-rootless
     *  node-name     = 1*VCHAR
     *  name-delim    = "/"
     *  demux         = *VCHAR
     * </p>
     *
     * @param uri a dtn-eid to check
     * @throws InvalidDtnEid if the eid is invalid
     */
    static void checkValidDtnEid(URI uri) throws InvalidDtnEid {
        checkSchemeNotNull(uri);
        checkSchemeSpecificPartNotNull(uri);
        if (!uri.getScheme().equals("dtn")) {
            throw new InvalidDtnEid();
        }
        if (uri.getSchemeSpecificPart().equals("none")) {
            return;
        }
        checkAuthorityNotNull(uri);
        checkPathNotNull(uri);
        if (uri.getPath().equals("")) {
            throw new InvalidDtnEid();
        }
    }

    static boolean isDtnEid(URI uri) {
        try {
            checkValidDtnEid(uri);
            return true;
        } catch (InvalidDtnEid e) {
            return false;
        }
    }

    static boolean isNullEid(URI uri) {
        return uri.toString().equals("dtn:none");
    }

    static boolean isSingleton(URI uri) {
        return !uri.getPath().startsWith("/~");
    }

    static URI nullEid() {
        return URI.create("dtn:none");
    }

    static URI generate() {
        final String uuid = UUID.randomUUID().toString().replace("-", "");
        return URI.create("dtn://" + uuid + "/");
    }

    static URI create(String node) throws URISyntaxException {
        return new URI("dtn", node, "/", null, null);
    }

    static URI create(String node, String path) throws URISyntaxException {
        return new URI("dtn", node, path, null, null);
    }

    static URI create(String node, String path, String query) throws URISyntaxException {
        return new URI("dtn", node, path, query, null);
    }

    static URI create(String node, String path, String query, String fragment) throws URISyntaxException, InvalidDtnEid {
        URI uri = new URI("dtn", node, path, query, fragment);
        checkValidDtnEid(uri);
        return uri;
    }
}
