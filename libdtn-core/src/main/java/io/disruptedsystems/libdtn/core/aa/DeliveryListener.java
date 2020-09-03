package io.disruptedsystems.libdtn.core.aa;

import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.eid.ApiEid;
import io.disruptedsystems.libdtn.common.data.eid.DtnEid;
import io.disruptedsystems.libdtn.common.data.eid.EidFormatException;
import io.disruptedsystems.libdtn.core.api.CoreApi;
import io.disruptedsystems.libdtn.core.api.LocalEidApi;
import io.disruptedsystems.libdtn.core.events.RegistrationActive;
import io.disruptedsystems.libdtn.core.storage.EventListener;
import io.marlinski.librxbus.Subscribe;

/**
 * DeliveryListener listen for active registration and forward matching undelivered bundle.
 */
public class DeliveryListener extends EventListener<String> {

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
        watch(bundle.getDestination().getEidString(), bundle.bid);
        if (bundle.getDestination() instanceof DtnEid) {
            try {
                ApiEid apiMeSwap = new ApiEid(((DtnEid) bundle.getDestination()).getPath());
                watch(apiMeSwap.getEidString(), bundle.bid);
            } catch (EidFormatException e) {
                core.getLogger().e(TAG, "couldn't swap api:me name for this eid: "
                        + bundle.getDestination().getEidString()
                        + " -- " + bundle.bid.getBidString());
                /* ignore */
            }
        }
    }

    public void unwatch(Bundle bundle) {
        unwatch(bundle.getDestination().getEidString(), bundle.bid);
        if (bundle.getDestination() instanceof DtnEid) {
            try {
                ApiEid apiMeSwap = new ApiEid(((DtnEid) bundle.getDestination()).getPath());
                unwatch(apiMeSwap.getEidString(), bundle.bid);
            } catch (EidFormatException e) {
                core.getLogger().e(TAG, "couldn't swap api:me name for this eid: "
                        + bundle.getDestination().getEidString()
                        + " -- " + bundle.bid.getBidString());
                /* ignore */
            }
        }
    }

}
