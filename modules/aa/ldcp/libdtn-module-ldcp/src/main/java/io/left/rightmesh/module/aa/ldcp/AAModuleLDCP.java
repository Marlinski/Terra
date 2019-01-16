package io.left.rightmesh.module.aa.ldcp;

import io.left.rightmesh.libdtn.common.ExtensionToolbox;
import io.left.rightmesh.libdtn.common.data.Bundle;
import io.left.rightmesh.libdtn.common.data.blob.BLOBFactory;
import io.left.rightmesh.libdtn.common.data.blob.NullBLOB;
import io.left.rightmesh.libdtn.common.utils.Log;
import io.left.rightmesh.libdtn.core.api.ConfigurationAPI;
import io.left.rightmesh.libdtn.core.api.DeliveryAPI;
import io.left.rightmesh.libdtn.core.api.RegistrarAPI;
import io.left.rightmesh.libdtn.core.spi.aa.ActiveRegistrationCallback;
import io.left.rightmesh.libdtn.core.spi.aa.ApplicationAgentAdapterSPI;
import io.left.rightmesh.module.aa.ldcp.messages.RequestMessage;
import io.left.rightmesh.module.aa.ldcp.messages.ResponseMessage;
import io.reactivex.Completable;

import static io.left.rightmesh.aa.ldcp.api.APIPaths.DELIVER;
import static io.left.rightmesh.aa.ldcp.api.APIPaths.DISPATCH;
import static io.left.rightmesh.aa.ldcp.api.APIPaths.FETCHBUNDLE;
import static io.left.rightmesh.aa.ldcp.api.APIPaths.GETBUNDLE;
import static io.left.rightmesh.aa.ldcp.api.APIPaths.ISREGISTERED;
import static io.left.rightmesh.aa.ldcp.api.APIPaths.REGISTER;
import static io.left.rightmesh.aa.ldcp.api.APIPaths.UNREGISTER;
import static io.left.rightmesh.aa.ldcp.api.APIPaths.UPDATE;
import static io.left.rightmesh.module.aa.ldcp.Configuration.LDCP_TCP_PORT;
import static io.left.rightmesh.module.aa.ldcp.Configuration.LDCP_TCP_PORT_DEFAULT;

/**
 * @author Lucien Loiseau on 25/10/18.
 */
public class AAModuleLDCP implements ApplicationAgentAdapterSPI {

    private static final String TAG = "ldcp";

    private static class RequestException extends Exception {
        RequestException(String msg) {
            super("Request: " + msg);
        }
    }

    RegistrarAPI registrar;
    Log logger;
    ExtensionToolbox toolbox;

    public AAModuleLDCP() {
    }

    @Override
    public String getModuleName() {
        return TAG;
    }

    @Override
    public void init(RegistrarAPI api, ConfigurationAPI conf, Log logger, ExtensionToolbox toolbox, BLOBFactory factory) {
        int port = conf.getModuleConf(getModuleName(), LDCP_TCP_PORT, LDCP_TCP_PORT_DEFAULT).value();
        this.registrar = api;
        this.logger = logger;
        this.toolbox = toolbox;
        logger.i(TAG, "starting a ldcp server on port " + port);
        new LdcpServer().start(port, toolbox, factory, logger,
                Router.create()
                        .GET(ISREGISTERED, isregistered)
                        .POST(REGISTER, register)
                        .POST(UPDATE, update)
                        .POST(UNREGISTER, unregister)
                        .GET(GETBUNDLE, get)
                        .GET(FETCHBUNDLE, fetch)
                        .POST(DISPATCH, dispatch));
    }

    private String checkField(RequestMessage req, String key) throws RequestException {
        if (!req.fields.containsKey(key)) {
            logger.v(TAG, ". missing field "+key);
            throw new RequestException("missing field");
        }
        logger.v(TAG, ". field "+key+"="+req.fields.get(key));
        return req.fields.get(key);
    }

    private ActiveRegistrationCallback deliverCallback(String sink, String host, int port) {
        return (bundle) ->
                LdcpRequest.POST(DELIVER)
                        .setBundle(bundle)
                        .send(host, port, toolbox, NullBLOB::new, logger)
                        .doOnError(d -> {
                            try {
                                /* connection fail - remote is no longer active */
                                registrar.setPassive(sink);
                            } catch(RegistrarAPI.RegistrarException re) {
                                /* ignore */
                            }
                        })
                        .flatMapCompletable(d ->
                                d.code.equals(ResponseMessage.ResponseCode.ERROR)
                                        ? Completable.error(new DeliveryAPI.DeliveryRefused())
                                        : Completable.complete());
    }

    private RequestHandler isregistered = (req, res) ->
            Completable.create(s -> {
                try {
                    String sink = checkField(req, "sink");
                    res.setCode(registrar.isRegistered(sink) ? ResponseMessage.ResponseCode.OK : ResponseMessage.ResponseCode.ERROR);
                    s.onComplete();
                } catch (RegistrarAPI.RegistrarException | RequestException re) {
                    s.onError(re);
                }
            });

    private RequestHandler register = (req, res) ->
            Completable.create(s -> {
                try {
                    String sink = checkField(req, "sink");
                    boolean active = checkField(req, "active").equals("true");

                    if (active) {
                        String host = checkField(req, "active-host");
                        int port = Integer.valueOf(checkField(req, "active-port"));

                        String cookie = registrar.register(sink, deliverCallback(sink, host, port));
                        res.setCode(ResponseMessage.ResponseCode.OK);
                        res.setHeader("cookie", cookie);
                        s.onComplete();
                    } else {
                        String cookie = registrar.register(sink);
                        res.setCode(ResponseMessage.ResponseCode.OK);
                        res.setHeader("cookie", cookie);
                        s.onComplete();
                    }
                } catch (RegistrarAPI.RegistrarException | RequestException re) {
                    s.onError(re);
                }
            });


    private RequestHandler update = (req, res) ->
            Completable.create(s -> {
                try {
                    String sink = checkField(req, "sink");
                    String cookie = checkField(req, "cookie");
                    boolean active = checkField(req, "active").equals("true");

                    if (active) {
                        String host = checkField(req, "active-host");
                        int port = Integer.valueOf(checkField(req, "active-port"));

                        registrar.setActive(sink, cookie, deliverCallback(sink, host, port));
                        res.setCode(ResponseMessage.ResponseCode.OK);
                        s.onComplete();
                    } else {
                        registrar.setPassive(sink, cookie);
                        res.setCode(ResponseMessage.ResponseCode.OK);
                        s.onComplete();
                    }
                } catch (RegistrarAPI.RegistrarException | RequestException re) {
                    s.onError(re);
                }
            });

    private RequestHandler unregister = (req, res) ->
            Completable.create(s -> {
                try {
                    String sink = checkField(req, "sink");
                    String cookie = checkField(req, "cookie");

                    res.setCode(registrar.unregister(sink, cookie) ? ResponseMessage.ResponseCode.OK : ResponseMessage.ResponseCode.ERROR);
                    s.onComplete();
                } catch (RegistrarAPI.RegistrarException | RequestException re) {
                    s.onError(re);
                }
            });

    private RequestHandler get = (req, res) ->
            Completable.create(s -> {
                try {
                    String sink = checkField(req, "sink");
                    String cookie = checkField(req, "cookie");
                    String bid = checkField(req, "bundle-id");

                    Bundle bundle = registrar.get(sink, cookie, bid);
                    if (bundle != null) {
                        res.setCode(ResponseMessage.ResponseCode.OK);
                        res.setBundle(bundle);
                    } else {
                        res.setCode(ResponseMessage.ResponseCode.ERROR);
                    }
                    s.onComplete();
                } catch (RegistrarAPI.RegistrarException | RequestException re) {
                    s.onError(re);
                }
            });

    private RequestHandler fetch = (req, res) ->
            Completable.create(s -> {
                try {
                    String sink = checkField(req, "sink");
                    String cookie = checkField(req, "cookie");
                    String bid = checkField(req, "bundle-id");

                    Bundle bundle = registrar.fetch(sink, cookie, bid);
                    if (bundle != null) {
                        res.setCode(ResponseMessage.ResponseCode.OK);
                        res.setBundle(bundle);
                    } else {
                        res.setCode(ResponseMessage.ResponseCode.ERROR);
                    }
                    s.onComplete();
                } catch (RegistrarAPI.RegistrarException | RequestException re) {
                    s.onError(re);
                }
            });

    private RequestHandler dispatch = (req, res) ->
            Completable.create(s -> {
                try {
                    res.setCode(registrar.send(req.bundle) ? ResponseMessage.ResponseCode.OK : ResponseMessage.ResponseCode.ERROR);
                    s.onComplete();
                } catch (RegistrarAPI.RegistrarException re) {
                    logger.w(TAG, "registrar exception: " + re.getMessage());
                    s.onError(re);
                }
            });
}
