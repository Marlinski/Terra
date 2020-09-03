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

    static boolean isDtnEid(URI uri) {
        try {
            checkValidApiEid(uri);
            return true;
        } catch(InvalidApiEid | Dtn.InvalidDtnEid e) {
            return false;
        }
    }

    static URI replaceApiMe(URI apime, URI dtn) throws URISyntaxException, Dtn.InvalidDtnEid, InvalidApiEid {
        checkValidApiEid(apime);
        checkValidDtnEid(dtn);
        return new URI(apime.getScheme(), dtn.getAuthority(), apime.getPath(), apime.getQuery(), apime.getFragment());
    }

    static URI me() {
        return URI.create("dtn://api:me/");
    }

    static URI me(String path) throws URISyntaxException {
        return new URI("dtn://api:me/"+path);
    }
}
