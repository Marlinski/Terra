package io.disruptedsystems.libdtn.core.spi;

import io.disruptedsystems.libdtn.core.api.CoreApi;

/**
 * Contract to be fulfilled by a core module. A core module has unrestricted access to all
 * components of the core.
 *
 * @author Lucien Loiseau on 25/10/18.
 */
public interface CoreModuleSpi extends ModuleSpi {

    /**
     * Initialize this module.
     *
     * @param api core ApiEid
     */
    void init(CoreApi api);

}
