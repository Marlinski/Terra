package io.left.rightmesh.libdtn.core.routing;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.left.rightmesh.libdtn.core.DTNConfiguration;
import io.left.rightmesh.libdtn.common.data.eid.CLA;
import io.left.rightmesh.libdtn.common.data.eid.EID;
import io.left.rightmesh.libdtn.core.DTNCore;
import io.reactivex.Observable;

import static io.left.rightmesh.libdtn.core.DTNConfiguration.Entry.COMPONENT_ENABLE_STATIC_ROUTING;
import static io.left.rightmesh.libdtn.core.DTNConfiguration.Entry.STATIC_ROUTE_CONFIGURATION;

/**
 * Static Routing is a routing component that uses the static route table to take
 * forwarding decisions.
 *
 * @author Lucien Loiseau on 24/08/18.
 */
public class RoutingTable {

    private static final String TAG = "RoutingTable";

    // ---- RoutingTable ----
    private boolean staticIsEnabled;
    private Set<TableEntry> staticRoutingTable;
    private Set<TableEntry> routingTable;

    public RoutingTable(DTNCore core) {
        staticRoutingTable = new HashSet<>();
        routingTable = new HashSet<>();
        core.getConf().<Boolean>get(COMPONENT_ENABLE_STATIC_ROUTING).observe().subscribe(
                b -> staticIsEnabled = b
        );
        core.getConf().<Map<EID, EID>>get(STATIC_ROUTE_CONFIGURATION).observe().subscribe(
                m -> {
                    staticRoutingTable.clear();
                    m.forEach((to, from) -> staticRoutingTable.add(new TableEntry(to, from)));
                });
    }


    private static class TableEntry {
        EID to;
        EID next;

        TableEntry(EID to, EID next) {
            this.to = to;
            this.next = next;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            if (o instanceof TableEntry) {
                return next.equals(o) && to.equals(o);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return next.getEIDString().concat(to.getEIDString()).hashCode();
        }
    }

    Observable<TableEntry> compoundTableObservable() {
        if(staticIsEnabled) {
            return Observable.fromIterable(staticRoutingTable)
                    .concatWith(Observable.fromIterable(routingTable));
        } else {
            return Observable.fromIterable(routingTable);
        }
    }

    Observable<EID> lookupPotentialNextHops(EID destination) {
        return Observable.concat(Observable.just(destination)
                        .filter(eid -> destination instanceof CLA),
                compoundTableObservable()
                        .filter(entry -> destination.matches(entry.to))
                        .map(entry -> entry.next));
    }

    private Observable<CLA> resolveEID(EID destination, Observable<EID> path) {
        return Observable.concat(
                lookupPotentialNextHops(destination)
                        .filter(eid -> eid instanceof CLA)
                        .map(eid -> (CLA)eid),
                lookupPotentialNextHops(destination)
                        .filter(eid -> !(eid instanceof CLA))
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

    Observable<CLA> resolveEID(EID destination) {
        return resolveEID(destination, Observable.empty());
    }

    // todo remove this
    public String print() {
        final StringBuilder sb = new StringBuilder("Routing Table:\n");
        sb.append("--------------\n\n");
        compoundTableObservable().subscribe(
                tableEntry -> {
                    sb.append(tableEntry.to.getEIDString() + " --> "+tableEntry.next.getEIDString()+"\n");
                }
        );
        sb.append("\n");
        return sb.toString();
    }
}
