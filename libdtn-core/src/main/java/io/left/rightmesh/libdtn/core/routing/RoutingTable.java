package io.left.rightmesh.libdtn.core.routing;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.left.rightmesh.libdtn.common.data.eid.BaseCLAEID;
import io.left.rightmesh.libdtn.common.data.eid.EID;
import io.left.rightmesh.libdtn.core.CoreComponent;
import io.left.rightmesh.libdtn.core.api.CoreAPI;
import io.left.rightmesh.libdtn.core.api.RoutingTableAPI;
import io.reactivex.Observable;

import static io.left.rightmesh.libdtn.core.api.ConfigurationAPI.CoreEntry.COMPONENT_ENABLE_STATIC_ROUTING;
import static io.left.rightmesh.libdtn.core.api.ConfigurationAPI.CoreEntry.STATIC_ROUTE_CONFIGURATION;

/**
 * Static Routing is a routing component that uses the static route table to take
 * forwarding decisions.
 *
 * @author Lucien Loiseau on 24/08/18.
 */
public class RoutingTable extends CoreComponent implements RoutingTableAPI {

    private static final String TAG = "RoutingTable";

    // ---- RoutingTable ----
    private CoreAPI core;
    private boolean staticIsEnabled;
    private Set<TableEntry> staticRoutingTable;
    private Set<TableEntry> routingTable;


    public RoutingTable(CoreAPI core) {
        this.core = core;
        staticRoutingTable = new HashSet<>();
        routingTable = new HashSet<>();
    }

    @Override
    public String getComponentName() {
        return TAG;
    }

    @Override
    protected void componentUp() {
        core.getConf().<Boolean>get(COMPONENT_ENABLE_STATIC_ROUTING).observe().subscribe(
                b -> staticIsEnabled = b
        );
        core.getConf().<Map<EID, EID>>get(STATIC_ROUTE_CONFIGURATION).observe().subscribe(
                m -> {
                    staticRoutingTable.clear();
                    m.forEach((to, from) -> staticRoutingTable.add(new RoutingTableAPI.TableEntry(to, from)));
                });
    }

    @Override
    protected void componentDown() {
    }

    private Observable<TableEntry> compoundTableObservable() {
        if(staticIsEnabled) {
            return Observable.fromIterable(staticRoutingTable)
                    .concatWith(Observable.fromIterable(routingTable));
        } else {
            return Observable.fromIterable(routingTable);
        }
    }

    private Observable<EID> lookupPotentialNextHops(EID destination) {
        return Observable.concat(Observable.just(destination)
                        .filter(eid -> destination instanceof BaseCLAEID),
                compoundTableObservable()
                        .filter(entry -> destination.matches(entry.to))
                        .map(entry -> entry.next));
    }

    private Observable<BaseCLAEID> resolveEID(EID destination, Observable<EID> path) {
        return Observable.concat(
                lookupPotentialNextHops(destination)
                        .filter(eid -> eid instanceof BaseCLAEID)
                        .map(eid -> (BaseCLAEID)eid),
                lookupPotentialNextHops(destination)
                        .filter(eid -> !(eid instanceof BaseCLAEID))
                        .flatMap(candidate ->
                                path.contains(candidate)
                                        .toObservable()
                                        .flatMap((b) -> {
                                            if (!b) {
                                                return resolveEID(
                                                        candidate,
                                                        path.concatWith(Observable.just(candidate)));
                                            }
                                            return Observable.empty();
                                        })));
    }

    @Override
    public void addRoute(EID to, EID nextHop) {
        if(!isEnabled()) {
            return;
        }

        core.getLogger().i(TAG, "adding a new Route: " + to.getEIDString() + " -> " + nextHop.getEIDString());
        routingTable.add(new TableEntry(to, nextHop));
    }

    @Override
    public Observable<BaseCLAEID> resolveEID(EID destination) {
        if(!isEnabled()) {
            return Observable.error(new ComponentIsDownException(getComponentName()));
        }
        return resolveEID(destination, Observable.empty());
    }

    @Override
    public Set<TableEntry> dumpTable() {
        if(!isEnabled()) {
            return new HashSet<>();
        }

        Set<TableEntry> ret = new HashSet<>();
        ret.addAll(staticRoutingTable);
        ret.addAll(routingTable);
        return Collections.unmodifiableSet(ret);
    }
}
