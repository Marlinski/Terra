package io.disruptedsystems.libdtn.core.api;

import io.disruptedsystems.libdtn.common.data.eid.ClaEid;
import io.disruptedsystems.libdtn.common.data.eid.Eid;
import io.disruptedsystems.libdtn.core.spi.ClaChannelSpi;
import io.reactivex.rxjava3.core.Maybe;

import java.util.Set;

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
    ClaEid isEidLinkLocal(Eid eid);

    /**
     * Find an open channel whose channel Eid matches the Eid requested.
     *
     * @param destination eid to find
     * @return Maybe observable with the matching ClaChannelSpi
     */
    Maybe<ClaChannelSpi> findCla(Eid destination);

    /**
     * Dump all channel from the link local table.
     *
     * @return Set of open channel
     */
    Set<ClaChannelSpi> dumpTable();

}
