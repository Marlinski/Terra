package io.disruptedsystems.libdtn.aa.api;

import io.disruptedsystems.libdtn.common.data.Bundle;
import io.reactivex.rxjava3.core.Completable;

/**
 * ActiveRegistrationCallback describe the signature of the callback that must be passed to
 * a registration to be able to receive Bundle.
 *
 * @author Lucien Loiseau on 26/10/18.
 */
public interface ActiveRegistrationCallback {

    /**
     * recv is called whenever some data are available to the current active registration.
     * If this method returns true, the bundle holding this data will be considered as
     * delivered and may trigger the sending of status report. If this method returns
     * false, the bundle may stay in cache until it is either pulled manually by the
     * application agent or deleted by the core.
     *
     * @param bundle to deliver
     * @return true if delivery is accepted, false otherwise.
     */
    Completable recv(Bundle bundle);

}
