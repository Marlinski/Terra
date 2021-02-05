package io.disruptedsystems.terra;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;

import io.disruptedsystems.libdtn.core.CoreConfiguration;
import io.disruptedsystems.libdtn.core.DtnCore;
import io.disruptedsystems.libdtn.core.api.ConfigurationApi;
import io.disruptedsystems.libdtn.core.api.CoreApi;
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

    @Option(names = {"-m", "--modules"},
            description = "set the path to the dtn modules.")
    private String moduleDirectory = null;

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
                URI eid = new URI(localEid);
                conf.get(ConfigurationApi.CoreEntry.LOCAL_EID).update(eid);
            } catch (URISyntaxException efe) {
                throw new Exception("localEid is not a valid Endpoint ID: " + efe.getMessage());
            }
        }

        if (volatileStorage) {
            conf.get(ConfigurationApi.CoreEntry.BLOB_VOLATILE_MAX_SIZE).update(volatileLimit);
        }

        if (persistentStorage) {
            conf.<String>get(ConfigurationApi.CoreEntry.PERSISTENCE_STORAGE_PATH).update(simplePath);
        }

        conf.get(ConfigurationApi.CoreEntry.ENABLE_STATUS_REPORTING).update(!disableReporting);
        conf.get(ConfigurationApi.CoreEntry.ENABLE_FORWARDING).update(!disableForwarding);
        conf.get(ConfigurationApi.CoreEntry.ENABLE_AUTO_CONNECT_FOR_BUNDLE).update(!disableEidAutoconnect);
        conf.get(ConfigurationApi.CoreEntry.ENABLE_AUTO_CONNECT_FOR_DETECT_EVENT).update(!disablePeerAutoconnect);

        conf.get(ConfigurationApi.CoreEntry.ENABLE_CLA_MODULES).update(true);
        conf.get(ConfigurationApi.CoreEntry.MODULES_CLA_PATH).update(moduleDirectory);
        conf.get(ConfigurationApi.CoreEntry.ENABLE_AA_MODULES).update(true);
        conf.get(ConfigurationApi.CoreEntry.MODULES_AA_PATH).update(moduleDirectory);
        conf.get(ConfigurationApi.CoreEntry.ENABLE_CORE_MODULES).update(true);
        conf.get(ConfigurationApi.CoreEntry.MODULES_CORE_PATH).update(moduleDirectory);

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

        DtnCore core = new DtnCore(conf);
        core.init();
        return null;
    }

    public static void main(String[] args) throws Exception {
        CommandLine.call(new Terra(), args);
    }
}
// CHECKSTYLE END IGNORE LineLength