package io.disruptedsystems.libdtn.core.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import io.disruptedsystems.libdtn.common.data.eid.Cla;
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

    private static final Logger log = LoggerFactory.getLogger(RoutingTable.class);

    // ---- RoutingTable ----
    private final CoreApi core;
    private boolean staticIsEnabled;
    private final Set<TableEntry> staticRoutingTable;
    private final Set<TableEntry> routingTable;


    public RoutingTable(CoreApi core) {
        this.core = core;
        staticRoutingTable = new HashSet<>();
        routingTable = new HashSet<>();
    }

    @Override
    public String getComponentName() {
        return "RoutingTable";
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
    public Observable<URI> findClaForEid(URI destination) {
        if (!isEnabled()) {
            return Observable.error(new ComponentIsDownException(getComponentName()));
        }

        return resolveEid(destination, Observable.just(Eid.getEndpoint(destination)));
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
    public Observable<URI> findEidForCla(URI claEid) {
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

        if(routingTable.add(new TableEntry(to, nextHop))) {
            log.info("route added: " + to.toString() + " -> "
                    + nextHop.toString());
        }
    }

    @Override
    public void delRoute(URI to, URI nextHop) {
        if (!isEnabled()) {
            return;
        }

        if(routingTable.remove(new TableEntry(to, nextHop))) {
            log.info("route deleted: " + to.toString() + " -> "
                    + nextHop.toString());
        }
    }

    @Override
    public void delRoutesWithNextHop(URI nextHop) {
        for (Iterator<TableEntry> i = routingTable.iterator(); i.hasNext();) {
            TableEntry entry = i.next();
            if (entry.getTo().equals(nextHop)) {
                i.remove();
            }
        }
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
