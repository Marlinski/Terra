package io.disruptedsystems.libdtn.module.core.ipdiscovery;

import java.net.URI;

import io.disruptedsystems.libdtn.core.api.ConfigurationApi;
import io.disruptedsystems.libdtn.core.api.CoreApi;
import io.disruptedsystems.libdtn.core.spi.CoreModuleSpi;
import io.marlinski.libdetect.ActionListener;
import io.marlinski.libdetect.LibDetect;
import io.marlinski.libdetect.PeerReachable;
import io.marlinski.libdetect.PeerUnreachable;

/**
 * Core module performing ip device discovery on the connected lans.
 *
 * @author Lucien Loiseau on 27/11/18.
 */
public class CoreModuleIpDiscovery implements CoreModuleSpi {

    private static final String TAG = "IpDiscovery";

    private CoreApi core;

    @Override
    public String getModuleName() {
        return "ipdiscovery";
    }

    @Override
    public void init(CoreApi api) {
        this.core = api;
        LibDetect.start(4000, new ActionListener() {
            @Override
            public void onPeerReachable(PeerReachable peer) {
                api.getLogger().i(TAG, "peer detected :" + peer.address.getHostAddress());
                if (core.getConf().<Boolean>get(ConfigurationApi.CoreEntry
                        .ENABLE_AUTO_CONNECT_FOR_DETECT_EVENT).value()) {
                    URI eid = URI.create("dtn://@stcp:" + peer.address.getHostAddress() + ":" + "4556/");
                    core.getClaManager().createOpportunity(eid).subscribe(
                            channel -> {
                                // ignore
                            },
                            e -> {
                                // ignore
                            }
                    );
                }
            }

            @Override
            public void onPeerUnreachable(PeerUnreachable peer) {
                api.getLogger().i(TAG, "peer unreachable");
            }
        }, true);
    }

}