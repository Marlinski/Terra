package io.disruptedsystems.libdtn.core.api;

import io.reactivex.rxjava3.core.Observable;

import java.net.URI;
import java.util.Set;

/**
 * API for the main routing table.
 *
 * @author Lucien Loiseau on 27/11/18.
 */
public interface RoutingTableApi extends CoreComponentApi {

    class TableEntry {
        public URI to;
        public URI next;

        public TableEntry(URI to, URI next) {
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

        public URI getTo() {
            return to;
        }

        public URI getNext() {
            return next;
        }

        @Override
        public int hashCode() {
            return next.toString().concat(to.toString()).hashCode();
        }
    }

    /**
     * Add a route to this routing table.
     * 
     * @param to Eid of destination
     * @param nextHop Eid of Next-Hop
     */
    void addRoute(URI to, URI nextHop);

    /**
     * Remove a route from this routing table.
     *
     * @param to Eid of destination
     * @param nextHop Eid of Next-Hop
     */
    void delRoute(URI to, URI nextHop);

    /**
     * Remove all routes whose next-hop matches the parameter
     *
     * @param nextHop Eid of Next-Hop
     */
    void delRoutesWithNextHop(URI nextHop);

    /**
     * Resolve an Eid using this Routing Table and return an observable of cla-eid.
     * This method would typically be used when a bundle is to be forwarded.
     *
     * @param destination Eid of destination
     * @return Observable of cla-eids that can be used to make forward progress toward destination
     */
    Observable<URI> findClaForEid(URI destination);

    /**
     * Find all the routes that can make use of this cla-eid.
     * This method would typically be used when a new opportunity is available (a channel is open)
     * and we want to find elligible bundles from storage.
     *
     * @param claEid Eid of the channel
     * @return Observable of URI that can be forwarded through this cla-eid
     */
    Observable<URI> findEidForCla(URI claEid);

    /**
     * Dump all entries from the Routing Table.
     * @return Set of entries.
     */
    Set<TableEntry> dumpTable();

}
