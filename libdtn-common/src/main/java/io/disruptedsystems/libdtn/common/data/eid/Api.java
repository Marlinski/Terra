package io.disruptedsystems.libdtn.common.data.eid;

import java.net.URI;
import java.net.URISyntaxException;

import static io.disruptedsystems.libdtn.common.data.eid.Dtn.checkValidDtnEid;

/**
 * @author Lucien Loiseau on 04/09/20.
 */
public interface Api {

    class InvalidApiEid extends Exception{
    }

    static void checkValidApiEid(URI uri) throws Dtn.InvalidDtnEid, InvalidApiEid {
        checkValidDtnEid(uri);
        if(!uri.getAuthority().equals("api:me")) {
            throw new InvalidApiEid();
        }
    }

    static boolean isApiEid(URI uri) {
        try {
            checkValidApiEid(uri);
            return true;
        } catch(InvalidApiEid | Dtn.InvalidDtnEid e) {
            return false;
        }
    }

    /**
     * swapApiMeUnsafe replace the authority of uri1 with that of uri2 but keep
     * the scheme, path, query and fragment of uri1 intact.
     *
     * So this is not api:me specific, this is mostly use to swap "api:me" in and out.
     *
     * @param uri1 swap into
     * @param uri2 swap from
     * @return
     */
    static URI swapApiMe(URI uri1, URI uri2) throws URISyntaxException, Dtn.InvalidDtnEid {
        checkValidDtnEid(uri1);
        checkValidDtnEid(uri2);
        return Dtn.create(uri2.getAuthority(), uri1.getPath(), uri1.getQuery(), uri1.getFragment());
    }

    static URI swapApiMeUnsafe(URI uri1, URI uri2) {
        try {
            return swapApiMe(uri1, uri2);
        } catch(URISyntaxException | Dtn.InvalidDtnEid e) {
            throw new IllegalArgumentException(e);
        }
    }

    static URI me() {
        return URI.create("dtn://api:me/");
    }

    static URI me(String path) throws URISyntaxException, Dtn.InvalidDtnEid, InvalidApiEid {
        return me(path, null);
    }

    static URI me(String path, String query) throws URISyntaxException, Dtn.InvalidDtnEid, InvalidApiEid {
       return me(path, query, null);
    }

    static URI me(String path, String query, String fragment) throws URISyntaxException, Dtn.InvalidDtnEid, InvalidApiEid {
        URI uri = Dtn.create("api:me", path, query, fragment);
        checkValidApiEid(uri);
        return uri;
    }
}
