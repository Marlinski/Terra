package io.left.rightmesh.libdtn.network.cla;

import java.nio.ByteBuffer;

import io.left.rightmesh.libcbor.CBOR;
import io.left.rightmesh.libcbor.CborEncoder;
import io.left.rightmesh.libcbor.CborParser;
import io.left.rightmesh.libcbor.rxparser.RxParserException;
import io.left.rightmesh.libdtn.data.Bundle;
import io.left.rightmesh.libdtn.data.EID;
import io.left.rightmesh.libdtn.data.MetaBundle;
import io.left.rightmesh.libdtn.data.bundleV7.BundleV7Parser;
import io.left.rightmesh.libdtn.data.bundleV7.BundleV7Serializer;
import io.left.rightmesh.libdtn.storage.bundle.Storage;
import io.left.rightmesh.librxtcp.RxTCP;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subscribers.DisposableSubscriber;

/**
 * Simple TCP (STCP) is a TCP Convergence Layer Adapter (CLA) for the Bundle Protocol. it was
 * introduced by Scott Burleigh in 2018 as an alternative to the quite complicated TCPCLv4.
 * As per the author's own words:
 *
 * <pre>
 *    It is less capable than tcpcl but quite a lot simpler.
 * </pre>
 *
 * <p> An STCP session is unidirectional and bundles flow from the peer that initiated the
 * connection towards the one that passively listen for incoming connection. When the connection
 * is open, bundles can be send without signalling needed. Each bundle is represented as a cbor
 * array with only two items, first item being a cbor integer value representing the length of the
 * serialized bundle followed by the serialized bundle itself. The connection can be shutdown by
 * any peer without any signalling needed. </p>
 *
 * <p>More details can be read in the draft itself:
 * https://www.ietf.org/id/draft-burleigh-dtn-stcp-00.txt</p>
 *
 * @author Lucien Loiseau on 17/08/18.
 */
public class STCP implements CLAInterface {

    private static final String TAG = "STCP";
    private static final int defaultPort = 4778;

    public static class STCPPeer extends TCPPeer {
        public STCPPeer(String host, int port) {
            super(host, port);
        }

        @Override
        public EID getEID() {
            return new EID.CLASTCP(host, port, "");
        }
    }

    private RxTCP.Server<RxTCP.Connection> server;
    private int port;

    public static String getCLAName() {
        return "stcp";
    }

    public STCP() {
        this.port = defaultPort;
    }

    public STCP setPort(int port) {
        this.port = port;
        return this;
    }

    @Override
    public Observable<CLAChannel> start() {
        server = new RxTCP.Server(port);
        return server.start()
                .map(tcpcon -> new Channel(tcpcon, false));
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop();
        }
    }

    public static Single<CLAChannel> open(String host, int port) {
        return new RxTCP.ConnectionRequest<>(host, port)
                .connect()
                .map(con -> new Channel(con, true));
    }

    public static Single<CLAChannel> open(EID.CLA peer) {
        if (peer instanceof EID.CLASTCP) {
            return Single.error(new Throwable("wrong stcp specific EID"));
        } else {
            EID.CLASTCP stcpPeer = (EID.CLASTCP) peer;
            return open(stcpPeer.host, stcpPeer.port);
        }
    }

    public static class Channel implements CLAChannel {

        RxTCP.Connection tcpcon;
        EID.CLA channelEID;
        boolean initiator;

        /**
         * Constructor.
         *
         * @param initiator true if current node initiated the STCP connection, false otherwise
         */
        public Channel(RxTCP.Connection tcpcon, boolean initiator) {
            this.tcpcon = tcpcon;
            this.initiator = initiator;
            channelEID = new EID.CLASTCP(tcpcon.getRemoteHost(),tcpcon.getRemotePort(),"/");
        }

        @Override
        public EID.CLA channelEID() {
            return channelEID;
        }

        @Override
        public void close() {
            tcpcon.closeJobsDone();
        }

        @Override
        public Observable<Integer> sendBundle(Bundle bundle) {
            if (!initiator) {
                return Observable.error(new RecvOnlyPeerException());
            }

            /* pull the bundle from storage if necessary */
            if (bundle instanceof MetaBundle) {
                return Observable.create(s -> Storage.get(bundle.bid).subscribe(
                        b -> {
                            Flowable<ByteBuffer> job = createBundleJob(bundle);
                            if (job == null) {
                                s.onError(new Throwable("Cannot serialize the bundle"));
                            }

                            RxTCP.Connection.JobHandle handle = tcpcon.order(job);
                            handle.observe().subscribe(s::onNext);
                        },
                        s::onError));
            } else {
                Flowable<ByteBuffer> job = createBundleJob(bundle);
                if (job == null) {
                    return Observable.error(new Throwable("Cannot serialize the bundle"));
                }

                RxTCP.Connection.JobHandle handle = tcpcon.order(job);
                return handle.observe();
            }
        }


        @Override
        public Observable<Integer> sendBundles(Flowable<Bundle> upstream) {
            if (!initiator) {
                return Observable.error(new RecvOnlyPeerException());
            }

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
                        sendBundle(bundle).subscribe(
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

        public Observable<Bundle> recvBundle() {
            if (initiator) {
                return Observable.empty();
            }

            return Observable.create(s -> {
                CborParser pdu = CBOR.parser()
                        .cbor_open_array(2)
                        .cbor_parse_int((__, ___, i) -> {
                            // we might want check the length and refuse large bundle
                        })
                        .cbor_parse_custom_item(BundleV7Parser.BundleItem::new, (__, ___, item) -> {
                            s.onNext(item.bundle);
                        });

                tcpcon.recv().subscribe(
                        buffer -> {
                            try {
                                while (buffer.hasRemaining()) {
                                    if (pdu.read(buffer)) {
                                        pdu.reset();
                                    }
                                }
                            } catch (RxParserException rpe) {
                                s.onComplete();
                                close();
                            }
                        },
                        e -> {
                            s.onComplete();
                            close();
                        },
                        () -> {
                            s.onComplete();
                            close();
                        }
                );
            });
        }

        Flowable<ByteBuffer> createBundleJob(Bundle b) {
            CborEncoder encodedB = BundleV7Serializer.encode(b);
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

    public static final class SendOnlyPeerException extends Exception {
    }

    public static final class RecvOnlyPeerException extends Exception {
    }
}
