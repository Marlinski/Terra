package io.disruptedsystems.libdtn.core;

import static org.junit.Assert.assertEquals;

import io.disruptedsystems.libdtn.core.api.ConfigurationApi;
import io.disruptedsystems.libdtn.common.data.eid.DtnEid;
import io.disruptedsystems.libdtn.common.data.eid.Eid;
import io.disruptedsystems.libdtn.common.data.eid.EidFormatException;

import org.junit.Test;

/**
 * @author Lucien Loiseau on 28/09/18.
 */
public class CoreConfigurationTest {
    
    @Test
    public void testLocalEIDConfiguration() {
        try {
            Eid testEid = new DtnEid("test");
            CoreConfiguration conf = new CoreConfiguration();
            conf.<Eid>get(ConfigurationApi.CoreEntry.LOCAL_EID).update(testEid);
            Eid localEid = conf.<Eid>get(ConfigurationApi.CoreEntry.LOCAL_EID).value();
            assertEquals(testEid.getEidString(), localEid.getEidString());
        } catch(EidFormatException ignore) {
            // sould not happen
            //todo: create a safe Eid constructor by encoding URI
        }
    }

}
