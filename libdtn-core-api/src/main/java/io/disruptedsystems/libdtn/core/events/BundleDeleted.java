package io.disruptedsystems.libdtn.core.events;

/**
 * BundleDeleted event is thrown when a bundle is deleted.
 *
 * @author Lucien Loiseau on 14/10/18.
 */
public class BundleDeleted {

    public String bid;

    public BundleDeleted(String bid) {
        this.bid = bid;
    }

    @Override
    public String toString() {
        return "Bundle deleted: bid=" + bid;
    }
}
