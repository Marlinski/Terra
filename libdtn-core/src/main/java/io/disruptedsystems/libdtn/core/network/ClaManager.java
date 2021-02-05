package io.disruptedsystems.libdtn.core.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import io.disruptedsystems.libdtn.common.data.eid.Cla;
import io.disruptedsystems.libdtn.core.api.ClaManagerApi;
import io.disruptedsystems.libdtn.core.api.ConfigurationApi;
import io.disruptedsystems.libdtn.core.api.CoreApi;
import io.disruptedsystems.libdtn.core.events.ChannelOpened;
import io.disruptedsystems.libdtn.core.spi.ClaChannelSpi;
import io.disruptedsystems.libdtn.core.spi.ConvergenceLayerSpi;
import io.marlinski.librxbus.RxBus;
import io.reactivex.rxjava3.core.Single;

/**
 * ClaManager implements the ClaManagerApi and provides entry point to add convergence layers.
 *
 * @author Lucien Loiseau on 16/10/18.
 */
public class ClaManager implements ClaManagerApi {

    private static final Logger log = LoggerFactory.getLogger(ClaManager.class);

    private final CoreApi core;
    private final List<ConvergenceLayerSpi> clas;

    public ClaManager(CoreApi core) {
        this.core = core;
        clas = new LinkedList<>();
    }

    @Override
    public boolean isEidLocalCla(URI eid) {
        for(ConvergenceLayerSpi cla : clas) {
            if(cla.isEidLocalCla(eid)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addCla(ConvergenceLayerSpi cla) {
        clas.add(cla);
        cla.start(core.getConf()).subscribe(
                dtnChannel -> {
                    RxBus.post(new ChannelOpened(dtnChannel));
                },
                e -> {
                    log.warn("can't start CLA " + cla.getModuleName()
                            + ": " + e.getMessage());
                    clas.remove(cla);
                },
                () -> {
                    log.warn("CLA " + cla.getModuleName() + " has stopped");
                    clas.remove(cla);
                });
    }

    @Override
    public Single<ClaChannelSpi> createOpportunity(URI eid) {
        if (!core.getConf().<Boolean>get(ConfigurationApi.CoreEntry.ENABLE_AUTO_CONNECT_FOR_BUNDLE)
                .value()) {
            return Single.error(new Throwable("AutoConnect is disabled"));
        }

        if(!Cla.isClaEid(eid)) {
            return Single.error(new Throwable("URI is not a cla-eid"));
        }

        String scheme = Cla.getClaSchemeUnsafe(eid);
        String parameters = Cla.getClaParametersUnsafe(eid);
        final String opp = "cla=" + scheme + " peer=" + parameters;

        log.info("trying to create an opportunity with " + opp);
        for (ConvergenceLayerSpi cla : clas) {
            if (scheme.equals(cla.getModuleName())) {
                return cla.open(eid)
                        .doOnError(e -> log.warn("opportunity creation failed "
                                + opp + ": " + e.getMessage()))
                        .doOnSuccess((c) -> {
                            log.info("opportunity creation success: " + opp);
                            RxBus.post(new ChannelOpened(c));
                        });
            }
        }
        return Single.error(new Throwable("no such CLA"));
    }
}
