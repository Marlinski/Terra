package io.disruptedsystems.libdtn.core;

import io.disruptedsystems.libdtn.core.api.BundleProtocolApi;
import io.disruptedsystems.libdtn.core.api.ClaManagerApi;
import io.disruptedsystems.libdtn.core.api.ConfigurationApi;
import io.disruptedsystems.libdtn.core.api.CoreApi;
import io.disruptedsystems.libdtn.core.api.DeliveryApi;
import io.disruptedsystems.libdtn.core.api.ExtensionManagerApi;
import io.disruptedsystems.libdtn.core.api.LinkLocalTableApi;
import io.disruptedsystems.libdtn.core.api.LocalEidApi;
import io.disruptedsystems.libdtn.core.api.ModuleLoaderApi;
import io.disruptedsystems.libdtn.core.api.RegistrarApi;
import io.disruptedsystems.libdtn.core.api.RoutingEngineApi;
import io.disruptedsystems.libdtn.core.api.RoutingTableApi;
import io.disruptedsystems.libdtn.core.api.StorageApi;
import io.disruptedsystems.libdtn.core.events.DtnEvent;
import io.disruptedsystems.libdtn.core.extension.ExtensionManager;
import io.disruptedsystems.libdtn.core.network.ClaManager;
import io.disruptedsystems.libdtn.core.processor.BundleProtocol;
import io.disruptedsystems.libdtn.core.routing.LinkLocalTable;
import io.disruptedsystems.libdtn.core.routing.LocalEidTable;
import io.disruptedsystems.libdtn.core.routing.RoutingEngine;
import io.disruptedsystems.libdtn.core.routing.RoutingTable;
import io.disruptedsystems.libdtn.core.services.NullAa;
import io.disruptedsystems.libdtn.core.spi.ApplicationAgentSpi;
import io.disruptedsystems.libdtn.core.aa.Registrar;
import io.disruptedsystems.libdtn.core.storage.simple.SimpleStorage;
import io.marlinski.librxbus.RxBus;
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
        this.localEidTable = new LocalEidTable(this);

        /* BP block toolbox */
        this.extensionManager = new ExtensionManager();

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
        linkLocalRouting.initComponent(getConf().get(COMPONENT_ENABLE_LINKLOCAL_ROUTING));
        routingTable.initComponent(getConf().get(COMPONENT_ENABLE_ROUTING));
        registrar.initComponent(getConf().get(COMPONENT_ENABLE_AA_REGISTRATION));
        storage.initComponent(getConf().get(COMPONENT_ENABLE_STORAGE));
        moduleLoader.initComponent(getConf().get(COMPONENT_ENABLE_MODULE_LOADER));

        /* starts DtnEid core services (AA) */
        ApplicationAgentSpi nullAa = new NullAa();
        nullAa.init(this.registrar);
    }

    @Override
    public ConfigurationApi getConf() {
        return conf;
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

    @Subscribe
    public void onEvent(DtnEvent event) {
        // do nothing
    }
}
