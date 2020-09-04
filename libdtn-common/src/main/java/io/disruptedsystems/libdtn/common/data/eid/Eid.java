package io.disruptedsystems.libdtn.common.data.eid;

import java.net.URI;

/**
 * @author Lucien Loiseau on 04/09/20.
 */
public interface Eid {

    static boolean hasSameAuthority(URI a, URI b) {
        return a.getAuthority().equals(b.getAuthority());
    }

}
