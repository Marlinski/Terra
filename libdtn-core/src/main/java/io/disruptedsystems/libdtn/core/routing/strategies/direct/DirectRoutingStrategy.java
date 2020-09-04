package io.disruptedsystems.libdtn.core.routing.strategies.direct;


import java.net.URI;

import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.BundleId;
import io.disruptedsystems.libdtn.common.data.CanonicalBlock;
import io.disruptedsystems.libdtn.common.data.bundlev7.processor.BlockProcessorFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.processor.ProcessingException;
import io.disruptedsystems.libdtn.common.data.eid.Cla;
import io.disruptedsystems.libdtn.core.api.CoreApi;
import io.disruptedsystems.libdtn.core.api.DirectRoutingStrategyApi;
import io.disruptedsystems.libdtn.core.api.EventListenerApi;
import io.disruptedsystems.libdtn.core.spi.ClaChannelSpi;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

/**
 * DirectRoutingStrategy will try to forward a bundle using a directly connected peer
 * after resolving the destination Eid against the link-local table and main routing table.
 *
 * @author Lucien Loiseau on 19/01/19.
 */
public class DirectRoutingStrategy implements DirectRoutingStrategyApi {

    private static final String TAG = "DirectRouting";
    public static final int MAIN_ROUTING_STRATEGY_ID = 1;

    private CoreApi core;
    private DirectRoutingListener directListener;

    public DirectRoutingStrategy(CoreApi core) {
        this.core = core;
        this.directListener = new DirectRoutingListener(core);
    }

    @Override
    public int getRoutingStrategyId() {
        return MAIN_ROUTING_STRATEGY_ID;
    }

    @Override
    public String getRoutingStrategyName() {
        return TAG;
    }

    @Override
    public Single<RoutingStrategyResult> route(Bundle bundle) {
        return findOpenedChannelTowards(bundle.getDestination())
                .concatMapMaybe(
                        claChannel ->
                                claChannel.sendBundle(
                                        bundle,
                                        core.getExtensionManager().getBlockDataSerializerFactory())
                                        .doOnSubscribe(
                                                (disposable) ->
                                                        prepareBundleForTransmission(
                                                                bundle,
                                                                claChannel))
                                        .lastElement()
                                        .onErrorComplete())
                .map(byteSent -> RoutingStrategyResult.Forwarded)
                .firstElement()
                .toSingle()
                .onErrorReturnItem(RoutingStrategyResult.CustodyRefused);
    }

    private Observable<ClaChannelSpi> findOpenedChannelTowards(URI destination) {
        return Observable.concat(
                core.getLinkLocalTable().lookupCla(destination)
                        .toObservable(),
                core.getRoutingTable().resolveEid(destination)
                        .map(core.getLinkLocalTable()::lookupCla)
                        .flatMap(Maybe::toObservable))
                .distinct();
    }

    private void prepareBundleForTransmission(Bundle bundle, ClaChannelSpi claChannel) {
        core.getLogger().v(TAG, "5.4-4 "
                + bundle.bid.getBidString() + " -> "
                + claChannel.channelEid().toString());

        /* call block-specific routine for transmission */
        for (CanonicalBlock block : bundle.getBlocks()) {
            try {
                core.getExtensionManager()
                        .getBlockProcessorFactory()
                        .create(block.type)
                        .onPrepareForTransmission(
                                block,
                                bundle,
                                core.getLogger());
            } catch (ProcessingException | BlockProcessorFactory.ProcessorNotFoundException pe) {
                /* ignore */
            }
        }
    }

    @Override
    public Single<RoutingStrategyResult> forwardLater(final Bundle bundle) {
        if (!bundle.isTagged("in_storage")) {
            return core.getStorage()
                    .store(bundle)
                    .flatMap(this::routeLaterFromStorage);
        } else {
            return routeLaterFromStorage(bundle);
        }
    }

    private Single<RoutingStrategyResult> routeLaterFromStorage(Bundle bundle) {
        final BundleId bid = bundle.bid;
        final URI destination = bundle.getDestination();

        Observable<URI> potentialClas = core.getRoutingTable().resolveEid(destination);
        core.getLogger().v(TAG, "forward later: "
                + bundle.bid.getBidString() + " -> "
                + potentialClas.map(URI::toString).reduce((str, eid) -> str + "," + eid).blockingGet());

        /* register a listener that will listen for LinkLocalEntryUp event
         * and pull the bundle from storage if there is a match */

        // watch bundle for all potential cla-eid
        potentialClas
                .map(claeid -> directListener.watchBundle(claeid, bundle))
                .subscribe();

        // then try to force an opportunity
        potentialClas
                .distinct()
                .concatMapMaybe(claeid ->
                        Maybe.fromSingle(core.getClaManager()
                                .createOpportunity(claeid))
                                .onErrorComplete())
                .firstElement()
                .subscribe(
                        (channel) -> {
                            /* ignore - directListener will take care of forwarding it*/
                        },
                        e -> {
                            /* ignore */
                        },
                        () -> {
                            /* ignore */
                        });

        return Single.just(RoutingStrategyResult.CustodyAccepted);
    }

}
