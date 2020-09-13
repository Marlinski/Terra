package io.disruptedsystems.libdtn.core.api;

import java.net.URI;

import io.disruptedsystems.libdtn.core.spi.ClaChannelSpi;
import io.disruptedsystems.libdtn.core.spi.ConvergenceLayerSpi;
import io.reactivex.rxjava3.core.Single;

/**
 * API for the convergence-layer adapters Manager.
 *
 * @author Lucien Loiseau on 27/11/18.
 */
public interface ClaManagerApi {

    /**
     * return true if a eid is a node alias set by an underlying
     * convergence-layer adapter (CLA).
     *
     * @param eid to query
     * @return true if local, false otherwise
     */
    boolean isEidLocalCla(URI eid);

    /**
     * Add a new convergence-layer adapter (CLA).
     *
     * @param cla convergence layer adapter to add
     */
    void addCla(ConvergenceLayerSpi cla);

    /**
     * Try to create an opportunity with a certain ClaEid.
     *
     * @param eid to create an opportunity to
     * @return a single observable to the created {@link ClaChannelSpi}
     */
    Single<ClaChannelSpi> createOpportunity(URI eid);



}
