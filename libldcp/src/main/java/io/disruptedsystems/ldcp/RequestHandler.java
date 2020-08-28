package io.disruptedsystems.ldcp;

import io.disruptedsystems.ldcp.messages.RequestMessage;
import io.disruptedsystems.ldcp.messages.ResponseMessage;
import io.reactivex.rxjava3.core.Completable;

/**
 * Interface to handle a LDCP Request.
 *
 * @author Lucien Loiseau on 26/10/18.
 */
public interface RequestHandler {

    Completable handle(RequestMessage req, ResponseMessage res);

}
