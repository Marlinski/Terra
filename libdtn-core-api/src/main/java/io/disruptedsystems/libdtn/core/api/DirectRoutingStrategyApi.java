package io.disruptedsystems.libdtn.core.api;

import io.disruptedsystems.libdtn.common.data.Bundle;
import io.reactivex.rxjava3.core.Single;

/**
 * Api for the default direct routing strategy.
 *
 * @author Lucien Loiseau on 20/01/19.
 */
public interface DirectRoutingStrategyApi extends RoutingStrategyApi {

    /**
     * routeLater will monitor the direct neighborhood and forward the bundle if there is
     * an opportunity for direct forwarding.
     *
     * @param bundle to route
     * @return a routing decision
     */
    Single<RoutingStrategyResult> routeLater(final Bundle bundle);

}
