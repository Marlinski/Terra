package io.disruptedsystems.libdtn.core.routing;

import io.disruptedsystems.libdtn.common.data.eid.ClaEid;
import io.disruptedsystems.libdtn.common.data.eid.DtnEid;
import io.disruptedsystems.libdtn.core.api.ConfigurationApi;
import io.disruptedsystems.libdtn.core.api.CoreApi;
import io.disruptedsystems.libdtn.core.api.LocalEidApi;
import io.disruptedsystems.libdtn.common.data.eid.Eid;
import io.disruptedsystems.libdtn.core.api.RegistrarApi;
import java.util.Set;

/**
 * LocalEidTable keeps track of current node local Eid and aliases.
 *
 * @author Lucien Loiseau on 04/09/18.
 */
public class LocalEidTable implements LocalEidApi {

    private static final String TAG = "LocalEidTable";

    private CoreApi core;

    public LocalEidTable(CoreApi core) {
        this.core = core;
        core.getLogger().i(TAG, "localEid=" + nodeId().getEidString());
    }

    public DtnEid nodeId() {
        return core.getConf().<DtnEid>get(ConfigurationApi.CoreEntry.LOCAL_EID)
                .value().copy();
    }

    public Set<Eid> aliases() {
        return core.getConf().<Set<Eid>>get(ConfigurationApi.CoreEntry.ALIASES).value();
    }

    @Override
    public Eid isEidNodeId(Eid eid) {
        if (nodeId().isAuthoritativeOver(eid)) {
            return nodeId();
        }
        for (Eid alias : aliases()) {
            if (alias.isAuthoritativeOver(eid)) {
                return alias;
            }
        }
        return null;
    }

    // all local EIDs and aliases are singleton
    // We assume that such singleton creates a namespace for which any
    // eid with such prefix belongs to.
    @Override
    public LocalEid isEidLocal(Eid eid) {
        try {
            // returns an eid if it matches with a registration
            if(core.getRegistrar().isRegistered(eid.getEidString())) {
                return new Registered(eid.getEidString());
            }
        } catch(RegistrarApi.NullArgument
                | RegistrarApi.RegistrarDisabled
                | RegistrarApi.InvalidEid ignored) {
            // do nothing
        }

        // returns true if it matches a node-id or an aliases
        Eid nodeId = isEidNodeId(eid);
        if(nodeId != null) {
            return new NodeId(nodeId);
        }

        // returns true if it matches with link local table
        ClaEid claId = core.getLinkLocalTable().isEidLinkLocal(eid);
        if(claId != null) {
            return new LinkLocal(claId);
        }

        return null;
    }
}
