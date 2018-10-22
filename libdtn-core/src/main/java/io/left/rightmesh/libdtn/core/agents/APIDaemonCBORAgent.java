package io.left.rightmesh.libdtn.core.agents;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.left.rightmesh.libcbor.CBOR;
import io.left.rightmesh.libcbor.CborParser;
import io.left.rightmesh.libcbor.rxparser.RxParserException;
import io.left.rightmesh.libdtn.core.DTNConfiguration;
import io.left.rightmesh.libdtn.core.BaseComponent;
import io.left.rightmesh.libdtn.core.DTNCore;
import io.left.rightmesh.libdtn.core.storage.blob.CoreBLOBFactory;
import io.left.rightmesh.libdtn.common.data.blob.BLOB;
import io.left.rightmesh.libdtn.common.data.blob.NullBLOB;
import io.left.rightmesh.libdtn.common.data.blob.WritableBLOB;
import io.left.rightmesh.libdtnagent.RequestMessage;
import io.left.rightmesh.libdtnagent.ResponseMessage;
import io.left.rightmesh.librxtcp.RxTCP;

import static io.left.rightmesh.libdtn.core.DTNConfiguration.Entry.COMPONENT_ENABLE_CBOR_DAEMON_API;

/**
 * @author Lucien Loiseau on 28/09/18.
 */
public class APIDaemonCBORAgent extends BaseComponent {

    private static final String TAG = "APIDaemonCBORAgent";

    private DTNCore core;
    private RxTCP.Server<RequestChannel> server;
    private final Map<RequestMessage.RequestCode, Action> ACTIONS;

    public APIDaemonCBORAgent(DTNCore core) {
        initComponent(core.getConf(), COMPONENT_ENABLE_CBOR_DAEMON_API);
        final HashMap<RequestMessage.RequestCode, Action> actionMap = new HashMap<>();
        actionMap.put(RequestMessage.RequestCode.REGISTER, new RegisterAction());
        actionMap.put(RequestMessage.RequestCode.UNREGISTER, new UnregisterAction());
        actionMap.put(RequestMessage.RequestCode.GET, new GETAction());
        actionMap.put(RequestMessage.RequestCode.POST, new POSTAction());
        ACTIONS = Collections.unmodifiableMap(actionMap);
    }

    @Override
    public String getComponentName() {
        return TAG;
    }

    @Override
    protected void componentUp() {
        //int signalPort = (Integer) DTNConfiguration.get(DTNConfiguration.Entry.API_DAEMON_SIGNAL_PORT).value();
        int serverPort = (Integer) core.getConf().get(DTNConfiguration.Entry.API_CBOR_DAEMON_CHANNEL_PORT).value();

        server = new RxTCP.Server<>(serverPort, RequestChannel::new);
        server.start().subscribe(
                c -> { /* ignore */
                },
                e -> { /* ignore */
                },
                () -> { /* ignore */
                });
    }

    @Override
    protected void componentDown() {
        if (server != null) {
            server.stop();
        }
    }

    class RequestChannel extends RxTCP.Connection {
        RequestChannel() {
            /* prepare parser */
            CborParser parser = CBOR.parser() /* header */
                    .cbor_parse_int((__, ___, i) -> { /* version */ })
                    .cbor_parse_int((p, ___, i) -> {  /* request code */
                        RequestMessage.RequestCode code = RequestMessage.RequestCode.fromId((int) i);
                        if(code == null) {
                            throw new RxParserException("wrong request code");
                        }
                        final RequestMessage message = new RequestMessage(code);
                        p.setReg(0, message);
                    })
                    .cbor_parse_linear_map( /* fields */
                            CBOR.TextStringItem::new,
                            CBOR.TextStringItem::new,
                            (p, ___, map) -> {
                                RequestMessage req = p.getReg(0);
                                for (CBOR.TextStringItem str : map.keySet()) {
                                    req.fields.put(str.value(), map.get(str).value());
                                }
                            })
                    .cbor_parse_byte_string( /* body */
                            (p, ___, size) -> {
                                RequestMessage req =  p.getReg(0);
                                try {
                                    if (size >= 0) {
                                        req.body = core.getStorage().getBlobFactory().createBLOB((int) size);
                                    } else {
                                        // indefinite length CoreBLOBFactory
                                        req.body = core.getStorage().getBlobFactory().createBLOB(2048); //todo change that
                                    }
                                } catch (CoreBLOBFactory.BLOBFactoryException sfe) {
                                    req.body = new NullBLOB();
                                }
                                p.setReg(1, ((BLOB)req.body).getWritableBLOB());
                            },
                            (p, chunk) -> {
                                WritableBLOB blob =  p.getReg(1);
                                try {
                                    blob.write(chunk);
                                } catch (WritableBLOB.BLOBOverflowException | IOException io) {
                                    blob.close();
                                    p.setReg(1, null);
                                    throw new RxParserException("can't save the body blob");
                                }
                            },
                            (p) -> p.<WritableBLOB>getReg(1).close());

            /* receive request */
            recv().subscribe(
                    byteBuffer -> {
                        while (byteBuffer.hasRemaining()) {
                            if (parser.isDone()) {
                                RequestMessage req =  parser.getReg(0);
                                if(req != null) {
                                    ACTIONS.get(req.code).process(req, this);
                                }
                            }
                        }
                    },
                    e -> closeNow(),
                    this::closeNow);
        }

        public void sendResponse(ResponseMessage message) {
        }
    }

    interface Action {
        void process(RequestMessage message, RequestChannel channel);
    }

    private class RegisterAction implements Action {
        public void process(RequestMessage message, RequestChannel channel) {
            System.out.println("register");
        }
    }

    private class UnregisterAction implements Action {
        public void process(RequestMessage message, RequestChannel channel) {
            System.out.println("unregister");
        }
    }

    private class GETAction implements Action {
        public void process(RequestMessage message, RequestChannel channel) {
            System.out.println("get");
        }
    }

    private class POSTAction implements Action {
        public void process(RequestMessage message, RequestChannel channel) {
            System.out.println("post");
        }
    }

}
