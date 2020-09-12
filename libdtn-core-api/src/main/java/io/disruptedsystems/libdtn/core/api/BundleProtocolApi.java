package io.disruptedsystems.libdtn.core.api;

import io.disruptedsystems.libdtn.common.data.Bundle;

/**
 * API for the BundleProcessor.
 *
 * @author Lucien Loiseau on 25/10/18.
 */
public interface BundleProtocolApi {

    String TAG_DISPATCH_PENDING = "dispatch_pending";
    String TAG_DELIVERY_PENDING = "delivery_pending";
    String TAG_FORWARD_PENDING = "forward_pending";
    String TAG_CLA_ORIGIN_IID = "cla-origin-iid";
    String TAG_DELETION_REASON = "reason_code";

    /**
     * Process a bundle for transmission.
     *
     * @param bundle to process
     */
    void bundleTransmission(Bundle bundle);

    /**
     * Dispatch a bundle (for delivery or forwarding).
     *
     * @param bundle to process
     */
    void bundleDispatching(Bundle bundle);

    /**
     * Call this method if delivery were successfully performed from another component.
     *
     * @param bundle to process
     */
    void bundleLocalDeliverySuccessful(Bundle bundle);

    /**
     * Call this method if another component attempted to deliver a bundle but failed.
     *
     * @param bundle to process
     * @param reason of the failure
     */
    void bundleLocalDeliveryFailure(Bundle bundle, Throwable reason);

    /**
     * Process Bundle that is expired.
     *
     * @param bundle to process
     */
    void bundleExpired(Bundle bundle);

    /**
     * Process Bundle that were received from a Convergence Layer Channel.
     *
     * @param bundle to process
     */
    void bundleReception(Bundle bundle);

    /**
     * Call this method if forwarding were successfully performed from another component.
     *
     * @param bundle to process
     */
    void bundleForwardingSuccessful(Bundle bundle);
}
