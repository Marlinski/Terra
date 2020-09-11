package io.disruptedsystems.libdtn.core.storage.simple;

import org.apache.commons.collections4.trie.PatriciaTrie;

import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.eid.Api;
import io.disruptedsystems.libdtn.common.data.eid.Dtn;
import io.disruptedsystems.libdtn.core.events.BundleIndexed;
import io.marlinski.librxbus.RxBus;

import static io.disruptedsystems.libdtn.core.api.BundleProtocolApi.TAG_DELIVERY_PENDING;
import static io.disruptedsystems.libdtn.core.api.BundleProtocolApi.TAG_FORWARD_PENDING;

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
    PatriciaTrie<Set<IndexEntry<T>>> forwardingTrie = new PatriciaTrie<>();
    PatriciaTrie<Set<IndexEntry<T>>> deliveryTrie = new PatriciaTrie<>();

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

            // reverse index if bundle is to be forwarded
            if(bundle.isTagged(TAG_FORWARD_PENDING)) {
                addToTrie(forwardingTrie, bundle.getDestination().toString(), entry);
            }

            // reverse index if bundle is to be delivered
            if(bundle.isTagged(TAG_DELIVERY_PENDING)) {
                addToTrie(deliveryTrie, bundle.getDestination().toString(), entry);
            }

            // if delivery is a dtn-eid, we add the api:me as well for easier matching
            if(bundle.isTagged(TAG_DELIVERY_PENDING) && Dtn.isDtnEid(bundle.getDestination())) {
                String apimepath = Api.swapApiMeUnsafe(bundle.getDestination(), Api.me()).toString();
                addToTrie(deliveryTrie, apimepath, entry);
            }

            bundle.tag("in_storage");
            RxBus.post(new BundleIndexed(bundle));
            return bundle;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void addToTrie(PatriciaTrie<Set<IndexEntry<T>>> trie, String path, IndexEntry<T> entry) {
        Set<IndexEntry<T>> entries = trie.get(path);
        if(entries == null) {
            entries = new HashSet<>();
            trie.put(path, entries);
        }
        entries.add(entry);
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
            URI destination =  entry.bundle.getDestination();

            // remove entry from reverse index
            if(entry.bundle.isTagged(TAG_FORWARD_PENDING)) {
                removeFromTrie(forwardingTrie, destination.toString(), entry);
            }

            // remove entry from reverse index
            if(entry.bundle.isTagged(TAG_DELIVERY_PENDING)) {
                removeFromTrie(deliveryTrie, destination.toString(), entry);
            }

            // if delivery is a dtn-eid, we remove the api:me
            if(entry.bundle.isTagged(TAG_DELIVERY_PENDING) && Dtn.isDtnEid(destination)) {
                String apimepath = Api.swapApiMeUnsafe(destination, Api.me()).toString();
                removeFromTrie(deliveryTrie, apimepath.toString(), entry);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void removeFromTrie(PatriciaTrie<Set<IndexEntry<T>>> trie, String path, IndexEntry<T> entry) {
        Set<IndexEntry<T>> entries = trie.get(path);
        if (entries == null) {
            // todo: should not happen, maybe throw an error ?
            return;
        }
        entries.remove(entry);

        // remove key if mapped set is empty
        if (entries.size() == 0) {
            forwardingTrie.remove(path, entries);
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
