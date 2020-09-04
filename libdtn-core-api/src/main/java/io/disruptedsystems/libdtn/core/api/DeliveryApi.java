package io.disruptedsystems.libdtn.core.api;

import io.disruptedsystems.libdtn.common.data.Bundle;
import io.reactivex.rxjava3.core.Completable;

/**
 * DeliveryApi is the registrar-side contract facing DTNCore. It lets Bundle to be delivered
 * to the correct application agent.
 *
 * @author Lucien Loiseau on 24/10/18.
 */
public interface DeliveryApi extends CoreComponentApi {

    class DeliveryFailure extends Exception {
        public enum Reason {
            DeliverySuccessful,
            DeliveryRefused,
            DeliveryDisabled,
            PassiveRegistration,
            UnregisteredEid,
        }

        public Reason reason;

        public DeliveryFailure(Reason reason) {
            this.reason = reason;
        }
    }

    /**
     * deliver a bundle. If the action completes, it means that the
     * application agent has accepted the delivery. It may fail if the aa is not currently
     * registered, passive or if the application agent has refused the delivery.
     *
     * @param localMatch the local Eid that matches this bundle.
     * @param bundle     to deliver
     * @return completable that completes upon successful delivery, onerror otherwise.
     */
    Completable deliver(LocalEidApi.LocalEid localMatch, Bundle bundle);

    /**
     * take care of this bundle for later delivery.
     * todo: probably not an ApiEid of delivery
     *
     * @param localMatch to deliver the bundle to
     * @param bundle     to deliver
     */
    void deliverLater(LocalEidApi.LocalEid localMatch, final Bundle bundle);

}
