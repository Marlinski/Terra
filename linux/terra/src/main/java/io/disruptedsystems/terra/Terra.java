package io.disruptedsystems.terra;

import io.disruptedsystems.libdtn.common.data.eid.BaseEidFactory;
import io.disruptedsystems.libdtn.common.data.eid.Eid;
import io.disruptedsystems.libdtn.common.data.eid.EidFormatException;
import io.disruptedsystems.libdtn.common.utils.Log;
import io.disruptedsystems.libdtn.core.CoreConfiguration;
import io.disruptedsystems.libdtn.core.DtnCore;
import io.disruptedsystems.libdtn.core.api.ConfigurationApi;
import io.disruptedsystems.libdtn.core.api.CoreApi;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

// CHECKSTYLE IGNORE LineLength
@Command(
        name = "terra", mixinStandardHelpOptions = true, version = "terra 1.0",
        header = {
                "@|green          *                                                   .             |@",
                "@|green       +. | .+         .                  .              .         .        |@",
                "@|green   .*.... O   .. *        .                     .       .             .     |@",
                "@|green    ...+.. `'O ..                                    .           |          |@",
                "@|green    +.....+  | ..+                    .                  .      -*-         |@",
                "@|green    ...+...  O ..      ---========================---       .    |          |@",
                "@|green    *...  O'` ...*             .          .                   .             |@",
                "@|green  .    O'`  .+.    _________ ____   _____    _____ .  ___                   |@",
                "@|green        `'O       /___  ___// __/  / __  |  / __  |  /   |                  |@",
                "@|green    .                / /.  / /_   / /_/ /  / /_/ /  / /| | .                |@",
                "@|green      .             / /   / __/  / _  |   / _  |   / __  |          .       |@",
                "@|green               .   /./   / /__  / / | |  / / | |  / /  | |                  |@",
                "@|green    |             /_/   /____/ /_/  |_| /_/  |_| /_/   |_|      .           |@",
                "@|green   -*-                                                                *     |@",
                "@|green    |     .           ---========================---             .          |@",
                "@|green       .                 Terrestrial DtnEid - v1.0     .                    .  |@",
                "@|green           .    .             *                    .             .          |@",
                "@|green                                  .                         .               |@",
                "@|green ____ /\\__________/\\____ ______________/\\/\\___/\\____________________________|@",
                "@|green                 __                                               ---       |@",
                "@|green          --           -            --  -      -         ---  __            |@",
                "@|green    --  __                      ___--     RightMesh (c) 2018        --  __  |@",
                ""},
        //descriptionHeading = "@|bold %nDescription|@:%n",
        description = {
                "",
                "Terra is a full node DtnEid implementation for Terrestrial DtnEid",},
        optionListHeading = "@|bold %nOptions|@:%n",
        footer = {
                ""})
public class Terra implements Callable<Void> {

    @Option(names = {"-e", "--eid"},
            description = "set local eid")
    private String localEid = null;

    @Option(names = {"--enable-volatile"},
            description = "enable volatile storage")
    private boolean volatileStorage = true;

    @Option(names = {"-l", "--volatile-limit"},
            description = "volatile storage size limit")
    private int volatileLimit = 1000000;

    @Option(names = {"-P", "--enable-persistent"},
            description = "enable volatile storage")
    private boolean persistentStorage = false;

    @Option(names = {"-p", "--persistent-path"},
            description = "persistent storage directory")
    private String simplePath = "./";

    @Option(names = {"-d", "--daemon"},
            description = "Start Terra as a daemon.")
    private boolean daemon;

    @Option(names = {"--module-cla"},
            description = "set the path to the network Convergence Layer Adapters modules.")
    private String claModuleDirectory = null;

    @Option(names = {"--module-aa"},
            description = "set the path to the Application Agent Adapters modules.")
    private String aaModuleDirectory = null;

    @Option(names = {"--module-core"},
            description = "set the path to the Core modules.")
    private String coreModuleDirectory = null;

    @Option(names = {"-v", "--verbose"},
            description = "set the log level to debug (-v -vv -vvv).")
    private boolean[] verbose = new boolean[0];

    @Option(names = {"--disable-reporting"},
            description = "disable sending status reporting.")
    private boolean disableReporting = false;

    @Option(names = {"--disable-forwarding"},
            description = "do not forward bundle that are not local.")
    private boolean disableForwarding = false;

    @Option(names = {"--disable-eid-autoconnect"},
            description = "do not try to create opportunity when dispatching bundles.")
    private boolean disableEidAutoconnect = false;

    @Option(names = {"--disable-peer-autoconnect"},
            description = "do not try to create opportunity with detected peers.")
    private boolean disablePeerAutoconnect = false;

    @Option(names = {"--ldcp-port"},
            description = "do not try to create opportunity with detected peers.")
    private int ldcpPort = 4557;

    @Option(names = {"--stcp-port"},
            description = "do not try to create opportunity with detected peers.")
    private int stcpPort = 4556;

    @Option(names = {"--http-port"},
            description = "do not try to create opportunity with detected peers.")
    private int httpPort = 8080;

    @Option(names = {"--enable-modules"},
            arity = "1..*",
            description = "whitelisted modules")
    private String[] whitelistModules = new String[0];

    @Option(names = {"--disable-modules"},
            arity = "1..*",
            description = "blacklisted modules")
    private String[] blacklistModules = new String[0];


    @Override
    public Void call() throws Exception {
        CoreConfiguration conf = new CoreConfiguration();

        /* Terra configuration */
        if (localEid != null) {
            try {
                Eid eid = new BaseEidFactory().create(localEid);
                conf.get(ConfigurationApi.CoreEntry.LOCAL_EID).update(eid);
            } catch (EidFormatException efe) {
                throw new Exception("localEid is not a valid Endpoint ID: " + efe.getMessage());
            }
        }

        conf.get(ConfigurationApi.CoreEntry.COMPONENT_ENABLE_VOLATILE_STORAGE).update(volatileStorage);
        if (volatileStorage) {
            conf.get(ConfigurationApi.CoreEntry.VOLATILE_BLOB_STORAGE_MAX_CAPACITY).update(volatileLimit);
        }

        conf.get(ConfigurationApi.CoreEntry.COMPONENT_ENABLE_SIMPLE_STORAGE).update(persistentStorage);
        if (persistentStorage) {
            Set<String> paths = new HashSet<>();
            paths.add(simplePath);
            conf.<Set<String>>get(ConfigurationApi.CoreEntry.SIMPLE_STORAGE_PATH).update(paths);
        }

        conf.get(ConfigurationApi.CoreEntry.ENABLE_STATUS_REPORTING).update(!disableReporting);
        conf.get(ConfigurationApi.CoreEntry.ENABLE_FORWARDING).update(!disableForwarding);
        conf.get(ConfigurationApi.CoreEntry.ENABLE_AUTO_CONNECT_FOR_BUNDLE).update(!disableEidAutoconnect);
        conf.get(ConfigurationApi.CoreEntry.ENABLE_AUTO_CONNECT_FOR_DETECT_EVENT).update(!disablePeerAutoconnect);

        if (claModuleDirectory != null) {
            conf.get(ConfigurationApi.CoreEntry.ENABLE_CLA_MODULES).update(true);
            conf.get(ConfigurationApi.CoreEntry.MODULES_CLA_PATH).update(claModuleDirectory);
        } else {
            conf.get(ConfigurationApi.CoreEntry.ENABLE_CLA_MODULES).update(false);
        }
        if (aaModuleDirectory != null) {
            conf.get(ConfigurationApi.CoreEntry.ENABLE_AA_MODULES).update(true);
            conf.get(ConfigurationApi.CoreEntry.MODULES_AA_PATH).update(aaModuleDirectory);
        } else {
            conf.get(ConfigurationApi.CoreEntry.ENABLE_AA_MODULES).update(false);
        }
        if (coreModuleDirectory != null) {
            conf.get(ConfigurationApi.CoreEntry.ENABLE_CORE_MODULES).update(true);
            conf.get(ConfigurationApi.CoreEntry.MODULES_CORE_PATH).update(coreModuleDirectory);
        } else {
            conf.get(ConfigurationApi.CoreEntry.ENABLE_CORE_MODULES).update(false);
        }

        switch (verbose.length) {
            case 0:
                conf.get(ConfigurationApi.CoreEntry.LOG_LEVEL).update(Log.LogLevel.WARN);
                break;
            case 1:
                conf.get(ConfigurationApi.CoreEntry.LOG_LEVEL).update(Log.LogLevel.INFO);
                break;
            case 2:
                conf.get(ConfigurationApi.CoreEntry.LOG_LEVEL).update(Log.LogLevel.DEBUG);
                break;
            default:
                conf.get(ConfigurationApi.CoreEntry.LOG_LEVEL).update(Log.LogLevel.VERBOSE);
        }

        /* module configuration */
        for (String enableModule : whitelistModules) {
            conf.getModuleEnabled(enableModule, true).update(true);
        }
        for (String disableModule : blacklistModules) {
            conf.getModuleEnabled(disableModule, false).update(false);
        }

        conf.getModuleConf("ldcp", "ldcp_tcp_port", 4557)
                .update(ldcpPort);
        conf.getModuleConf("stcp", "cla_stcp_port", 4556)
                .update(stcpPort);
        conf.getModuleConf("http", "module_http_port", 8080)
                .update(httpPort);

        CoreApi core = new DtnCore(conf);
        ((DtnCore) core).init();
        return null;
    }

    public static void main(String[] args) throws Exception {
        CommandLine.call(new Terra(), args);
    }
}
// CHECKSTYLE END IGNORE LineLength