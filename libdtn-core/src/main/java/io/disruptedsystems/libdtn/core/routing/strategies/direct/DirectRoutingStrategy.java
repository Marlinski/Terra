package io.disruptedsystems.libdtn.core.routing.strategies.direct;


import java.io.IOException;
import java.net.URI;

import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.CanonicalBlock;
import io.disruptedsystems.libdtn.common.data.bundlev7.processor.BlockProcessorFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.processor.ProcessingException;
import io.disruptedsystems.libdtn.core.api.CoreApi;
import io.disruptedsystems.libdtn.core.api.DirectRoutingStrategyApi;
import io.disruptedsystems.libdtn.core.events.LinkLocalEntryUp;
import io.disruptedsystems.libdtn.core.spi.ClaChannelSpi;
import io.marlinski.librxbus.RxBus;
import io.marlinski.librxbus.Subscribe;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

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

    public DirectRoutingStrategy(CoreApi core) {
        this.core = core;
        RxBus.register(this);
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
                .concatMapMaybe(claChannel -> claChannel
                        .sendBundle(bundle, core.getExtensionManager().getBlockDataSerializerFactory())
                        .doOnSubscribe(disposable -> prepareBundleForTransmission(bundle, claChannel))
                        .lastElement() // total bytes sent
                        .onErrorComplete())
                .map(byteSent -> RoutingStrategyResult.Forwarded)
                .firstElement() // stop when one of the cla channel worked
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
                + bundle.bid.toString() + " -> "
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
        core.getLogger().v(TAG, "forward later: " + bundle.bid);
        if (!bundle.isTagged("in_storage")) {
            return core.getStorage()
                    .store(bundle)
                    .andThen(Single.just(RoutingStrategyResult.CustodyAccepted));
        } else {
            return Single.just(RoutingStrategyResult.CustodyAccepted);
        }
    }

    @Subscribe
    public void onEvent(LinkLocalEntryUp event) {
        if (event.channel.getMode() == ClaChannelSpi.ChannelMode.InUnidirectional) {
            return;
        }

        core.getLogger().v(TAG, "pull all relevant bundles for cla: " + event.channel.channelEid());
        core.getRoutingTable()
                .reverseCla(event.channel.channelEid()) // find all destination enabled by this channel
                .flatMap(destination -> core.getStorage().findBundlesToForward(destination.toString())) // pull all relevent bundles
                .flatMap(bid -> core.getStorage().get(bid).toObservable().onErrorComplete()) // pull the bundle from storage
                .map(bundle -> { // check that the channel is open or close the whole stream
                    if (event.channel.isOpen()) {
                        throw new IOException("channel is closed");
                    }
                    return bundle;
                })
                .flatMap(bundle -> event.channel // send it
                        .sendBundle(bundle, core.getExtensionManager().getBlockDataSerializerFactory())
                        .doOnError(e -> core.getLogger().w(TAG, event.channel.channelEid()
                                + " : transmission failed for bundle "
                                + bundle.bid + " dest="
                                + bundle.getDestination().toString()))
                        .doOnComplete(() -> core.getLogger().w(TAG, event.channel.channelEid()
                                + " : bundle successfully transmitted "
                                + bundle.bid + " dest="
                                + bundle.getDestination().toString()))
                        .onErrorComplete()) // do not close the stream if this one failed
                .onErrorComplete() // do not throw exception
                .subscribeOn(Schedulers.computation()) // do not block the caller's thread
                .subscribe(); // execute!
    }

    /*
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
                        },
                        e -> {
                        },
                        () -> {
                        });
        return Single.just(RoutingStrategyResult.CustodyAccepted);
    */

}
