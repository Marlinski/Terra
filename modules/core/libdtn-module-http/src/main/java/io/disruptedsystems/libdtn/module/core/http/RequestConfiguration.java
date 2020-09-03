package io.disruptedsystems.libdtn.module.core.http;

import java.util.Set;

import io.disruptedsystems.libdtn.core.api.CoreApi;
import io.disruptedsystems.libdtn.module.core.http.nettyrouter.Dispatch;
import io.disruptedsystems.libdtn.module.core.http.nettyrouter.Router;
import io.disruptedsystems.libdtn.common.data.eid.Eid;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import rx.Observable;

import static rx.Observable.just;

/**
 * @author Lucien Loiseau on 14/10/18.
 */
public class RequestConfiguration {

    private CoreApi core;

    RequestConfiguration(CoreApi core) {
        this.core = core;
    }

    private Action confLocalEID = (params, req, res) -> {
        final String localeid = core.getLocalEid().nodeId().getEidString();
        return res.setStatus(HttpResponseStatus.OK).writeString(just("localeid=" + localeid));
    };

    private Action confAliases = (params, req, res) -> {
        final Set<Eid> aliases = core.getLocalEid().aliases();

        return res.setStatus(HttpResponseStatus.OK).writeString(
                Observable.from(aliases)
                        .flatMap((a) -> just(a.getEidString() + "\n")));
    };

    private Action dumpConfiguration = (params, req, res) ->
            res.setStatus(HttpResponseStatus.OK).writeString(just("conf"));

    Router<ByteBuf, ByteBuf> router = new Router<ByteBuf, ByteBuf>()
            .GET("/conf/", dumpConfiguration)
            .GET("/conf/localeid/", confLocalEID)
            .GET("/conf/aliases/", confAliases);

    Action confAction = (params, req, res) -> Dispatch.using(router).handle(req, res);
}
