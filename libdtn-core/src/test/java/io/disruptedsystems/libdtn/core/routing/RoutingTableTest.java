package io.disruptedsystems.libdtn.core.routing;

import org.junit.Test;

import java.net.URI;
import java.util.List;

import io.disruptedsystems.libdtn.common.utils.Log;
import io.disruptedsystems.libdtn.common.utils.SimpleLogger;
import io.disruptedsystems.libdtn.core.CoreConfiguration;
import io.disruptedsystems.libdtn.core.MockCore;
import io.disruptedsystems.libdtn.core.api.ConfigurationApi;
import io.disruptedsystems.libdtn.core.api.CoreApi;

import static io.disruptedsystems.libdtn.core.api.ConfigurationApi.CoreEntry.COMPONENT_ENABLE_ROUTING;
import static org.junit.Assert.assertEquals;

/**
 * @author Lucien Loiseau on 11/09/20.
 */
public class RoutingTableTest {

    CoreConfiguration conf = new CoreConfiguration();
    private CoreApi mockCore = mockCore();

    public CoreApi mockCore() {
        return new MockCore() {
            {{
                conf.<Boolean>get(COMPONENT_ENABLE_ROUTING).update(true);
            }}

            @Override
            public ConfigurationApi getConf() {
                return conf;
            }

            @Override
            public Log getLogger() {
                return new SimpleLogger();
            }
        };
    }

    @Test
    public void testRoutingTable() {
        RoutingTable table = new RoutingTable(mockCore);
        table.initComponent(mockCore.getConf().get(COMPONENT_ENABLE_ROUTING), mockCore.getLogger());

        table.addRoute(URI.create("dtn://mars.orbital/station/1"), URI.create("dtn://@cla:stcp:nasa.gov:7777/"));
        table.addRoute(URI.create("dtn://mars.orbital/station/1"), URI.create("dtn://mars.orbital/mule/001"));
        table.addRoute(URI.create("dtn://mars.orbital/mule/001"), URI.create("dtn://mars.orbital/mule/002"));
        table.addRoute(URI.create("dtn://mars.orbital/station/1"), URI.create("dtn://spacex/mission/scx01"));
        table.addRoute(URI.create("dtn://spacex/mission/scx01"), URI.create("dtn://@cla:stcp:spacex.io:7777/"));
        table.addRoute(URI.create("dtn://nodle.io/gateway/report"), URI.create("dtn://@cla:stcp:nodle.io:7777/"));

        List<URI> clas = table.findClaForEid(URI.create("dtn://mars.orbital/station/1")).toList().blockingGet();
        for(URI cla : clas) {
            System.out.println(cla);
        }
        assertEquals(2, clas.size());

        System.out.println("-----");

        List<URI> eids = table.findEidForCla(URI.create("dtn://@cla:stcp:spacex.io:7777/")).toList().blockingGet();
        for(URI eid : eids) {
            System.out.println(eid);
        }
        assertEquals(3, eids.size());

        // add a routing loop
        table.addRoute(URI.create("dtn://spacex/mission/scx01"), URI.create("dtn://mars.orbital/station/1"));

        clas = table.findClaForEid(URI.create("dtn://mars.orbital/station/1")).toList().blockingGet();
        for(URI cla : clas) {
            System.out.println(cla);
        }
        assertEquals(2, clas.size());

        System.out.println("-----");

        eids = table.findEidForCla(URI.create("dtn://@cla:stcp:spacex.io:7777/")).toList().blockingGet();
        for(URI eid : eids) {
            System.out.println(eid);
        }
        assertEquals(3, eids.size());
    }

}
