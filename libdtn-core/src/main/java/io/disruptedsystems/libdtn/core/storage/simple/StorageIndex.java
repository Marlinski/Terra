package io.disruptedsystems.libdtn.core.storage.simple;

import org.apache.commons.collections4.trie.PatriciaTrie;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.core.events.BundleIndexed;
import io.marlinski.librxbus.RxBus;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * @author Lucien Loiseau on 10/09/20.
 */
class StorageIndex<T> {

    ReadWriteLock lock = new ReentrantReadWriteLock();

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

        @Override
        public int hashCode() {
            return bundle.bid.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return bundle.bid.equals(o);
        }
    }

    /*
     * Indexes
     * -------
     * - we keep an index that maps a bid to a bundle.
     * - and a reverse index (patricia trie) that maps destination to bundles for efficient lookup
     */
    Map<String, IndexEntry<T>> bundleIndex = new ConcurrentHashMap<>();
    PatriciaTrie<Set<IndexEntry<T>>> destinationTrie = new PatriciaTrie<>();

    void teardown() {
        bundleIndex.clear();
    }

    Bundle put(String bid, Bundle bundle) {
        return put(bid, bundle, null);
    }

    Bundle put(String bid, Bundle bundle, T attached) {
        lock.writeLock().lock();
        try {
            // add in bundle index
            IndexEntry<T> entry = new IndexEntry<T>(bundle, attached);
            bundleIndex.put(bid, entry);

            // add destination in reverse index
            Set<IndexEntry<T>> entries = destinationTrie.get(bundle.getDestination().toString());
            if(entries == null) {
                entries = new HashSet<>();
                destinationTrie.put(bundle.getDestination().toString(), entries);
            }
            entries.add(entry);

            bundle.tag("in_storage");
            RxBus.post(new BundleIndexed(bundle));
            return bundle;
        } finally {
            lock.writeLock().unlock();
        }
    }

    Bundle pullBundle(String bid) {
        IndexEntry<T> entry = pullEntry(bid);
        if (entry != null) {
            return entry.bundle;
        }
        return null;
    }

    IndexEntry<T> pullEntry(String bid) {
        return bundleIndex.get(bid);
    }

    void remove(String bid) {
        lock.writeLock().lock();
        try {
            IndexEntry<T> entry = bundleIndex.get(bid);
            if (entry == null) {
                return;
            }

            // remove entry from index
            bundleIndex.remove(bid);

            // remove entry from reverse index
            Set<IndexEntry<T>> entries = destinationTrie.get(entry.bundle.getDestination().toString());
            if(entries == null) {
                // todo: should not happen, maybe throw an error ?
                return;
            }
            entries.remove(entry);

            // remove key if mapped set is empty
            if (entries.size() == 0) {
                destinationTrie.remove(entry.bundle.getDestination().toString(), entries);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    int size() {
        return bundleIndex.size();
    }

    boolean contains(String bid) {
        return bundleIndex.containsKey(bid);
    }

    Set<String> allBid() {
        return bundleIndex.keySet();
    }
}
