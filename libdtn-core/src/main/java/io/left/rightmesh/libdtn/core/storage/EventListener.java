package io.left.rightmesh.libdtn.core.storage;

import static io.left.rightmesh.libdtn.core.api.ConfigurationApi.CoreEntry.COMPONENT_ENABLE_EVENT_PROCESSING;

import io.left.rightmesh.libdtn.common.data.BundleId;
import io.left.rightmesh.libdtn.core.CoreComponent;
import io.left.rightmesh.libdtn.core.api.CoreApi;
import io.left.rightmesh.libdtn.core.events.BundleDeleted;
import io.left.rightmesh.librxbus.RxBus;
import io.left.rightmesh.librxbus.Subscribe;
import io.reactivex.Observable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * When an Event is fired, it may trigger some operation to a bundle. For instance, a new
 * ChannelOpenned may represent an opportunity to transmit a bundle, or a RegistrationActive
 * event may trigger multiple bundle to be deliver to the application agent.
 * <p>
 * In order to prevent iterating over the entire bundle storage index every time there's an event,
 * we allow other component to "register" a bundle to a specific event. The idea is thus that for
 * each event that fires, we can directly access a list of "Bundle of Interest" that will be
 * function of the event parameter. Take for example the event RegistrationActive that contains
 * the "sink" value of the registration, an EventListener would thus maintain a list as following:
 *
 * <pre>
 *    Listener for Event RegistrationActive
 *
 *     +-------+-------+-----+-------+
 *     | Sink1 | Sink2 | ... | SinkN |
 *     +-------+-------+-----+-------+
 *         |       |             |
 *         V       V             V
 *     +------+ +------+      +------+
 *     | BID1 | | BID3 |      | BID4 |
 *     +------+ +------+      +------+
 *     | BID2 |               | BID5 |
 *     +------+               +------+
 *                            | BID6 |
 *                            +------+
 * </pre>
 *
 * <p> Similarly for ChannelOpenned Event we would have the following EventListener:
 *
 * <pre>
 *    Listener for Event RegistrationActive
 *
 *     +-------+-------+-----+-------+
 *     | Peer1 | Peer2 | ... | PeerN |
 *     +-------+-------+-----+-------+
 *         |       |             |
 *         V       V             V
 *     +------+ +------+      +------+
 *     | BID1 | | BID1 |      | BID1 |
 *     +------+ +------+      +------+
 *     | BID2 |               | BID5 |
 *     +------+               +------+
 *                            | BID6 |
 *                            +------+
 * </pre>
 *
 * <p>Notice how a Bundle may appear in more than one list ? That is because it may be looking for
 * a direct link-local peer, or any other route, the first match is a win. Such structure provides
 * a short access at the cost of having to hold pointers in memory. the complexity is ~O(NxE) as
 * a bundle is not expected to be register in many different listener nor in many different sublist.
 *
 * <p>now if a bundle is actually transmitted and deleted, we must clear the bundle from all those
 * list hold by the listener.
 *
 * <p>Note that this component may be disabled if we use a fast database access for instance.
 *
 * @author Lucien Loiseau on 14/10/18.
 */
public abstract class EventListener<T> extends CoreComponent {

    private static final String TAG = "EventListener";

    private final Object lock = new Object();
    private Map<T, Set<BundleId>> watchList;
    private CoreApi core;

    /**
     * Constructor.
     *
     * @param core reference for the core.
     */
    public EventListener(CoreApi core) {
        this.core = core;
        watchList = new ConcurrentHashMap<>();
        initComponent(core.getConf(), COMPONENT_ENABLE_EVENT_PROCESSING, core.getLogger());
    }

    @Override
    protected void componentUp() {
        RxBus.register(this);
    }

    @Override
    protected void componentDown() {
        RxBus.unregister(this);
        synchronized (lock) {
            for (T key : watchList.keySet()) {
                Set<BundleId> set = watchList.get(key);
                if (set != null) {
                    set.clear();
                }
            }
            watchList.clear();
        }
    }

    /**
     * Add bundle to a watchlist.
     *
     * @param key key identifying the bundle
     * @param bid bundle id
     * @return true if the bundle was added to the watchlist, false othewise
     */
    public boolean watch(T key, BundleId bid) {
        if (!isEnabled()) {
            return false;
        }

        synchronized (lock) {
            core.getLogger().d(getComponentName(), "add bundle to a watchlist: "
                    + bid.getBidString() + " key=" + key.toString());
            return watchList.computeIfAbsent(key, k -> new HashSet<>()).add(bid);
        }
    }

    /**
     * remove bundle from all watchlist.
     *
     * @param bid bundle id
     */
    public void unwatch(BundleId bid) {
        if (!isEnabled()) {
            return;
        }

        synchronized (lock) {
            core.getLogger().d(getComponentName(), "remove bundle from a watchlist: "
                    + bid.getBidString());
            for (T key : watchList.keySet()) {
                Set<BundleId> set = watchList.get(key);
                if (set != null) {
                    set.remove(bid);
                }
            }
        }
    }

    /**
     * remove bundle from all watchlist, specifying the key.
     *
     * @param key key of the watchlist
     * @param bid bundle id
     * @return true if the bundle was successfully removed, false otherwise
     */
    public boolean unwatch(T key, BundleId bid) {
        if (!isEnabled()) {
            return false;
        }

        synchronized (lock) {
            Set<BundleId> set = watchList.get(key);
            if (set != null) {
                set.remove(bid);
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * get all the bundles that matches a key.
     *
     * @param key key of the watchlist
     * @return an observable of bundle ids
     */
    public Observable<BundleId> getBundlesOfInterest(T key) {
        if (!isEnabled()) {
            return Observable.empty();
        }

        synchronized (lock) {
            Set<BundleId> set = watchList.get(key);
            if (set == null) {
                return Observable.empty();
            }
            return Observable.fromIterable(new HashSet<>(set));
        }
    }

    @Subscribe
    public void onEvent(BundleDeleted event) {
        unwatch(event.bid);
    }

}