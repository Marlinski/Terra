package io.disruptedsystems.libdtn.core.api;

import java.util.Set;

import io.disruptedsystems.libdtn.common.data.eid.ClaEid;
import io.disruptedsystems.libdtn.common.data.eid.DtnEid;
import io.disruptedsystems.libdtn.common.data.eid.Eid;

/**
 * API to request the local Eid of the node.
 *
 * @author Lucien Loiseau on 26/10/18.
 */
public interface LocalEidApi {

    abstract class LocalEid<T> {
        public T eid;

        LocalEid(T eid) {
            this.eid = eid;
        }

        public static Registered registered(String eid) {
            return new Registered(eid);
        }

        public static NodeId alias(Eid eid) {
            return new NodeId(eid);
        }

        public static LinkLocal link(ClaEid eid) {
            return new LinkLocal(eid);
        }
    }

    class Registered extends LocalEid<String> {
        public Registered(String eid) {
            super(eid);
        }
    }

    class NodeId extends LocalEid<Eid> {
        public NodeId(Eid eid) {
            super(eid);
        }
    }

    class LinkLocal extends LocalEid<ClaEid> {
        public LinkLocal(ClaEid eid) {
            super(eid);
        }
    }

    /**
     * Return the configured local Eid for current node.
     *
     * @return local Eid
     */
    DtnEid nodeId();

    /**
     * Return the set of all aliases for current node.
     *
     * @return Set of Eid
     */
    Set<Eid> aliases();

    /**
     * Check if an EID is a node id (either local node-id or an alias).
     *
     * @return true if eid is a local node id, false otherwise.
     */
    Eid isEidNodeId(Eid eid);

    /**
     * check if an Eid is local. It checks if the eid
     * - matches one of the local node-id or aliases
     * - matches a registered eid by an application agent
     * - matches a CLA Eid in the link-local table.
     *
     * @param eid to check
     * @return true if eid is local, false otherwise.
     */
    LocalEid isEidLocal(Eid eid);
}
