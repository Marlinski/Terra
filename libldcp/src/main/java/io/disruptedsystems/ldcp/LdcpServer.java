package io.disruptedsystems.ldcp;

import io.disruptedsystems.ldcp.messages.RequestMessage;
import io.disruptedsystems.ldcp.messages.ResponseMessage;
import io.disruptedsystems.libdtn.common.ExtensionToolbox;
import io.disruptedsystems.libdtn.common.data.blob.BlobFactory;
import io.disruptedsystems.libdtn.common.utils.Log;
import io.marlinski.libcbor.CborParser;
import io.marlinski.libcbor.rxparser.RxParserException;
import io.marlinski.librxtcp.RxTCP;

/**
 * LdcpServer can serve LdcpRequest from client host.
 *
 * @author Lucien Loiseau on 25/10/18.
 */
public class LdcpServer {

    private static final String TAG = "ldcp-server";

    RxTCP.Server<RxTCP.Connection> server;

    public int getPort() {
        return server.getPort();
    }

    /**
     * Start a TCP server and listen for LdcpRequest.
     *
     * @param port    port to listen to
     * @param toolbox toolbox to parse/serialize bundle
     * @param factory blob factory to receive bundle
     * @param logger  logger
     * @param action  router
     */
    public void start(int port,
                      ExtensionToolbox toolbox,
                      BlobFactory factory,
                      Log logger,
                      RequestHandler action) {
        server = new RxTCP.Server<>(port);
        server.start().subscribe(
                con -> {
                    CborParser parser = RequestMessage.getParser(logger, toolbox, factory);
                    con.recv().subscribe(
                            buf -> {
                                try {
                                    while (buf.hasRemaining() && !parser.isDone()) {
                                        if (parser.read(buf)) {
                                            RequestMessage req = parser.getReg(0);
                                            logger.v(TAG, con.getRemoteHost() + " "
                                                    + req.code.name() + " " + req.path);
                                            ResponseMessage res = new ResponseMessage();
                                            action.handle(req, res).subscribe(
                                                    () -> LdcpResponse
                                                            .wrap(res)
                                                            .send(con),
                                                    e -> LdcpResponse.ERROR()
                                                            .setBody(e.getClass().getSimpleName() + ":" + e.getMessage())
                                                            .send(con)
                                            );
                                        }
                                    }
                                } catch (RxParserException rpe) {
                                    logger.w(TAG, "request parser exception: "
                                            + rpe.getMessage());
                                    con.closeNow();
                                }
                            },
                            e -> con.closeNow(),
                            con::closeNow
                    );

                });
    }

    public void stop() {
        server.stop();
    }

}
