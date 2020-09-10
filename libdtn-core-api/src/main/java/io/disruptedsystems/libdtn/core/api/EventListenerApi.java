package io.disruptedsystems.libdtn.core.api;

import io.reactivex.rxjava3.core.Observable;

/**
 * An EventListener groups bundle together if they share a common "key". It is used to track
 * event and trigger bundle-related batch operation.
 *
 * @author Lucien Loiseau on 20/01/19.
 */
public interface EventListenerApi<T> {

    /**
     * Add bundle to a watchlist.
     *
     * @param key key identifying the bundle
     * @param bid bundle id
     * @return true if the bundle was added to the watchlist, false othewise
     */
    boolean watch(T key, String bid);

    /**
     * remove bundle from all watchlist.
     *
     * @param bid bundle id
     */
    void unwatch(String bid);

    /**
     * remove bundle from all watchlist, specifying the key.
     *
     * @param key key of the watchlist
     * @param bid bundle id
     * @return true if the bundle was successfully removed, false otherwise
     */
    boolean unwatch(T key, String bid);


    /**
     * get all the bundles that matches a key.
     *
     * @param key key of the watchlist
     * @return an observable of bundle ids
     */
    Observable<String> getBundlesOfInterest(T key);

}
