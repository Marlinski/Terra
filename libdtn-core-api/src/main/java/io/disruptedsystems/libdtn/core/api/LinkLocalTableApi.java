package io.disruptedsystems.libdtn.core.api;


import java.net.URI;
import java.util.Set;

import io.disruptedsystems.libdtn.core.spi.ClaChannelSpi;
import io.reactivex.rxjava3.core.Maybe;

/**
 * API for the link-local routing table.
 *
 * @author Lucien Loiseau on 27/11/18.
 */
public interface LinkLocalTableApi extends CoreComponentApi {

    /**
     * Check if an Eid is a local link-local Eid.
     *
     * @param eid to check
     * @return the BaseClaEid-Eid matching this Eid, null otherwise.
     */
    URI isEidLinkLocal(URI eid);

    /**
     * Find an open channel whose channel Eid matches the Eid requested.
     *
     * @param destination eid to find
     * @return Maybe observable with the matching ClaChannelSpi
     */
    Maybe<ClaChannelSpi> lookupCla(URI destination);

    /**
     * Dump all channel from the link local table.
     *
     * @return Set of open channel
     */
    Set<ClaChannelSpi> dumpTable();

}
