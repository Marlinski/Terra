package io.disruptedsystems.libdtn.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.disruptedsystems.libdtn.core.api.ConfigurationApi;
import io.disruptedsystems.libdtn.core.api.CoreApi;
import io.disruptedsystems.libdtn.core.api.ModuleLoaderApi;
import io.disruptedsystems.libdtn.core.spi.ApplicationAgentAdapterSpi;
import io.disruptedsystems.libdtn.core.spi.ConvergenceLayerSpi;
import io.disruptedsystems.libdtn.core.spi.CoreModuleSpi;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ServiceLoader;

/**
 * ModuleLoader implements the ModuleLoaderApi. It provides static method to load module as well
 * as the ability to load module from a directory given as part of the configuration.
 *
 * @author Lucien Loiseau on 25/10/18.
 */
public class ModuleLoader extends CoreComponent implements ModuleLoaderApi {

    private static final Logger log = LoggerFactory.getLogger(ModuleLoader.class);

    private final CoreApi core;

    /**
     * Constructor.
     *
     * @param core reference to the core
     */
    public ModuleLoader(CoreApi core) {
        this.core = core;
    }

    @Override
    public String getComponentName() {
        return "ModuleLoader";
    }

    @Override
    protected void componentUp() {
        loadAaModulesFromDirectory();
        loadClaModulesFromDirectory();
        loadCoreModulesFromDirectory();
    }

    @Override
    protected void componentDown() {
        // todo unload all modules
    }

    @Override
    public void loadAaModule(ApplicationAgentAdapterSpi aa) {
        if (!isEnabled()) {
            return;
        }

        aa.init(core.getRegistrar(),
                core.getConf(),
                core.getExtensionManager(),
                core.getStorage().getBlobFactory());
        log.info("AA module loaded: " + aa.getModuleName() + " - UP");
    }

    @Override
    public void loadClaModule(ConvergenceLayerSpi cla) {
        if (!isEnabled()) {
            return;
        }

        core.getClaManager().addCla(cla);
        log.info("CLA module loaded: " + cla.getModuleName() + " - UP");
    }

    @Override
    public void loadCoreModule(CoreModuleSpi cm) {
        if (!isEnabled()) {
            return;
        }

        cm.init(core);
        log.info("CORE module loaded: " + cm.getModuleName() + " - UP");
    }

    private void loadAaModulesFromDirectory() {
        if (core.getConf().<Boolean>get(ConfigurationApi.CoreEntry.ENABLE_AA_MODULES).value()) {
            String path = core.getConf().<String>get(ConfigurationApi.CoreEntry.MODULES_AA_PATH).value();
            try {
                URLClassLoader ucl = new URLClassLoader(pathToListOfJarUrl(path));
                ServiceLoader<ApplicationAgentAdapterSpi> sl
                        = ServiceLoader.load(ApplicationAgentAdapterSpi.class, ucl);
                for (ApplicationAgentAdapterSpi aa : sl) {
                    if (core.getConf().<Boolean>getModuleEnabled(aa.getModuleName(), false)
                            .value()) {
                        loadAaModule(aa);
                    } else {
                        log.info("AA module loaded: " + aa.getModuleName()
                                + " - DOWN");
                    }
                }
            } catch (Exception e) {
                log.warn("error loading AA module: " + e.getMessage());
            }
        }
    }

    private void loadClaModulesFromDirectory() {
        if (core.getConf().<Boolean>get(ConfigurationApi.CoreEntry.ENABLE_CLA_MODULES).value()) {
            String path = core.getConf().<String>get(ConfigurationApi.CoreEntry.MODULES_CLA_PATH).value();
            try {
                URLClassLoader ucl = new URLClassLoader(pathToListOfJarUrl(path));
                ServiceLoader<ConvergenceLayerSpi> sl
                        = ServiceLoader.load(ConvergenceLayerSpi.class, ucl);
                for (ConvergenceLayerSpi cla : sl) {
                    if (core.getConf().getModuleEnabled(cla.getModuleName(), false)
                            .value()) {
                        loadClaModule(cla);
                    } else {
                        log.info("CLA module loaded: " + cla.getModuleName()
                                + " - DOWN");
                    }
                }
            } catch (Exception e) {
                log.warn("error loading CLA module: " + e.getMessage());
            }
        }
    }

    private void loadCoreModulesFromDirectory() {
        if (core.getConf().<Boolean>get(ConfigurationApi.CoreEntry.ENABLE_CORE_MODULES).value()) {
            String path = core.getConf().<String>get(ConfigurationApi.CoreEntry.MODULES_CORE_PATH).value();
            try {
                URLClassLoader ucl = new URLClassLoader(pathToListOfJarUrl(path));
                ServiceLoader<CoreModuleSpi> sl = ServiceLoader.load(CoreModuleSpi.class, ucl);
                for (CoreModuleSpi cm : sl) {
                    if (core.getConf().getModuleEnabled(cm.getModuleName(), false)
                            .value()) {
                        loadCoreModule(cm);
                    } else {
                        log.info("CORE module loaded: " + cm.getModuleName()
                                + " - DOWN");
                    }
                }
            } catch (Exception e) {
                log.warn("error loading CORE module: " + e.getMessage());
            }
        }
    }

    private URL[] pathToListOfJarUrl(String path) throws Exception {
        File loc = new File(path);
        File[] flist = loc.listFiles(f -> f.getPath().toLowerCase().endsWith(".jar"));
        if (flist == null) {
            throw new Exception();
        }
        URL[] urls = new URL[flist.length];
        for (int i = 0; i < flist.length; i++) {
            urls[i] = flist[i].toURI().toURL();
        }
        return urls;
    }
}
