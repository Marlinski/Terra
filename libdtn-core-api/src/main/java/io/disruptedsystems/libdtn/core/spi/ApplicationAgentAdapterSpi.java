package io.disruptedsystems.libdtn.core.spi;

import io.disruptedsystems.libdtn.common.ExtensionToolbox;
import io.disruptedsystems.libdtn.common.data.blob.BlobFactory;
import io.disruptedsystems.libdtn.common.utils.Log;
import io.disruptedsystems.libdtn.core.api.ConfigurationApi;
import io.disruptedsystems.libdtn.core.api.RegistrarApi;

/**
 * Contract to be fulfilled by an Application Agent Adapter module.
 *
 * @author Lucien Loiseau on 23/10/18.
 */
public interface ApplicationAgentAdapterSpi extends ModuleSpi {

    /**
     * Initialize this module.
     *
     * @param api registrar api
     * @param conf configuration
     * @param logger logger instance
     * @param toolbox block and eids factories
     * @param factory to create new Blob
     */
    void init(RegistrarApi api,
              ConfigurationApi conf,
              Log logger,
              ExtensionToolbox toolbox,
              BlobFactory factory);

}
