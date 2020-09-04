package io.disruptedsystems.libdtn.core.api;

import java.net.URI;
import java.util.Set;


/**
 * API to request the local Eid of the node.
 *
 * @author Lucien Loiseau on 26/10/18.
 */
public interface LocalEidApi {

    abstract class LocalEid {
        public URI eid;

        LocalEid(URI eid) {
            this.eid = eid;
        }

        public static Registered registered(URI eid) {
            return new Registered(eid);
        }

        public static NodeId alias(URI eid) {
            return new NodeId(eid);
        }

        public static LinkLocal link(URI eid) {
            return new LinkLocal(eid);
        }
    }

    class Registered extends LocalEid {
        public Registered(URI eid) {
            super(eid);
        }
    }

    class NodeId extends LocalEid {
        public NodeId(URI eid) {
            super(eid);
        }
    }

    class LinkLocal extends LocalEid {
        public LinkLocal(URI eid) {
            super(eid);
        }
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
     * Check if an EID is a node id (either local node-id or an alias).
     *
     * @return true if eid is a local node id, false otherwise.
     */
    URI isEidNodeId(URI eid);

    /**
     * check if an Eid is local. It checks if the eid
     * - matches one of the local node-id or aliases
     * - matches a registered eid by an application agent
     * - matches a CLA Eid in the link-local table.
     *
     * @param eid to check
     * @return true if eid is local, false otherwise.
     */
    LocalEid isEidLocal(URI eid);
}
