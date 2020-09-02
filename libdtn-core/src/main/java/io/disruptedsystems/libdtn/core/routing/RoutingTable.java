package io.disruptedsystems.libdtn.core.routing;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.disruptedsystems.libdtn.common.data.eid.BaseClaEid;
import io.disruptedsystems.libdtn.common.data.eid.ClaEid;
import io.disruptedsystems.libdtn.common.data.eid.Eid;
import io.disruptedsystems.libdtn.core.CoreComponent;
import io.disruptedsystems.libdtn.core.api.ConfigurationApi;
import io.disruptedsystems.libdtn.core.api.CoreApi;
import io.disruptedsystems.libdtn.core.api.RoutingTableApi;
import io.reactivex.rxjava3.core.Observable;

/**
 * RoutingTable is the default routing table used to take forwarding decisions. It contains
 * static mapping and up-to-date information about the connected neighborhood.
 *
 * @author Lucien Loiseau on 24/08/18.
 */
public class RoutingTable extends CoreComponent implements RoutingTableApi {

    private static final String TAG = "RoutingTable";

    // ---- RoutingTable ----
    private CoreApi core;
    private boolean staticIsEnabled;
    private Set<TableEntry> staticRoutingTable;
    private Set<TableEntry> routingTable;

    /**
     * Constructor.
     *
     * @param core reference to the core
     */
    public RoutingTable(CoreApi core) {
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
        core.getConf().<Boolean>get(ConfigurationApi.CoreEntry.COMPONENT_ENABLE_STATIC_ROUTING).observe().subscribe(
                b -> staticIsEnabled = b
        );
        core.getConf().<Map<Eid, Eid>>get(ConfigurationApi.CoreEntry.STATIC_ROUTE_CONFIGURATION).observe().subscribe(
                m -> {
                    staticRoutingTable.clear();
                    m.forEach((to, from) -> staticRoutingTable.add(
                            new RoutingTableApi.TableEntry(to, from)));
                });
    }

    @Override
    protected void componentDown() {
    }

    private Observable<TableEntry> compoundTableObservable() {
        if (staticIsEnabled) {
            return Observable.fromIterable(staticRoutingTable)
                    .concatWith(Observable.fromIterable(routingTable));
        } else {
            return Observable.fromIterable(routingTable);
        }
    }

    private Observable<Eid> lookupPotentialNextHops(Eid destination) {
        return Observable.concat(
                Observable.just(destination).filter(eid -> destination instanceof ClaEid),
                compoundTableObservable()
                        .filter(entry -> entry.to.isAuthoritativeOver(destination))
                        .map(entry -> entry.next));
    }

    private Observable<BaseClaEid> resolveEid(Eid destination, Observable<Eid> path) {
        return Observable.concat(
                lookupPotentialNextHops(destination)
                        .filter(eid -> eid instanceof BaseClaEid)
                        .map(eid -> (BaseClaEid) eid),
                lookupPotentialNextHops(destination)
                        .filter(eid -> !(eid instanceof BaseClaEid))
                        .flatMap(candidate ->
                                path.contains(candidate)
                                        .toObservable()
                                        .flatMap((b) -> {
                                            if (!b) {
                                                return resolveEid(
                                                        candidate,
                                                        path.concatWith(
                                                                Observable.just(candidate)));
                                            }
                                            return Observable.empty();
                                        })));
    }

    @Override
    public Observable<BaseClaEid> resolveEid(Eid destination) {
        if (!isEnabled()) {
            return Observable.error(new ComponentIsDownException(getComponentName()));
        }
        return resolveEid(destination, Observable.empty());
    }


    @Override
    public void addRoute(Eid to, Eid nextHop) {
        if (!isEnabled()) {
            return;
        }

        core.getLogger().i(TAG, "adding a new Route: " + to.getEidString() + " -> "
                + nextHop.getEidString());
        routingTable.add(new TableEntry(to, nextHop));
    }

    @Override
    public Set<TableEntry> dumpTable() {
        if (!isEnabled()) {
            return new HashSet<>();
        }

        Set<TableEntry> ret = new HashSet<>();
        ret.addAll(staticRoutingTable);
        ret.addAll(routingTable);
        return Collections.unmodifiableSet(ret);
    }
}
