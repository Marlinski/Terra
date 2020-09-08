package io.disruptedsystems.libdtn.module.core.hello;

import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.PayloadBlock;
import io.disruptedsystems.libdtn.common.data.blob.VolatileBlob;
import io.disruptedsystems.libdtn.common.data.blob.WritableBlob;
import io.disruptedsystems.libdtn.common.data.eid.Api;
import io.disruptedsystems.libdtn.common.data.eid.Dtn;
import io.disruptedsystems.libdtn.common.data.eid.Eid;
import io.disruptedsystems.libdtn.core.api.CoreApi;
import io.disruptedsystems.libdtn.core.api.RegistrarApi;
import io.disruptedsystems.libdtn.core.events.LinkLocalEntryUp;
import io.disruptedsystems.libdtn.core.spi.CoreModuleSpi;
import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborParser;
import io.marlinski.libcbor.rxparser.RxParserException;
import io.marlinski.librxbus.RxBus;
import io.marlinski.librxbus.Subscribe;
import io.reactivex.rxjava3.core.Completable;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

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
    private static final URI HELLO_SINK = URI.create("dtn://api:me/hello/");

    private CoreApi coreApi;
    private Bundle helloBundle;
    private String cookie;

    @Override
    public String getModuleName() {
        return "hello";
    }

    private void prepareHelloBundle() {
        HelloMessage hello = new HelloMessage();

        /* add node local Eid */
        hello.eids.add(coreApi.getLocalEidTable().nodeId());

        /* add aliases */
        hello.eids.addAll(coreApi.getLocalEidTable().aliases());

        /* get size of hello message for the payload */
        long size = hello.encode().observe()
                .map(ByteBuffer::remaining)
                .reduce(0, (a, b) -> a + b)
                .blockingGet();

        /* serialize the hello message into a Blob (for the payload) */
        VolatileBlob blobHello = new VolatileBlob((int) size);
        final WritableBlob wblob = blobHello.getWritableBlob();
        hello.encode().observe()
                .map(wblob::write)
                .doOnComplete(wblob::close)
                .subscribe();

        /* create Hello Bundle Skeleton */
        helloBundle = new Bundle(Dtn.nullEid());
        helloBundle.addBlock(new PayloadBlock(blobHello));
    }

    @Override
    public void init(CoreApi api) {
        this.coreApi = api;
        prepareHelloBundle();

        try {
            this.cookie = api.getRegistrar().register(HELLO_SINK, (bundle) -> {
                if (bundle.getTagAttachment("cla-origin-iid") != null) {
                    coreApi.getLogger().i(TAG, "received hello message from: "
                            + bundle.getSource()
                            + " on BaseClaEid: "
                            + bundle.<Eid>getTagAttachment("cla-origin-iid"));
                    CborParser parser = CBOR.parser()
                            .cbor_parse_custom_item(
                                    HelloMessage::new,
                                    (p, t, item) -> {
                                        for (URI eid : item.eids) {
                                            api.getRoutingTable().addRoute(
                                                    eid,
                                                    bundle.getTagAttachment("cla-origin-iid"));
                                        }
                                    });

                    bundle.getPayloadBlock().data.observe().subscribe(
                            b -> {
                                try {
                                    while (b.hasRemaining() && !parser.isDone()) {
                                        parser.read(b);
                                    }
                                } catch (RxParserException rpe) {
                                    api.getLogger().i(TAG, "malformed hello message: "
                                            + rpe.getMessage());
                                }
                            });
                } else {
                    coreApi.getLogger().i(TAG, "received hello message from: "
                            + bundle.getSource()
                            + " but the BaseClaEid wasn't tagged - ignoring");
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
        try {
            URI eid = Api.swapApiMe(Api.me("/hello"), up.channel.channelEid());
            coreApi.getLogger().i(TAG, "sending hello message to: " + eid);
            helloBundle.setDestination(eid);
            up.channel.sendBundle(helloBundle,
                    coreApi.getExtensionManager().getBlockDataSerializerFactory())
                    .subscribe(
                            b -> {
                                /* ignore */
                            },
                            err -> {
                                coreApi.getLogger().v(TAG, "hello failed to be sent to: "
                                        + eid);
                            },
                            () -> {
                                coreApi.getLogger().v(TAG, "hello sent to: "
                                        + eid);
                            });
        } catch (URISyntaxException | Dtn.InvalidDtnEid | Api.InvalidApiEid err) {
            coreApi.getLogger().e(TAG, "Could not send the hello bundle "
                    + err.getMessage());
        }
    }

}
