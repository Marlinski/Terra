package io.disruptedsystems.libdtn.core.api;

import io.disruptedsystems.libdtn.common.data.Bundle;
import io.reactivex.rxjava3.core.Single;

/**
 * A Routing Strategy provides method to process and decide how to route a Bundle.
 *
 * @author Lucien Loiseau on 19/01/19.
 */
public interface RoutingStrategyApi {
    
    class RoutingFailure extends Exception {
        public enum Reason {
            NoAlternateStrategyFound,
        }

        public Reason reason;

        public RoutingFailure(Reason reason) {
            this.reason = reason;
        }
    }

    enum RoutingStrategyResult {
        Forwarded,
        CustodyAccepted,
        CustodyRefused
    }

    /**
     * returns the Id identifying this strategy.
     *
     * @return id
     */
    int getRoutingStrategyId();

    /**
     * returns the stategy name in human readable form.
     *
     * @return name of the routing strategy
     */
    String getRoutingStrategyName();

    /**
     * Route a Bundle by executing the routing strategy.
     *
     * @param bundle to route
     * @return a RoutingStrategyResult
     */
    Single<RoutingStrategyResult> route(Bundle bundle);

}
