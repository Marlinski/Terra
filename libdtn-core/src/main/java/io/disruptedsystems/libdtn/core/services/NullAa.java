package io.disruptedsystems.libdtn.core.services;

import io.disruptedsystems.libdtn.core.api.RegistrarApi;
import io.disruptedsystems.libdtn.core.spi.ApplicationAgentSpi;
import io.disruptedsystems.libdtn.common.utils.Log;
import io.reactivex.rxjava3.core.Completable;

/**
 * Any Bundle sent to the Null Application Agent will be successfully delivered but deleted
 * immediately.
 *
 * @author Lucien Loiseau on 11/11/18.
 */
public class NullAa implements ApplicationAgentSpi {

    @Override
    public void init(RegistrarApi registrar, Log logger) {
        try {
            registrar.register("/null/", (bundle) -> {
                bundle.clearBundle();
                return Completable.complete();
            });
        } catch (RegistrarApi.RegistrarDisabled
                | RegistrarApi.NullArgument
                | RegistrarApi.SinkAlreadyRegistered e) {
            /* ignore */
        }
    }

}
