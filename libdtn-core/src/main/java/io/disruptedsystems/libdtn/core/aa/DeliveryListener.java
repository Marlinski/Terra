package io.disruptedsystems.libdtn.core.aa;

import java.net.URI;

import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.eid.Api;
import io.disruptedsystems.libdtn.common.data.eid.Dtn;
import io.disruptedsystems.libdtn.common.data.eid.Eid;
import io.disruptedsystems.libdtn.core.api.CoreApi;
import io.disruptedsystems.libdtn.core.api.LocalEidApi;
import io.disruptedsystems.libdtn.core.events.RegistrationActive;
import io.disruptedsystems.libdtn.core.storage.EventListener;
import io.marlinski.librxbus.Subscribe;

/**
 * DeliveryListener listen for active registration and forward matching undelivered bundle.
 */
public class DeliveryListener extends EventListener<URI> {

    private static final String TAG = "DeliveryListener";

    DeliveryListener(CoreApi core) {
        super(core);
    }

    @Override
    public String getComponentName() {
        return "DeliveryListener";
    }

    /**
     * React to RegistrationActive event and forward the relevent bundles.
     *
     * @param event active registration event
     */
    @Subscribe
    public void onEvent(RegistrationActive event) {
        /* deliver every bundle of interest */
        getBundlesOfInterest(event.eid).subscribe(
                bundleID -> {
                    /* retrieve the bundle */
                    core.getStorage().get(bundleID).subscribe(
                            /* deliver it */
                            bundle -> event.cb.recv(bundle).subscribe(
                                    () -> {
                                        unwatch(bundle.bid);
                                        core.getBundleProtocol()
                                                .bundleLocalDeliverySuccessful(bundle);
                                    },
                                    e -> core.getBundleProtocol()
                                            .bundleLocalDeliveryFailure(bundle, LocalEidApi.LocalEid.registered(event.eid), e)),
                            e -> {
                            });
                });
    }


    public void watch(Bundle bundle) {
        watch(Eid.getEndpoint(bundle.getDestination()), bundle.bid);
        if (Dtn.isDtnEid(bundle.getDestination())) {
            URI apiMeSwap = Api.swapApiMeUnsafe(bundle.getDestination(), Api.me());
            watch(Eid.getEndpoint(apiMeSwap), bundle.bid);
        }
    }

    public void unwatch(Bundle bundle) {
        unwatch(Eid.getEndpoint(bundle.getDestination()), bundle.bid);
        if (Dtn.isDtnEid(bundle.getDestination())) {
            URI apiMeSwap = Api.swapApiMeUnsafe(bundle.getDestination(), Api.me());
            unwatch(Eid.getEndpoint(apiMeSwap), bundle.bid);
        }
    }

}
