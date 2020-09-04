package io.disruptedsystems.libdtn.core;

import org.junit.Test;

import java.net.URI;

import io.disruptedsystems.libdtn.core.api.ConfigurationApi;

import static org.junit.Assert.assertEquals;

/**
 * @author Lucien Loiseau on 28/09/18.
 */
public class CoreConfigurationTest {

    @Test
    public void testLocalEIDConfiguration() {
        URI testEid = URI.create("dtn://test/");
        CoreConfiguration conf = new CoreConfiguration();
        conf.<URI>get(ConfigurationApi.CoreEntry.LOCAL_EID).update(testEid);
        URI localEid = conf.<URI>get(ConfigurationApi.CoreEntry.LOCAL_EID).value();
        assertEquals(testEid, localEid);
    }

}
