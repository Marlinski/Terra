package io.disruptedsystems.libdtn.core.events;

import io.disruptedsystems.libdtn.common.data.Bundle;

/**
 * BundleIndexed event is thrown when bundle was added to the global bundle index.
 *
 * @author Lucien Loiseau on 10/10/18.
 */
public class BundleIndexed implements DtnEvent {
    public Bundle bundle;

    public BundleIndexed(Bundle bundle) {
        this.bundle = bundle;
    }

    @Override
    public String toString() {
        return "Bundle indexed: bid=" + bundle.bid.toString()
                + " to: " + bundle.getDestination().toString();
    }
}
