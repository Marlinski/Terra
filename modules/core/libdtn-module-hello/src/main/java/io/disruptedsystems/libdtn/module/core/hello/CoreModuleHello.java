package io.disruptedsystems.libdtn.module.core.hello;

import java.net.URI;
import java.nio.ByteBuffer;

import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.PayloadBlock;
import io.disruptedsystems.libdtn.common.data.HopCountBlock;
import io.disruptedsystems.libdtn.common.data.blob.VolatileBlob;
import io.disruptedsystems.libdtn.common.data.blob.WritableBlob;
import io.disruptedsystems.libdtn.common.data.eid.Eid;
import io.disruptedsystems.libdtn.core.api.CoreApi;
import io.disruptedsystems.libdtn.core.api.RegistrarApi;
import io.disruptedsystems.libdtn.core.events.LinkLocalEntryDown;
import io.disruptedsystems.libdtn.core.events.LinkLocalEntryUp;
import io.disruptedsystems.libdtn.core.spi.ClaChannelSpi;
import io.disruptedsystems.libdtn.core.spi.CoreModuleSpi;
import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborParser;
import io.marlinski.libcbor.rxparser.RxParserException;
import io.marlinski.librxbus.RxBus;
import io.marlinski.librxbus.Subscribe;
import io.reactivex.rxjava3.core.Completable;

import static io.disruptedsystems.libdtn.core.api.BundleProtocolApi.TAG_CLA_ORIGIN_IID;

/**
 * <p>CoreHelloModule is a Core Module that reacts to new peer event. For each new peer, it sends
 * an hello message containing all the local Eids of the current node. </p>
 *
 * <p>When an Hello message is received by a peer, it updates the Routing Table and adds an entry
 * matching the received peer local Eid with the ClaEid  of the ClaChannel this hello message
 * was received from.</p>
 *
 * @author Lucien Loiseau on 13/11/18.
 */
public class CoreModuleHello implements CoreModuleSpi {

    private static final String TAG = "HelloModule";
    private static final URI HELLO_URI = URI.create("dtn://link-local/~hello");

    private CoreApi core;
    private String cookie;

    @Override
    public String getModuleName() {
        return "hello";
    }

    private Bundle prepareHelloBundle() {
        HelloMessage hello = new HelloMessage();

        /* add node local Eid */
        hello.eids.add(core.getLocalEidTable().nodeId());

        /* add aliases */
        hello.eids.addAll(core.getLocalEidTable().aliases());

        /* add local node registration */
        hello.eids.addAll(core.getRegistrar().allRegistrations());

        /* get size of hello message for the payload */
        long size = hello.encode().observe()
                .map(ByteBuffer::remaining)
                .reduce(0, Integer::sum)
                .blockingGet();

        /* serialize the hello message into a Blob (for the payload) */
        VolatileBlob blobHello = new VolatileBlob((int) size);
        final WritableBlob wblob = blobHello.getWritableBlob();
        hello.encode().observe()
                .map(wblob::write)
                .doOnComplete(wblob::close)
                .subscribe();

        /* create Hello Bundle Skeleton */
        Bundle bundle = new Bundle(HELLO_URI);
        bundle.addBlock(new PayloadBlock(blobHello));
        bundle.addBlock(new HopCountBlock());
        return bundle;
    }

    @Override
    public void init(CoreApi api) {
        this.core = api;
        prepareHelloBundle();

        try {
            this.cookie = api.getRegistrar().register(HELLO_URI, (bundle) -> {
                if (bundle.getTagAttachment("cla-origin-iid") != null) {
                    core.getLogger().i(TAG, "received hello message from: "
                            + bundle.getSource()
                            + " on cla-eid: "
                            + bundle.<Eid>getTagAttachment(TAG_CLA_ORIGIN_IID));

                    CborParser parser = CBOR.parser()
                            .cbor_parse_custom_item(
                                    HelloMessage::new,
                                    (p, t, item) -> {
                                        for (URI eid : item.eids) {
                                            core.getRoutingTable().addRoute(
                                                    eid,
                                                    bundle.getTagAttachment(TAG_CLA_ORIGIN_IID));
                                        }
                                    });

                    bundle.getPayloadBlock().data.observe().subscribe(
                            b -> {
                                try {
                                    while (b.hasRemaining() && !parser.isDone()) {
                                        parser.read(b);
                                    }
                                } catch (RxParserException rpe) {
                                    core.getLogger().i(TAG, "malformed hello message: "
                                            + rpe.getMessage());
                                }
                            });
                } else {
                    core.getLogger().i(TAG, "received hello message from: "
                            + bundle.getSource()
                            + " but the " + TAG_CLA_ORIGIN_IID + " tag is missing - ignoring");
                }

                return Completable.complete();
            });
        } catch (RegistrarApi.EidAlreadyRegistered
                | RegistrarApi.InvalidEid
                | RegistrarApi.RegistrarDisabled
                | RegistrarApi.NullArgument re) {
            api.getLogger().e(TAG, "initialization failed: " + re.getMessage());
            return;
        }

        RxBus.register(this);
    }

    /**
     * For every new peer that is connected, we send a hello message.
     *
     * @param up event
     */
    @Subscribe
    public void onEvent(LinkLocalEntryUp up) {
        if (up.channel.getMode().equals(ClaChannelSpi.ChannelMode.InUnidirectional)) {
            return;
        }

        core.getLogger().i(TAG, "sending hello message to: " + up.channel.channelEid());
        up.channel.sendBundle(prepareHelloBundle(),
                core.getExtensionManager().getBlockDataSerializerFactory())
                .subscribe(
                        b -> {
                            /* ignore */
                        },
                        err -> {
                            core.getLogger().v(TAG, "hello failed to be sent to: "
                                    + up.channel.channelEid());
                        },
                        () -> {
                            core.getLogger().v(TAG, "hello sent to: "
                                    + up.channel.channelEid());
                        });
    }

    /**
     * When a peer disconnect, we remove the corresponding entries from the routing table.
     *
     * @param up event
     */
    @Subscribe
    public void onEvent(LinkLocalEntryDown up) {
        core.getLogger().i(TAG, "removing routes associated with: " + up.channel.channelEid());
        core.getRoutingTable().delRoutesWithNextHop(up.channel.channelEid());
    }

}
