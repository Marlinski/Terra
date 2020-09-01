package io.disruptedsystems.libdtn.module.cla.bows;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketExtension;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.net.URI;
import java.nio.ByteBuffer;

import io.disruptedsystems.libdtn.common.ExtensionToolbox;
import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.blob.BlobFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.parser.BundleV7Item;
import io.disruptedsystems.libdtn.common.data.bundlev7.serializer.BlockDataSerializerFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.serializer.BundleV7Serializer;
import io.disruptedsystems.libdtn.common.utils.Log;
import io.disruptedsystems.libdtn.common.utils.NullLogger;
import io.disruptedsystems.libdtn.core.api.ConfigurationApi;
import io.disruptedsystems.libdtn.core.spi.ClaChannelSpi;
import io.disruptedsystems.libdtn.core.spi.ConvergenceLayerSpi;
import io.disruptedsystems.libdtn.common.data.eid.ClaEid;
import io.disruptedsystems.libdtn.common.data.eid.ClaEidParser;
import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborEncoder;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subscribers.DisposableSubscriber;

/**
 * Bundle Over WebSocket (BOWS) is a WebSocket Convergence Layer Adapter for the Bundle Protocol.
 * it was introduced by Scott Burleigh in 2018 as an alternative to the quite complicated TCPCLv4.
 *
 * <p> A ClaBowsEid session is initiated by a websocket-client to a websocket-server. Once the
 * connection is open, it is bidirectional and bundles may flow to or from the remote server.
 * Bundles can be send without signalling needed. Each bundle is represented as a cbor
 * array with only two items, first item being a cbor integer value representing the length of the
 * serialized bundle followed by the serialized bundle itself. The connection can be shutdown by
 * any peer without any signalling needed. </p>
 *
 * @author Lucien Loiseau on 17/08/18.
 */
public class ConvergenceLayerBows implements ConvergenceLayerSpi {

    private static final String TAG = "ConvergenceLayerStcp";

    private Log logger = new NullLogger();

    public String getModuleName() {
        return "bows";
    }

    /**
     * Default constructor.
     */
    public ConvergenceLayerBows() {
    }

    @Override
    public ClaEidParser getClaEidParser() {
        return new ClaBowsEidParser();
    }

    @Override
    public Observable<ClaChannelSpi> start(ConfigurationApi conf, Log logger) {
        return Observable.error(new Throwable("operation not supported"));
    }

    @Override
    public void stop() {
    }

    @Override
    public Single<ClaChannelSpi> open(ClaEid peer) {
        if (peer instanceof ClaBowsEid) {
            return open(((ClaBowsEid) peer).claParameters);
        } else {
            return Single.error(new Throwable("peer is not a ConvergenceLayerStcp peer"));
        }
    }

    private Single<ClaChannelSpi> open(String webSocketUrl) {
        return Single.just(webSocketUrl)
                .map(url -> new URI(url).toASCIIString())
                .map(uri -> new WebSocketFactory()
                        .createSocket(uri)
                        .setPingInterval(0)
                        .addExtension(WebSocketExtension.PERMESSAGE_DEFLATE)
                        .connect())
                .map(Channel::new);
    }


    public class Channel implements ClaChannelSpi {

        WebSocket  ws;
        ClaBowsEid channelEid;

        /**
         * Constructor.
         *
         * @param ws a connected websocket
         */
        public Channel(WebSocket ws) {
            this.ws = ws;
            channelEid = ClaBowsEid.unsafe(ws.getURI());
            logger.i(TAG, "new ClaBowsEid CLA channel openned: " + channelEid.getEidString());
        }

        @Override
        public ChannelMode getMode() {
            return ChannelMode.BiDirectional;
        }

        @Override
        public ClaBowsEid channelEid() {
            return channelEid;
        }

        @Override
        public ClaBowsEid localEid() { return null; }

        @Override
        public void close() {
            if (ws != null) {
                try {
                    //ws.removeListener(webSocketListener);
                    ws.disconnect();
                    ws = null;
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        @Override
        public Observable<Integer> sendBundle(Bundle bundle,
                                              BlockDataSerializerFactory serializerFactory) {
            Flowable<ByteBuffer> job = createBundleJob(bundle, serializerFactory);
            if (job == null) {
                return Observable.error(new Throwable("Cannot serialize the bundle"));
            }
            return Observable.error(new Throwable("Cannot serialize the bundle"));
        }


        @Override
        public Observable<Integer> sendBundles(Flowable<Bundle> upstream,
                                               BlockDataSerializerFactory serializerFactory) {
            return Observable.create(s -> {
                upstream.subscribe(new DisposableSubscriber<Bundle>() {
                    int bundleSent;

                    @Override
                    protected void onStart() {
                        bundleSent = 0;
                        request(1);
                    }

                    @Override
                    public void onNext(Bundle bundle) {
                        sendBundle(bundle, serializerFactory).subscribe(
                                i -> {
                                },
                                e -> request(1),
                                () -> {
                                    s.onNext(++bundleSent);
                                    request(1);
                                }
                        );
                    }

                    @Override
                    public void onError(Throwable t) {
                        s.onError(t);
                    }

                    @Override
                    public void onComplete() {
                        s.onComplete();
                    }
                });
            });
        }

        @Override
        public Observable<Bundle> recvBundle(ExtensionToolbox toolbox,
                                             BlobFactory blobFactory) {
            return Observable.error(new Throwable("todo"));
        }

        Flowable<ByteBuffer> createBundleJob(Bundle b,
                                             BlockDataSerializerFactory serializerFactory) {
            CborEncoder encodedB = BundleV7Serializer.encode(b, serializerFactory);
            long[] size = {0};
            encodedB.observe().subscribe(
                    buffer -> {
                        size[0] += buffer.remaining();
                    },
                    e -> {
                        size[0] = -1;
                    });
            if (size[0] < 0) {
                return null;
            }
            return CBOR.encoder()
                    .cbor_start_array(2)
                    .cbor_encode_int(size[0])
                    .merge(encodedB)
                    .observe(2048);
        }

    }

}