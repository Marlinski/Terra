package io.disruptedsystems.libdtn.core;

import io.disruptedsystems.libdtn.core.api.BundleProtocolApi;
import io.disruptedsystems.libdtn.core.api.ClaManagerApi;
import io.disruptedsystems.libdtn.core.api.ConfigurationApi;
import io.disruptedsystems.libdtn.core.api.CoreApi;
import io.disruptedsystems.libdtn.core.api.DeliveryApi;
import io.disruptedsystems.libdtn.core.api.DirectRoutingStrategyApi;
import io.disruptedsystems.libdtn.core.api.EventListenerApi;
import io.disruptedsystems.libdtn.core.api.ExtensionManagerApi;
import io.disruptedsystems.libdtn.core.api.LinkLocalTableApi;
import io.disruptedsystems.libdtn.core.api.LocalEidApi;
import io.disruptedsystems.libdtn.core.api.ModuleLoaderApi;
import io.disruptedsystems.libdtn.core.api.RegistrarApi;
import io.disruptedsystems.libdtn.core.api.RoutingEngineApi;
import io.disruptedsystems.libdtn.core.api.RoutingTableApi;
import io.disruptedsystems.libdtn.core.api.StorageApi;
import io.disruptedsystems.libdtn.core.events.BundleIndexed;
import io.disruptedsystems.libdtn.core.extension.ExtensionManager;
import io.disruptedsystems.libdtn.core.network.ClaManager;
import io.disruptedsystems.libdtn.core.processor.BundleProtocol;
import io.disruptedsystems.libdtn.core.routing.LinkLocalTable;
import io.disruptedsystems.libdtn.core.routing.LocalEidTable;
import io.disruptedsystems.libdtn.core.routing.RoutingEngine;
import io.disruptedsystems.libdtn.core.routing.RoutingTable;
import io.disruptedsystems.libdtn.core.routing.strategies.direct.DirectRoutingStrategy;
import io.disruptedsystems.libdtn.core.services.NullAa;
import io.disruptedsystems.libdtn.core.spi.ApplicationAgentSpi;
import io.disruptedsystems.libdtn.common.utils.Log;
import io.disruptedsystems.libdtn.core.aa.Registrar;
import io.disruptedsystems.libdtn.core.storage.simple.SimpleStorage;
import io.disruptedsystems.libdtn.core.utils.Logger;
import io.marlinski.librxbus.RxBus;
import io.marlinski.librxbus.RxThread;
import io.marlinski.librxbus.Subscribe;

import static io.disruptedsystems.libdtn.core.api.ConfigurationApi.CoreEntry.COMPONENT_ENABLE_AA_REGISTRATION;
import static io.disruptedsystems.libdtn.core.api.ConfigurationApi.CoreEntry.COMPONENT_ENABLE_LINKLOCAL_ROUTING;
import static io.disruptedsystems.libdtn.core.api.ConfigurationApi.CoreEntry.COMPONENT_ENABLE_MODULE_LOADER;
import static io.disruptedsystems.libdtn.core.api.ConfigurationApi.CoreEntry.COMPONENT_ENABLE_ROUTING;
import static io.disruptedsystems.libdtn.core.api.ConfigurationApi.CoreEntry.COMPONENT_ENABLE_STORAGE;

/**
 * DtnCore registers all the DtnEid Core CoreComponent.
 *
 * @author Lucien Loiseau on 24/08/18.
 */
public class DtnCore implements CoreApi {

    public static final String TAG = "DtnCore";

    private ConfigurationApi conf;
    private Log logger;
    private LocalEidApi localEidTable;
    private ExtensionManagerApi extensionManager;
    private LinkLocalTableApi linkLocalRouting;
    private RoutingTableApi routingTable;
    private RoutingEngineApi routingEngine;
    private Registrar registrar;
    private StorageApi storage;
    private BundleProtocolApi bundleProcessor;
    private ClaManagerApi claManager;
    private ModuleLoaderApi moduleLoader;

    /**
     * Constructor.
     *
     * @param conf core configuration
     */
    public DtnCore(CoreConfiguration conf) {
        this.conf = conf;

        /* core */
        this.logger = new Logger(conf);
        this.localEidTable = new LocalEidTable(this);

        /* BP block toolbox */
        this.extensionManager = new ExtensionManager(logger);

        /* storage */
        this.storage = SimpleStorage.create(this);

        /* routing */
        this.linkLocalRouting = new LinkLocalTable(this);
        this.routingTable = new RoutingTable(this);
        this.routingEngine = new RoutingEngine(this);
        this.registrar = new Registrar(this);

        /* bundle processor */
        this.bundleProcessor = new BundleProtocol(this);

        /* network cla */
        this.claManager = new ClaManager(this);

        /* runtime modules */
        this.moduleLoader = new ModuleLoader(this);
    }

    @Override
    public void init() {
        RxBus.register(this);
        linkLocalRouting.initComponent(getConf().get(COMPONENT_ENABLE_LINKLOCAL_ROUTING), getLogger());
        routingTable.initComponent(getConf().get(COMPONENT_ENABLE_ROUTING), getLogger());
        registrar.initComponent(getConf().get(COMPONENT_ENABLE_AA_REGISTRATION), getLogger());
        storage.initComponent(getConf().get(COMPONENT_ENABLE_STORAGE), getLogger());
        moduleLoader.initComponent(getConf().get(COMPONENT_ENABLE_MODULE_LOADER), getLogger());

        /* starts DtnEid core services (AA) */
        ApplicationAgentSpi nullAa = new NullAa();
        nullAa.init(this.registrar, this.logger);
    }

    @Override
    public ConfigurationApi getConf() {
        return conf;
    }

    @Override
    public Log getLogger() {
        return logger;
    }

    @Override
    public LocalEidApi getLocalEidTable() {
        return localEidTable;
    }

    @Override
    public ExtensionManagerApi getExtensionManager() {
        return extensionManager;
    }

    @Override
    public ModuleLoaderApi getModuleLoader() {
        return moduleLoader;
    }

    @Override
    public RegistrarApi getRegistrar() {
        return registrar;
    }

    @Override
    public DeliveryApi getDelivery() {
        return registrar;
    }

    @Override
    public BundleProtocolApi getBundleProtocol() {
        return bundleProcessor;
    }

    @Override
    public StorageApi getStorage() {
        return storage;
    }

    @Override
    public ClaManagerApi getClaManager() {
        return claManager;
    }

    @Override
    public RoutingEngineApi getRoutingEngine() {
        return routingEngine;
    }

    @Override
    public LinkLocalTableApi getLinkLocalTable() {
        return linkLocalRouting;
    }

    @Override
    public RoutingTableApi getRoutingTable() {
        return routingTable;
    }

    @Subscribe(thread = RxThread.IO)
    public void onEvent(BundleIndexed event) {
        bundleProcessor.bundleDispatching(event.bundle);
    }
}
