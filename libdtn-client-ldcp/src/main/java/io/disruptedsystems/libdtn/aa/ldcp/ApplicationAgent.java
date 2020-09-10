package io.disruptedsystems.libdtn.aa.ldcp;

import io.disruptedsystems.ldcp.LdcpRequest;
import io.disruptedsystems.ldcp.LdcpServer;
import io.disruptedsystems.ldcp.Router;
import io.disruptedsystems.ldcp.messages.ResponseMessage;
import io.disruptedsystems.libdtn.aa.api.ActiveRegistrationCallback;
import io.disruptedsystems.libdtn.aa.api.ApplicationAgentApi;
import io.disruptedsystems.libdtn.common.BaseExtensionToolbox;
import io.disruptedsystems.libdtn.common.ExtensionToolbox;
import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.blob.BaseBlobFactory;
import io.disruptedsystems.libdtn.common.data.blob.BlobFactory;
import io.disruptedsystems.libdtn.common.utils.Log;
import io.disruptedsystems.libdtn.common.utils.NullLogger;
import io.reactivex.rxjava3.core.Single;

import java.net.URI;
import java.util.Set;

/**
 * ApplicationAgent implements ApplicationAgentApi and uses LDCP request over TCP.
 *
 * @author Lucien Loiseau on 25/10/18.
 */
public class ApplicationAgent implements ApplicationAgentApi {

    private static final String TAG = "ldcp-api";

    private String host;
    private int port;
    private LdcpServer server;
    private BlobFactory factory;
    private ExtensionToolbox toolbox;
    private Log logger;

    /**
     * Constructor.
     *
     * @param host host of the LDCP server running on the registrar
     * @param port port of the LDCP server running on the registrar
     * @return a new ApplicationAgent
     */
    public static ApplicationAgentApi create(String host,
                                             int port) {
        return new ApplicationAgent(
                host,
                port,
                new BaseExtensionToolbox(),
                new BaseBlobFactory().setVolatileMaxSize(1000000),
                new NullLogger());
    }

    /**
     * Constructor.
     *
     * @param host    host of the LDCP server running on the registrar
     * @param port    port of the LDCP server running on the registrar
     * @param factory Blob factory
     * @return a new ApplicationAgent
     */
    public static ApplicationAgentApi create(String host,
                                             int port,
                                             BlobFactory factory) {
        return new ApplicationAgent(
                host,
                port,
                new BaseExtensionToolbox(),
                factory,
                new NullLogger());
    }

    /**
     * Constructor.
     *
     * @param host    host of the LDCP server running on the registrar
     * @param port    port of the LDCP server running on the registrar
     * @param toolbox Blocks and Eids factory
     * @param factory Blob factory
     * @return a new ApplicationAgent
     */
    public static ApplicationAgentApi create(String host,
                                             int port,
                                             ExtensionToolbox toolbox,
                                             BlobFactory factory) {
        return new ApplicationAgent(
                host,
                port,
                toolbox,
                factory,
                new NullLogger());
    }

    /**
     * Constructor.
     *
     * @param host    host of the LDCP server running on the registrar
     * @param port    port of the LDCP server running on the registrar
     * @param toolbox Blocks and Eids factory
     * @param factory Blob factory
     * @param logger  logging service
     * @return a new ApplicationAgent
     */
    public static ApplicationAgentApi create(String host,
                                             int port,
                                             ExtensionToolbox toolbox,
                                             BlobFactory factory,
                                             Log logger) {
        return new ApplicationAgent(
                host,
                port,
                toolbox,
                factory,
                logger);
    }

    private ApplicationAgent(String host,
                             int port,
                             ExtensionToolbox toolbox,
                             BlobFactory factory,
                             Log logger) {
        this.host = host;
        this.port = port;
        this.toolbox = toolbox;
        this.factory = factory;
        this.logger = logger;
    }

    private boolean startServer(ActiveRegistrationCallback cb) {
        if (server != null) {
            return false;
        }

        server = new LdcpServer();
        server.start(0, toolbox, factory, logger,
                Router.create()
                        .POST(ApiPaths.DaemonToClientLdcpPathVersion1.DELIVER.path,
                                (req, res) -> cb.recv(req.bundle)
                                        .doOnComplete(() ->
                                                res.setCode(ResponseMessage.ResponseCode.OK))
                                        .doOnError(e ->
                                                res.setCode(ResponseMessage.ResponseCode.ERROR))));
        return true;
    }

    private boolean stopServer() {
        if (server == null) {
            return false;
        }
        server.stop();
        return true;
    }

    @Override
    public Single<Boolean> isRegistered(URI eid) {
        return LdcpRequest.GET(ApiPaths.ClientToDaemonLdcpPathVersion1.ISREGISTERED.path)
                .setHeader("eid", eid.toString())
                .send(host, port, toolbox, factory, logger)
                .map(res -> res.code == ResponseMessage.ResponseCode.OK);
    }

    @Override
    public Single<String> register(URI sink) {
        return register(sink, null);
    }

    @Override
    public Single<String> register(URI eid, ActiveRegistrationCallback cb) {
        if (startServer(cb)) {
            return LdcpRequest.POST(ApiPaths.ClientToDaemonLdcpPathVersion1.REGISTER.path)
                    .setHeader("eid", eid.toString())
                    .setHeader("active", cb == null ? "false" : "true")
                    .setHeader("active-host", "127.0.0.1")
                    .setHeader("active-port", "" + server.getPort())
                    .send(host, port, toolbox, factory, logger)
                    .flatMap(res -> {
                        if (res.code == ResponseMessage.ResponseCode.ERROR) {
                            return Single.error(new ApplicationAgentException(res.body));
                        }
                        if (res.fields.get("cookie") == null) {
                            return Single.error(new ApplicationAgentException("no cookie received"));
                        }
                        return Single.just(res.fields.get("cookie"));
                    });
        } else {
            return Single.error(new RegistrationAlreadyActive());
        }
    }

    @Override
    public Single<Boolean> unregister(URI eid, String cookie) {
        return LdcpRequest.POST(ApiPaths.ClientToDaemonLdcpPathVersion1.UNREGISTER.path)
                .setHeader("eid", eid.toString())
                .setHeader("cookie", cookie)
                .send(host, port, toolbox, factory, logger)
                .map(res -> res.code == ResponseMessage.ResponseCode.OK);
    }

    @Override
    public Set<String> checkInbox(URI sink, String cookie) {
        return null;
    }

    @Override
    public Single<Bundle> get(URI eid, String cookie, String bundleId) {
        return LdcpRequest.GET(ApiPaths.ClientToDaemonLdcpPathVersion1.GETBUNDLE.path)
                .setHeader("eid", eid.toString())
                .setHeader("cookie", cookie)
                .setHeader("bundle-id", bundleId)
                .send(host, port, toolbox, factory, logger)
                .flatMap(res -> {
                    if (res.code == ResponseMessage.ResponseCode.ERROR) {
                        return Single.error(new ApplicationAgentException());
                    }
                    if (res.bundle == null) {
                        return Single.error(new ApplicationAgentException());
                    }
                    return Single.just(res.bundle);
                });
    }

    @Override
    public Single<Bundle> fetch(URI eid, String cookie, String bundleId) {
        return LdcpRequest.GET(ApiPaths.ClientToDaemonLdcpPathVersion1.FETCHBUNDLE.path)
                .setHeader("eid", eid.toString())
                .setHeader("cookie", cookie)
                .setHeader("bundle-id", bundleId)
                .send(host, port, toolbox, factory, logger)
                .flatMap(res -> {
                    if (res.code == ResponseMessage.ResponseCode.ERROR) {
                        return Single.error(new ApplicationAgentException());
                    }
                    if (res.bundle == null) {
                        return Single.error(new ApplicationAgentException());
                    }
                    return Single.just(res.bundle);
                });
    }

    @Override
    public Single<Boolean> send(URI eid, String cookie, Bundle bundle) {
        return LdcpRequest.POST(ApiPaths.ClientToDaemonLdcpPathVersion1.DISPATCH.path)
                .setHeader("eid", eid.toString())
                .setHeader("cookie", cookie)
                .setBundle(bundle)
                .send(host, port, toolbox, factory, logger)
                .map(res -> res.code == ResponseMessage.ResponseCode.OK);
    }

    @Override
    public Single<Boolean> send(Bundle bundle) {
        return LdcpRequest.POST(ApiPaths.ClientToDaemonLdcpPathVersion1.DISPATCH.path)
                .setBundle(bundle)
                .send(host, port, toolbox, factory, logger)
                .map(res -> res.code == ResponseMessage.ResponseCode.OK);
    }

    @Override
    public Single<Boolean> reAttach(URI eid, String cookie, ActiveRegistrationCallback cb) {
        if (startServer(cb)) {
            return LdcpRequest.POST(ApiPaths.ClientToDaemonLdcpPathVersion1.UPDATE.path)
                    .setHeader("eid", eid.toString())
                    .setHeader("cookie", cookie)
                    .setHeader("active", "true")
                    .setHeader("active-host", "127.0.0.1")
                    .setHeader("active-port", "" + server.getPort())
                    .send(host, port, toolbox, factory, logger)
                    .flatMap(res -> {
                        if (res.code == ResponseMessage.ResponseCode.ERROR) {
                            return Single.error(new ApplicationAgentException(res.body));
                        }
                        return Single.just(true);
                    });
        } else {
            return Single.error(new RegistrationAlreadyActive());
        }
    }

    @Override
    public Single<Boolean> setPassive(URI eid, String cookie) {
        stopServer();
        return LdcpRequest.POST(ApiPaths.ClientToDaemonLdcpPathVersion1.UPDATE.path)
                .setHeader("active", "false")
                .setHeader("eid", eid.toString())
                .setHeader("cookie", cookie)
                .send(host, port, toolbox, factory, logger)
                .flatMap(res -> {
                    if (res.code == ResponseMessage.ResponseCode.ERROR) {
                        return Single.error(new ApplicationAgentException(res.body));
                    }
                    return Single.just(true);
                });
    }
}
