package io.disruptedsystems.libdtn.core.routing.strategies.direct;

import java.net.URI;

import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.eid.Api;
import io.disruptedsystems.libdtn.common.data.eid.Dtn;
import io.disruptedsystems.libdtn.common.data.eid.Eid;
import io.disruptedsystems.libdtn.core.api.CoreApi;
import io.disruptedsystems.libdtn.core.events.LinkLocalEntryUp;
import io.disruptedsystems.libdtn.core.storage.EventListener;
import io.marlinski.librxbus.Subscribe;

/**
 * DirectRoutingListener tracks and groups bundle together and listen for link-up event to
 * forward bundle.
 *
 * @author Lucien Loiseau on 19/01/19.
 */
public class DirectRoutingListener extends EventListener<String> {

    public static final String TAG = "DirectRoutingListener";

    public DirectRoutingListener(CoreApi core) {
        super(core);
    }

    @Override
    public String getComponentName() {
        return TAG;
    }

    /**
     * Listen for new peer event and forward relevant bundle accordingly.
     *
     * @param event new peer
     */
    // CHECKSTYLE IGNORE LineLength
    @Subscribe
    public void onEvent(LinkLocalEntryUp event) {
        /* deliver every bundle of interest */
        core.getLogger().i(TAG, "step 1: get all bundleOfInterest " + event.channel.channelEid());
        getBundlesOfInterest(event.channel.channelEid().getAuthority()).subscribe(
                bundleID -> {
                    core.getLogger().v(TAG, "step 1.1: pull from storage "
                            + bundleID.getBidString());
                    core.getStorage().get(bundleID).subscribe(
                            bundle -> {
                                core.getLogger().v(TAG,
                                        "step 1.2-1: forward bundle "
                                        + bundleID.getBidString());
                                event.channel.sendBundle(
                                        bundle,
                                        core.getExtensionManager().getBlockDataSerializerFactory()
                                ).ignoreElements().subscribe(
                                        () -> {
                                            core.getLogger().v(TAG, "step 1.3: forward successful, resume processing " + bundleID.getBidString());
                                            this.unwatch(bundle.bid);
                                            core.getBundleProtocol()
                                                    .bundleForwardingSuccessful(bundle);
                                        },
                                        e -> {
                                            /* do nothing and wait for next opportunity */
                                            core.getLogger().v(TAG, "step 1.3: forward failed, wait next opportunity " + bundleID.getBidString());
                                        });
                            },
                            e -> {
                                core.getLogger().w(TAG,
                                        "step 1.2-2: failed to pull bundle from storage " + bundleID.getBidString() + ": " + e.getLocalizedMessage());
                            });
                });
    }
    // CHECKSTYLE END IGNORE LineLength

    // we only watch the authority part of the cla-eid
    public boolean watchBundle(URI claEid, Bundle bundle) {
        watch(claEid.getAuthority(), bundle.bid);
        return true;
    }
}

