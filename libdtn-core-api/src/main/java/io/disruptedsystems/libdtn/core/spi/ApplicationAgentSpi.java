package io.disruptedsystems.libdtn.core.spi;

import io.disruptedsystems.libdtn.common.utils.Log;
import io.disruptedsystems.libdtn.core.api.RegistrarApi;

/**
 * Contract to be fulfilled by an Application Agent module.
 *
 * @author Lucien Loiseau on 11/11/18.
 */
public interface ApplicationAgentSpi {

    void init(RegistrarApi registrar, Log logger);

}
