package io.disruptedsystems.libdtn.module.core.http;

import io.disruptedsystems.libdtn.core.api.CoreApi;
import io.disruptedsystems.libdtn.core.spi.CoreModuleSpi;
import io.disruptedsystems.libdtn.module.core.http.nettyrouter.Dispatch;
import io.disruptedsystems.libdtn.module.core.http.nettyrouter.Router;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import jauter.MethodlessRouter;
import rx.Observable;

import static io.disruptedsystems.libdtn.module.core.http.Configuration.API_DAEMON_HTTP_API_PORT;
import static io.disruptedsystems.libdtn.module.core.http.Configuration.API_DAEMON_HTTP_API_PORT_DEFAULT;
import static io.disruptedsystems.libdtn.module.core.http.nettyrouter.Dispatch.using;

/**
 * @author Lucien Loiseau on 13/10/18.
 */
public class ModuleHTTPDaemon implements CoreModuleSpi {

    private static final String TAG = "http";

    private CoreApi core;
    private HttpServer<ByteBuf, ByteBuf> server;
    private Router<ByteBuf, ByteBuf> router;

    @Override
    public String getModuleName() {
        return TAG;
    }

    @Override
    public void init(CoreApi api) {
        this.core = api;
        RequestConfiguration configurationAPI = new RequestConfiguration(core);
        RequestRegistration registrationAPI = new RequestRegistration(core);
        RequestNetwork networkAPI = new RequestNetwork(core);
        RequestStorage storageAPI = new RequestStorage(core);
        RequestBundle applicationAgentAPI = new RequestBundle(core);

        int serverPort = core.getConf().getModuleConf(getModuleName(),
                API_DAEMON_HTTP_API_PORT, API_DAEMON_HTTP_API_PORT_DEFAULT).value();
        api.getLogger().i(TAG, "starting a http server on port "+serverPort);

        router = new Router<ByteBuf, ByteBuf>()
                .GET("/", rootAction)
                .GET("/help", rootAction)
                .ANY("/conf/", configurationAPI.confAction)
                .ANY("/conf/:*", configurationAPI.confAction)
                .ANY("/registration/", registrationAPI.registerAction)
                .ANY("/network/", networkAPI.networkAction)
                .ANY("/network/:*", networkAPI.networkAction)
                .ANY("/cache/", storageAPI.cacheAction)
                .ANY("/cache/:*", storageAPI.cacheAction)
                .ANY("/aa/", applicationAgentAPI.aaAction)
                .ANY("/aa/:*", applicationAgentAPI.aaAction)
                .notFound(handler404);

        server = HttpServer.newServer(serverPort)
                .start(using(router));
    }

    protected void close() {
        if (server != null) {
            server.shutdown();
        }
    }

    public String dump() {
        StringBuilder sb = new StringBuilder();
        for (String path : router.getPaths()) {
            sb.append(path).append("\n");
        }
        return sb.toString();
    }

    private Action rootAction = (params, req, res) -> {
        String[] header = {
                "         *                                                   .             \n",
                "      +. | .+         .                  .              .         .        \n",
                "  .*.... O   .. *        .                     .       .             .     \n",
                "   ...+.. `'O ..                                    .           |          \n",
                "   +.....+  | ..+                    .                  .      -*-         \n",
                "   ...+...  O ..      ---========================---       .    |          \n",
                "   *...  O'` ...*             .          .                   .             \n",
                " .    O'`  .+.    _________ ____   _____    _____ .  ___                   \n",
                "       `'O       /___  ___// __/  / __  |  / __  |  /   |                  \n",
                "   .                / /.  / /_   / /_/ /  / /_/ /  / /| | .                \n",
                "     .             / /   / __/  / _  |   / _  |   / __  |          .       \n",
                "              .   /./   / /__  / / | |  / / | |  / /  | |                  \n",
                "   |             /_/   /____/ /_/  |_| /_/  |_| /_/   |_|      .           \n",
                "  -*-                                                                *     \n",
                "   |     .           ---========================---             .          \n",
                "      .                 Terrestrial DtnEid - v1.0     .                    .  \n",
                "          .    .             *                    .             .          \n",
                "                                 .                         .               \n",
                "____ /\\__________/\\____ ______________/\\/\\___/\\__________________________|@\n",
                "                __                                               ---       \n",
                "         --           -            --  -      -         ---  __            \n",
                "   --  __                      ___--     RightMesh (c) 2018        --  __  \n\n",
                "REST ApiEid Available: \n",
                "------------------- \n\n",
                dump(),
                ""};
        return res.setStatus(HttpResponseStatus.OK)
                .writeString(Observable.from(header));
    };

    private Action handler404 = (params, req, res) ->
            res.setStatus(HttpResponseStatus.NOT_FOUND);
}
