package io.left.rightmesh.libdtn.core.agents.http;

import io.left.rightmesh.libdtn.core.processor.BundleProcessor;
import io.left.rightmesh.libdtn.data.Bundle;
import io.left.rightmesh.libdtn.data.BundleID;
import io.left.rightmesh.libdtn.data.EID;
import io.left.rightmesh.libdtn.data.PayloadBlock;
import io.left.rightmesh.libdtn.storage.blob.ByteBufferBLOB;
import io.left.rightmesh.libdtn.storage.bundle.Storage;
import io.left.rightmesh.libdtn.utils.Log;
import io.left.rightmesh.libdtn.utils.nettyrouter.Router;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import rx.Observable;

import static io.left.rightmesh.libdtn.utils.nettyrouter.Dispatch.using;
import static rx.Observable.just;

/**
 * @author Lucien Loiseau on 15/10/18.
 */
public class ApplicationAgentAPI {

    private static final String TAG = "ApplicationAgentHTTP";

    private static class BadRequestException extends Exception {
        BadRequestException(String msg) {
            super(msg);
        }
    }

    /**
     * Fetch a bundle and deliver it to the client but don't mark the bundle as delivered
     */
    private static Action aaActionGet = (params, req, res) -> {
        System.out.println("coucou");
        String param = params.get("*");
        if(param == null) {
            return res.setStatus(HttpResponseStatus.BAD_REQUEST)
                    .writeStringAndFlushOnEach(just("incorrect BundleID"));
        }
        BundleID bid = BundleID.create(param);
        if (Storage.contains(bid)) {
            Log.i(TAG, "delivering payload: "+bid);
            return Observable.<Bundle>create(s ->
                    Storage.get(bid).subscribe(
                            bundle -> {
                                s.onNext(bundle);
                                s.onCompleted();
                            },
                            s::onError))
                    .flatMap((bundle) -> res.write(bundle.getPayloadBlock().data.netty()));
        } else {
            return res.writeString(just("no such bundle"));
        }
    };

    /**
     * Fetch a bundle and deliver it to the client then mark the bundle as delivered
     * (remove from storage, send report
     */
    private static Action aaActionFetch = (params, req, res) -> {
        String param = params.get("*");
        BundleID bid = BundleID.create(param);
        if (Storage.contains(bid)) {
            Log.i(TAG, "delivering payload: "+bid);
            return Observable.<Bundle>create(s ->
                    Storage.get(bid).subscribe(
                            bundle -> {
                                s.onNext(bundle);
                                s.onCompleted();
                            },
                            s::onError))
                    .flatMap((bundle) -> res.write(
                            bundle.getPayloadBlock().data.netty().doOnCompleted(
                                    () -> BundleProcessor.bundleLocalDeliverySuccessful(bundle))));
        } else {
            return res.writeString(just("no such bundle"));
        }
    };

    /**
     * Create a new bundle and dispatch it immediatly
     */
    private static Action aaActionPost = (params, req, res) -> {
        final String destEID = req.getHeader("BundleDestinationEID");
        final String reportToEID = req.getHeader("BundleReportToEID");
        final String lifetime = req.getHeader("BundleLifetime");

        try {
            final Bundle bundle = createBundleSkeletonFromHTTPHeaders(destEID, reportToEID, lifetime);
            final ByteBufferBLOB blob = new ByteBufferBLOB((int) req.getContentLength());
            return req.getContent()
                    .reduce(blob.getWritableBLOB(), (wblob, buff) -> {
                        try {
                            wblob.write(buff.nioBuffer());
                        } catch (Exception e) {
                            res.setStatus(HttpResponseStatus.BAD_REQUEST);
                        }
                        return wblob;
                    })
                    .flatMap((wblob) -> {
                        wblob.close();
                        bundle.addBlock(new PayloadBlock(blob));
                        final String bundleid = bundle.bid.toString();
                        res.setStatus(HttpResponseStatus.OK);
                        BundleProcessor.bundleDispatching(bundle);
                        return res;
                    });
        } catch (BadRequestException | EID.EIDFormatException | NumberFormatException bre) {
            Log.i(TAG, req.getDecodedPath() + " - bad request: " + bre.getMessage());
            return res.setStatus(HttpResponseStatus.BAD_REQUEST);
        }
    };

    private static Bundle createBundleSkeletonFromHTTPHeaders(String destinationstr,
                                                      String reporttostr,
                                                      String lifetimestr)
            throws BadRequestException, EID.EIDFormatException, NumberFormatException {
        EID destination;
        EID reportTo;
        long lifetime;

        if (destinationstr == null) {
            throw new BadRequestException("DestinationEID is null");
        }

        destination = EID.create(destinationstr);
        if (reporttostr == null) {
            reportTo = EID.NullEID();
        } else {
            reportTo = EID.create(reporttostr);
        }

        if (lifetimestr == null) {
            lifetime = 0;
        } else {
            lifetime = Long.valueOf(lifetimestr);
        }

        Bundle bundle = new Bundle(destination, lifetime);
        bundle.reportto = reportTo;
        return bundle;
    }

    static Action aaAction = (params, req, res) -> using(new Router<ByteBuf, ByteBuf>()
            .GET("/aa/bundle/:*", aaActionGet)
            .DELETE("/aa/bundle/:*", aaActionFetch)
            .POST("/aa/bundle/", aaActionPost))
            .handle(req, res);

}
