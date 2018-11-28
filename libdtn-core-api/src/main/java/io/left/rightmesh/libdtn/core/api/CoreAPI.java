package io.left.rightmesh.libdtn.core.api;

import io.left.rightmesh.libdtn.common.utils.Log;

/**
 * @author Lucien Loiseau on 24/10/18.
 */
public interface CoreAPI {

    ConfigurationAPI getConf();

    Log getLogger();

    LocalEIDAPI getLocalEID();

    ExtensionManagerAPI getExtensionManager();

    RoutingAPI getRoutingEngine();

    RegistrarAPI getRegistrar();

    DeliveryAPI getDelivery();

    BundleProcessorAPI getBundleProcessor();

    StorageAPI getStorage();

    CLAManagerAPI getClaManager();

    LinkLocalRoutingAPI getLinkLocalRouting();

    RoutingTableAPI getRoutingTable();

    ModuleLoaderAPI getModuleLoader();

}
