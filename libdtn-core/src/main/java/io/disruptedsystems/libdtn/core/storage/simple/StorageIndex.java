package io.disruptedsystems.libdtn.core.storage.simple;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.disruptedsystems.libdtn.common.data.Bundle;

/**
 * @author Lucien Loiseau on 10/09/20.
 */
class StorageIndex<T> {

    public static class IndexEntry<T> {
        Bundle bundle;      /* either a bundle or a metabundle */
        T attached;

        IndexEntry(Bundle bundle) {
            this.bundle = bundle;
        }
        IndexEntry(Bundle bundle, T attached) {
            this.bundle = bundle;
            this.attached = attached;
        }
    }

    Map<String, IndexEntry<T>> index = new ConcurrentHashMap<>();

    void teardown() {
        index.clear();
    }

    Bundle put(String bid, Bundle bundle) {
        IndexEntry<T> entry = new IndexEntry<T>(bundle);
        index.put(bid, entry);
        bundle.tag("in_storage");
        return bundle;
    }

    Bundle put(String bid, Bundle bundle, T attached) {
        IndexEntry<T> entry = new IndexEntry<T>(bundle, attached);
        index.put(bid, entry);
        bundle.tag("in_storage");
        return bundle;
    }

    Bundle pullBundle(String bid) {
        if(!index.containsKey(bid)) {
            return null;
        }
        return index.get(bid).bundle;
    }

    IndexEntry<T> pullEntry(String bid) {
        if(!index.containsKey(bid)) {
            return null;
        }
        return index.get(bid);
    }

    void remove(String bid) {
        index.remove(bid);
    }

    int size() {
        return index.size();
    }

    boolean contains(String bid) {
        return index.containsKey(bid);
    }

    Set<String> allBid() {
        return index.keySet();
    }
}
