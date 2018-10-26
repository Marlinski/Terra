package io.left.rightmesh.terra;

import java.util.concurrent.Callable;

import io.left.rightmesh.libdtn.core.DTNConfiguration;
import io.left.rightmesh.libdtn.core.DTNCore;
import io.left.rightmesh.libdtn.core.api.CoreAPI;
import io.reactivex.Completable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import static io.left.rightmesh.libdtn.core.api.ConfigurationAPI.CoreEntry.ENABLE_AA_MODULES;
import static io.left.rightmesh.libdtn.core.api.ConfigurationAPI.CoreEntry.ENABLE_CLA_MODULES;
import static io.left.rightmesh.libdtn.core.api.ConfigurationAPI.CoreEntry.ENABLE_CORE_MODULES;
import static io.left.rightmesh.libdtn.core.api.ConfigurationAPI.CoreEntry.MODULES_AA_PATH;
import static io.left.rightmesh.libdtn.core.api.ConfigurationAPI.CoreEntry.MODULES_CLA_PATH;
import static io.left.rightmesh.libdtn.core.api.ConfigurationAPI.CoreEntry.MODULES_CORE_PATH;


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
                "@|green       .                 Terrestrial DTN - v1.0     .                    .  |@",
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
                "Terra is a full node DTN implementation for Terrestrial DTN", },
        optionListHeading = "@|bold %nOptions|@:%n",
        footer = {
                ""})
public class Terra implements Callable<Void> {

    @Option(names = {"-d", "--daemon"}, description = "Start Terra as a daemon.")
    private boolean daemon;

    @Option(names = {"-n", "--module-cla-path"}, description = "set the path to the network Convergence Layer Adapters modules.")
    private String claModuleDirectory;

    @Option(names = {"-a", "--module-aa-path"}, description = "set the path to the Application Agent Adapters modules.")
    private String aaModuleDirectory;

    @Option(names = {"-c", "--module-core-path"}, description = "set the path to the Core modules.")
    private String coreModuleDirectory;

    @Override
    public Void call() throws Exception {
        DTNConfiguration conf = new DTNConfiguration();
        conf.get(ENABLE_CLA_MODULES).update(true);
        conf.get(MODULES_CLA_PATH).update(claModuleDirectory);
        conf.get(ENABLE_AA_MODULES).update(true);
        conf.get(MODULES_AA_PATH).update(aaModuleDirectory);
        conf.get(ENABLE_CORE_MODULES).update(true);
        conf.get(MODULES_CORE_PATH).update(coreModuleDirectory);

        CoreAPI core = DTNCore.init(conf);
        core.getRegistrar().register("/netflix/video/", (bundle) -> {
                System.out.println("receive a Bundle");
                return Completable.complete();
            });
        return null;
    }

    public static void main(String[] args) throws Exception {
        CommandLine.call(new Terra(), args);
    }
}
