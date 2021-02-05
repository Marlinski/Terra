package io.disruptedsystems.libdtn.core.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Set;

import io.disruptedsystems.libdtn.common.data.eid.Eid;
import io.disruptedsystems.libdtn.core.api.ConfigurationApi;
import io.disruptedsystems.libdtn.core.api.CoreApi;
import io.disruptedsystems.libdtn.core.api.LocalEidApi;
import io.disruptedsystems.libdtn.core.api.RegistrarApi;

/**
 * LocalEidTable keeps track of current node local Eid and aliases.
 *
 * @author Lucien Loiseau on 04/09/18.
 */
public class LocalEidTable implements LocalEidApi {

    private static final Logger log = LoggerFactory.getLogger(LocalEidTable.class);

    private CoreApi core;

    public LocalEidTable(CoreApi core) {
        this.core = core;
        log.info("localEid=" + nodeId().toString());
    }

    public URI nodeId() {
        return core.getConf().<URI>get(ConfigurationApi.CoreEntry.LOCAL_EID)
                .value();
    }

    public Set<URI> aliases() {
        return core.getConf().<Set<URI>>get(ConfigurationApi.CoreEntry.ALIASES).value();
    }

    @Override
    public URI isEidNodeId(URI eid) {
        if (nodeId().getAuthority().equals(eid.getAuthority())) {
            return nodeId();
        }
        for (URI alias : aliases()) {
            if (alias.getAuthority().equals(eid.getAuthority())) {
                return alias;
            }
        }
        return null;
    }

    // an EID is local if it matches a registration or if the authority
    // falls within local node namespace.
    @Override
    public LocalEidApi.LookUpResult isEidLocal(URI eid) {
        URI eidToMatch = Eid.getEndpoint(eid);

        try {
            // returns an eid if it matches with a registration
            if (core.getRegistrar().isRegistered(eidToMatch)) {
                return LookUpResult.eidMatchAARegistration;
            }
        } catch (RegistrarApi.NullArgument
                | RegistrarApi.RegistrarDisabled
                | RegistrarApi.InvalidEid ignored) {
            // do nothing
        }

        // returns true if it matches a node-id or an aliases
        URI nodeId = isEidNodeId(eid);
        if (nodeId != null) {
            return LookUpResult.eidMatchNodeId;
        }

        // returns true if it matches with an alias created by a CLA
        if (core.getClaManager().isEidLocalCla(eidToMatch)) {
            return LookUpResult.eidMatchCla;
        }

        // returns true if it matches with link local table
        URI claEid = core.getLinkLocalTable().isEidLinkLocal(eidToMatch);
        if (claEid != null) {
            return LookUpResult.eidMatchCla;
        }

        return LookUpResult.eidIsNotLocal;
    }
}
