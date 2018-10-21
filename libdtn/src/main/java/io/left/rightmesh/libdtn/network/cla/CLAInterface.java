package io.left.rightmesh.libdtn.network.cla;

import io.left.rightmesh.libdtncommon.data.eid.CLA;
import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * A CLAInterface is an abstraction of the underlying protocol used as a CLA.
 *
 * @author Lucien Loiseau on 16/10/18.
 */
public interface CLAInterface {

    /**
     * The name for this CLA must be the exact same one that is used in a EIDCLA to identify
     * this Convergence Layer Adapter.
     *
     * @return a String with the name of this CLA.
     */
    String getCLAName();

    /**
     * When a CLA is started it should return an Observable of CLAChannel used to actually send
     * and receive bundles.
     *
     * @return Flowable of Bundle
     */
    Observable<CLAChannel> start();

    /**
     * When a CLA is stopped, it should stop returning any new CLAChannel. It is an implementation
     * specific decision wether or not to close all the underlying CLAChannels that were previously
     * openned.
     */
    void stop();


    /**
     * Try to open a channel to the given EID CLA.
     *
     * @param eid of the peer to open a channel too
     * @return Single of CLAChannel if successful, error otherwise
     */
    Single<CLAChannel> open(CLA eid);

}
