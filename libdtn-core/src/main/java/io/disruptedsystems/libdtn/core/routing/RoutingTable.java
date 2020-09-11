package io.disruptedsystems.libdtn.core.routing;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.disruptedsystems.libdtn.common.data.eid.Cla;
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
        core.getConf().<Map<URI, URI>>get(ConfigurationApi.CoreEntry.STATIC_ROUTE_CONFIGURATION).observe().subscribe(
                m -> {
                    staticRoutingTable.clear();
                    m.forEach((to, from) -> staticRoutingTable.add(new RoutingTableApi.TableEntry(to, from)));
                });
    }

    @Override
    protected void componentDown() {
    }

    @Override
    public Observable<URI> resolveEid(URI destination) {
        if (!isEnabled()) {
            return Observable.error(new ComponentIsDownException(getComponentName()));
        }

        return resolveEid(destination, Observable.just(destination));
    }

    private Observable<URI> resolveEid(URI destination, Observable<URI> path) {
        return Observable.concat(
                lookupPotentialNextHops(destination) // we add all next hops cla-eid
                        .filter(Cla::isClaEid),
                lookupPotentialNextHops(destination) // and we recurse for the non cla-eid
                        .filter(eid -> !(Cla.isClaEid(eid)))
                        .filter(candidate -> !path.contains(candidate).blockingGet())
                        .flatMap(candidate -> resolveEid(candidate, path.concatWith(Observable.just(candidate)))))
                .distinct();
    }

    private Observable<URI> lookupPotentialNextHops(URI destination) {
        return Observable.just(destination)
                .concatWith(Observable.fromIterable(dumpTable())
                        .filter(entry -> (entry.to.equals(destination))) // todo || regexp
                        .map(entry -> entry.next));
    }

    @Override
    public Observable<URI> reverseCla(URI claEid) {
        if (!isEnabled()) {
            return Observable.error(new ComponentIsDownException(getComponentName()));
        }

        return reverseCla(claEid, Observable.just(claEid));
    }

    private Observable<URI> reverseCla(URI destination, Observable<URI> path) {
        return Observable.concat(
                lookupPotentialMatchesToward(destination), // we add every matches
                lookupPotentialMatchesToward(destination)  // and we recurse on them
                        .filter(candidate -> !path.contains(candidate).blockingGet())
                        .flatMap(candidate -> reverseCla(candidate, path.concatWith(Observable.just(candidate)))))
                .distinct();
    }

    private Observable<URI> lookupPotentialMatchesToward(URI destination) {
        return Observable.just(destination)
                .concatWith(Observable.fromIterable(dumpTable())
                        .filter(entry -> (entry.next.equals(destination))) // todo || regexp
                        .map(entry -> entry.to));
    }


    @Override
    public void addRoute(URI to, URI nextHop) {
        if (!isEnabled()) {
            return;
        }

        core.getLogger().i(TAG, "adding a new Route: " + to.toString() + " -> "
                + nextHop.toString());
        routingTable.add(new TableEntry(to, nextHop));
    }

    @Override
    public Set<TableEntry> dumpTable() {
        if (!isEnabled()) {
            return new HashSet<>();
        }

        Set<TableEntry> ret = new HashSet<>(routingTable);
        if (staticIsEnabled) {
            ret.addAll(staticRoutingTable);
        }
        return Collections.unmodifiableSet(ret);
    }
}
