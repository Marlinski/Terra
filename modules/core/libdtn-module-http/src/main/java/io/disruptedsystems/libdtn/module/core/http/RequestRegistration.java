package io.disruptedsystems.libdtn.module.core.http;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

import io.disruptedsystems.libdtn.core.api.CoreApi;
import io.disruptedsystems.libdtn.core.api.RegistrarApi;
import io.disruptedsystems.libdtn.module.core.http.nettyrouter.Router;
import io.disruptedsystems.libdtn.module.core.http.nettyrouter.Dispatch;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;

import static rx.Observable.just;

/**
 * @author Lucien Loiseau on 14/10/18.
 */
public class RequestRegistration {

    private CoreApi core;

    RequestRegistration(CoreApi core) {
        this.core = core;
    }

    private Action registerActionGet = (params, req, res) ->
            res.setStatus(HttpResponseStatus.BAD_REQUEST)
                    .writeString(just(core.getRegistrar().printTable()));

    private Action registerActionAdd = (params, req, res) ->
            req.getContent()
                .reduce("", (content, buff) ->
                        content+buff.toString(Charset.defaultCharset()))
                .flatMap((sink) -> {
                    try {
                        if (core.getRegistrar().isRegistered(new URI(sink))) {
                            return res.setStatus(HttpResponseStatus.CONFLICT)
                                    .writeString(just("sink is already registered: " + sink))
                                    .writeString(just(core.getRegistrar().printTable()));
                        } else {
                            String cookie = core.getRegistrar().register(new URI(sink));
                            return res.setStatus(HttpResponseStatus.OK)
                                    .writeString(just("sink registered: " + sink))
                                    .writeString(just("registration cookie: " + cookie))
                                    .writeString(just(core.getRegistrar().printTable()));
                        }
                    } catch(RegistrarApi.RegistrarException | URISyntaxException re) {
                        return res.setStatus(HttpResponseStatus.BAD_REQUEST)
                                .writeString(just("sink is not valid"));
                    }
                });

    private Action registerActionDelete = (params, req, res) ->
        req.getContent()
                .reduce("", (content, buff) ->
                        content+buff.toString(Charset.defaultCharset()))
                .flatMap((sink) -> {
                    try {
                        // todo fix cookie parameter
                        core.getRegistrar().unregister(new URI(sink), sink);
                        return res.setStatus(HttpResponseStatus.OK)
                                .writeString(just("sink unregistered: " + sink))
                                .writeString(just(core.getRegistrar().printTable()));
                    } catch(RegistrarApi.RegistrarException | URISyntaxException re) {
                        return res.setStatus(HttpResponseStatus.BAD_REQUEST)
                                .writeString(just("request not valid"));
                    }
                });

    Action registerAction = (params, req, res) -> Dispatch.using(new Router<ByteBuf, ByteBuf>()
            .GET("/registration/", registerActionGet)
            .POST("/registration/", registerActionAdd)
            .DELETE("/registration/", registerActionDelete))
            .handle(req, res);
}
