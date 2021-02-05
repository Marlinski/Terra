package io.disruptedsystems.libdtn.core.spi;

import io.disruptedsystems.libdtn.core.api.ExtensionManagerApi;

/**
 * Contract to be fulfilled by a module that introduces new ExtensionBlock.
 *
 * @author Lucien Loiseau on 03/11/18.
 */
public interface BlockSpi {

    void init(ExtensionManagerApi extensionManager);

}
