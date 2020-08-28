package io.disruptedsystems.libdtn.core.api;

import io.disruptedsystems.libdtn.core.spi.ApplicationAgentAdapterSpi;
import io.disruptedsystems.libdtn.core.spi.ConvergenceLayerSpi;
import io.disruptedsystems.libdtn.core.spi.CoreModuleSpi;

/**
 * API to load modules dynamically into the system.
 *
 * @author Lucien Loiseau on 19/11/18.
 */
public interface ModuleLoaderApi extends CoreComponentApi {

    /**
     * Load an Application Agent Adapter Module.
     * @param aa module to load
     */
    void loadAaModule(ApplicationAgentAdapterSpi aa);

    /**
     * Load a Convergence Layer Adapter Module.
     * @param cla module to load
     */
    void loadClaModule(ConvergenceLayerSpi cla);

    /**
     * Load a Core Module.
     * @param cm module to load
     */
    void loadCoreModule(CoreModuleSpi cm);

}
