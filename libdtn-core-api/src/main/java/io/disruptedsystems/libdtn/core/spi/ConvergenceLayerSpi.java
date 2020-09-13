package io.disruptedsystems.libdtn.core.spi;

import java.net.URI;
import java.util.Set;

import io.disruptedsystems.libdtn.common.utils.Log;
import io.disruptedsystems.libdtn.core.api.ConfigurationApi;
import io.disruptedsystems.libdtn.core.api.LocalEidApi;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

/**
 * A ConvergenceLayerSpi is an abstraction of the underlying protocol used as a CLA.
 *
 * @author Lucien Loiseau on 16/10/18.
 */
public interface ConvergenceLayerSpi extends ModuleSpi {

    /**
     * query the convergence layer to check if a given URI is to be treated as local.
     *
     * @param uri
     * @return true if uri is local, false otherwise.
     */
    boolean isEidLocalCla(URI uri);

    /**
     * When a BaseURIis started it should return an Observable of ClaChannelSpi used to
     * actually send and receive bundles.
     *
     * @param api    configuration
     * @param logger logger instance
     */
    Observable<ClaChannelSpi> start(ConfigurationApi api, Log logger);

    /**
     * Return true if the convergence layer is currently running, false otherwise.
     */
    boolean isStarted();

    /**
     * When a BaseURIis stopped, it should stop creating any new ClaChannelSpi and terminate the
     * observable. It is an implementation specific decision wether or not to close all the
     * underlying CLAChannels that were previously openned.
     */
    void stop();

    /**
     * Tries to open a channel to the given Cla-specific Eid.
     *
     * @param eid of the peer to open a channel too, must be Cla-specific
     * @return Single of ClaChannelSpi if successful, error otherwise
     */
    Single<ClaChannelSpi> open(URI eid);

}
