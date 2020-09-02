package io.disruptedsystems.libdtn.module.aa.ldcp;

import io.disruptedsystems.ldcp.LdcpRequest;
import io.disruptedsystems.ldcp.LdcpServer;
import io.disruptedsystems.ldcp.RequestHandler;
import io.disruptedsystems.ldcp.Router;
import io.disruptedsystems.ldcp.messages.RequestMessage;
import io.disruptedsystems.ldcp.messages.ResponseMessage;
import io.disruptedsystems.libdtn.aa.ldcp.ApiPaths;
import io.disruptedsystems.libdtn.common.ExtensionToolbox;
import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.blob.BlobFactory;
import io.disruptedsystems.libdtn.common.data.blob.NullBlob;
import io.disruptedsystems.libdtn.common.utils.Log;
import io.disruptedsystems.libdtn.core.api.ConfigurationApi;
import io.disruptedsystems.libdtn.core.api.DeliveryApi;
import io.disruptedsystems.libdtn.core.api.RegistrarApi;
import io.disruptedsystems.libdtn.core.spi.ActiveRegistrationCallback;
import io.disruptedsystems.libdtn.core.spi.ApplicationAgentAdapterSpi;
import io.reactivex.rxjava3.core.Completable;

/**
 * AAModuleLdcp is an module that is used to enable register and manage application agents
 * over LDCP.
 *
 * @author Lucien Loiseau on 25/10/18.
 */
public class AaModuleLdcp implements ApplicationAgentAdapterSpi {

    private static final String TAG = "ldcp";

    private static class RequestException extends Exception {
        RequestException(String msg) {
            super("Request: " + msg);
        }
    }

    RegistrarApi registrar;
    Log logger;
    ExtensionToolbox toolbox;

    public AaModuleLdcp() {
    }

    @Override
    public String getModuleName() {
        return TAG;
    }

    @Override
    public void init(RegistrarApi api,
                     ConfigurationApi conf,
                     Log logger,
                     ExtensionToolbox toolbox,
                     BlobFactory factory) {
        this.registrar = api;
        this.logger = logger;
        this.toolbox = toolbox;

        int port = conf.getModuleConf(getModuleName(),
                Configuration.LDCP_TCP_PORT,
                Configuration.LDCP_TCP_PORT_DEFAULT).value();
        logger.i(TAG, "starting a ldcp server on port " + port);
        new LdcpServer().start(port, toolbox, factory, logger,
                Router.create()
                        .GET(ApiPaths.ClientToDaemonLdcpPathVersion1.ISREGISTERED.path,
                                isregistered)
                        .POST(ApiPaths.ClientToDaemonLdcpPathVersion1.REGISTER.path,
                                register)
                        .POST(ApiPaths.ClientToDaemonLdcpPathVersion1.UPDATE.path,
                                update)
                        .POST(ApiPaths.ClientToDaemonLdcpPathVersion1.UNREGISTER.path,
                                unregister)
                        .GET(ApiPaths.ClientToDaemonLdcpPathVersion1.GETBUNDLE.path,
                                get)
                        .GET(ApiPaths.ClientToDaemonLdcpPathVersion1.FETCHBUNDLE.path,
                                fetch)
                        .POST(ApiPaths.ClientToDaemonLdcpPathVersion1.DISPATCH.path,
                                dispatch));
    }

    private String checkField(RequestMessage req, String key) throws RequestException {
        if (!req.fields.containsKey(key)) {
            logger.v(TAG, ". missing field " + key);
            throw new RequestException("missing field");
        }
        logger.v(TAG, ". field " + key + "=" + req.fields.get(key));
        return req.fields.get(key);
    }

    private ActiveRegistrationCallback deliverCallback(String eid, String host, int port) {
        return (bundle) ->
                LdcpRequest.POST(ApiPaths.DaemonToClientLdcpPathVersion1.DELIVER.path)
                        .setBundle(bundle)
                        .send(host, port, toolbox, NullBlob::new, logger)
                        .doOnError(d -> {
                            try {
                                /* connection fail - remote is no longer active */
                                registrar.setPassive(eid);
                            } catch (RegistrarApi.RegistrarException re) {
                                /* ignore */
                            }
                        })
                        .flatMapCompletable(d ->
                                d.code.equals(ResponseMessage.ResponseCode.ERROR)
                                        ? Completable.error(new DeliveryApi.DeliveryRefused())
                                        : Completable.complete());
    }

    private RequestHandler isregistered = (req, res) ->
            Completable.create(s -> {
                try {
                    String eid = checkField(req, "eid");
                    res.setCode(registrar.isRegistered(eid)
                            ? ResponseMessage.ResponseCode.OK
                            : ResponseMessage.ResponseCode.ERROR);
                    s.onComplete();
                } catch (RegistrarApi.RegistrarException | RequestException re) {
                    s.onError(re);
                }
            });

    private RequestHandler register = (req, res) ->
            Completable.create(s -> {
                try {
                    String eid = checkField(req, "eid");
                    boolean active = checkField(req, "active").equals("true");

                    if (active) {
                        String host = checkField(req, "active-host");
                        int port = Integer.valueOf(checkField(req, "active-port"));

                        String cookie = registrar.register(eid, deliverCallback(eid, host, port));
                        res.setCode(ResponseMessage.ResponseCode.OK);
                        res.setHeader("cookie", cookie);
                        s.onComplete();
                    } else {
                        String cookie = registrar.register(eid);
                        res.setCode(ResponseMessage.ResponseCode.OK);
                        res.setHeader("cookie", cookie);
                        s.onComplete();
                    }
                } catch (RegistrarApi.RegistrarException | RequestException re) {
                    s.onError(re);
                }
            });


    private RequestHandler update = (req, res) ->
            Completable.create(s -> {
                try {
                    String eid = checkField(req, "eid");
                    String cookie = checkField(req, "cookie");
                    boolean active = checkField(req, "active").equals("true");

                    if (active) {
                        String host = checkField(req, "active-host");
                        int port = Integer.valueOf(checkField(req, "active-port"));

                        registrar.setActive(eid, cookie, deliverCallback(eid, host, port));
                        res.setCode(ResponseMessage.ResponseCode.OK);
                        s.onComplete();
                    } else {
                        registrar.setPassive(eid, cookie);
                        res.setCode(ResponseMessage.ResponseCode.OK);
                        s.onComplete();
                    }
                } catch (RegistrarApi.RegistrarException | RequestException re) {
                    s.onError(re);
                }
            });

    private RequestHandler unregister = (req, res) ->
            Completable.create(s -> {
                try {
                    String eid = checkField(req, "eid");
                    String cookie = checkField(req, "cookie");

                    res.setCode(registrar.unregister(eid, cookie)
                            ? ResponseMessage.ResponseCode.OK
                            : ResponseMessage.ResponseCode.ERROR);
                    s.onComplete();
                } catch (RegistrarApi.RegistrarException | RequestException re) {
                    s.onError(re);
                }
            });

    private RequestHandler get = (req, res) ->
            Completable.create(s -> {
                try {
                    String eid = checkField(req, "eid");
                    String cookie = checkField(req, "cookie");
                    String bid = checkField(req, "bundle-id");

                    Bundle bundle = registrar.get(eid, cookie, bid);
                    if (bundle != null) {
                        res.setCode(ResponseMessage.ResponseCode.OK);
                        res.setBundle(bundle);
                    } else {
                        res.setCode(ResponseMessage.ResponseCode.ERROR);
                    }
                    s.onComplete();
                } catch (RegistrarApi.RegistrarException | RequestException re) {
                    s.onError(re);
                }
            });

    private RequestHandler fetch = (req, res) ->
            Completable.create(s -> {
                try {
                    String eid = checkField(req, "eid");
                    String cookie = checkField(req, "cookie");
                    String bid = checkField(req, "bundle-id");

                    Bundle bundle = registrar.fetch(eid, cookie, bid);
                    if (bundle != null) {
                        res.setCode(ResponseMessage.ResponseCode.OK);
                        res.setBundle(bundle);
                    } else {
                        res.setCode(ResponseMessage.ResponseCode.ERROR);
                    }
                    s.onComplete();
                } catch (RegistrarApi.RegistrarException | RequestException re) {
                    s.onError(re);
                }
            });

    private RequestHandler dispatch = (req, res) ->
            Completable.create(s -> {
                try {
                    res.setCode(registrar.send(req.bundle)
                            ? ResponseMessage.ResponseCode.OK
                            : ResponseMessage.ResponseCode.ERROR);
                    s.onComplete();
                } catch (RegistrarApi.RegistrarException re) {
                    logger.w(TAG, "registrar exception: " + re.getMessage());
                    s.onError(re);
                }
            });
}
