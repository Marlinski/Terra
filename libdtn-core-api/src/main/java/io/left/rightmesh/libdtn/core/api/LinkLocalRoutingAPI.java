package io.left.rightmesh.libdtn.core.api;

import java.util.Set;

import io.left.rightmesh.libdtn.common.data.eid.CLA;
import io.left.rightmesh.libdtn.common.data.eid.EID;
import io.left.rightmesh.libdtn.core.spi.cla.CLAChannelSPI;
import io.reactivex.Maybe;

/**
 * @author Lucien Loiseau on 27/11/18.
 */
public interface LinkLocalRoutingAPI {

    /**
     * Check if an EID is a local link-local EID.
     *
     * @param eid to check
     * @return the CLA-EID matching this EID, null otherwise.
     */
    CLA isEIDLinkLocal(EID eid);

    Maybe<CLAChannelSPI> findCLA(EID destination);

    Set<CLAChannelSPI> dumpTable();

}