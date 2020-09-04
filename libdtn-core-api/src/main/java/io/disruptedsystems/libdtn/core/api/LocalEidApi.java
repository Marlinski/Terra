package io.disruptedsystems.libdtn.core.api;

import java.net.URI;
import java.util.Set;


/**
 * API to request the local Eid of the node.
 *
 * @author Lucien Loiseau on 26/10/18.
 */
public interface LocalEidApi {

    enum LookUpResult {
        eidIsNotLocal,
        eidMatchAARegistration,
        eidMatchNodeId,
        eidMatchCla,
    }

    /**
     * Return the configured local Eid for current node.
     *
     * @return local Eid
     */
    URI nodeId();

    /**
     * Return the set of all aliases for current node.
     *
     * @return Set of Eid
     */
    Set<URI> aliases();

    /**
     * Check if an EID is one of the current node-id (either local node-id or an alias).
     *
     * @return true if eid is a local node id, false otherwise.
     */
    URI isEidNodeId(URI eid);

    /**
     * check if an Eid is local. It checks if the eid
     * - matches a registered eid by an application agent
     * - matches one of the local node-id or aliases
     * - matches a cla-eid in the link-local table.
     *
     * @param eid to check
     * @return the result of the lookup
     */
    LookUpResult isEidLocal(URI eid);
}
