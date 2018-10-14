package io.left.rightmesh.libdtn.core.agents.http;

import io.netty.handler.codec.http.HttpResponseStatus;

import static rx.Observable.just;

/**
 * @author Lucien Loiseau on 14/10/18.
 */
public class StorageAPI {

    static Action fetchAction = (params, req, res) -> {
        return res.writeString(just("fetch"));
    };


    static Action cacheAction = (params, req, res) -> {
        return res.setStatus(HttpResponseStatus.OK).writeString(just("cache"));
    };
}