package io.left.rightmesh.libdtn.core;

import org.junit.Test;

import io.left.rightmesh.libdtn.common.data.eid.DTN;
import io.left.rightmesh.libdtn.common.data.eid.EID;
import io.left.rightmesh.libdtn.core.api.ConfigurationAPI;

import static org.junit.Assert.assertEquals;

/**
 * @author Lucien Loiseau on 28/09/18.
 */
public class DTNConfigurationTest {


    @Test
    public void testLocalEIDConfiguration() {
        try {
            EID testEID = DTN.create("test");
            DTNConfiguration conf = new DTNConfiguration();
            conf.<EID>get(ConfigurationAPI.CoreEntry.LOCAL_EID).update(testEID);
            EID localEID = conf.<EID>get(ConfigurationAPI.CoreEntry.LOCAL_EID).value();
            assertEquals(testEID.getEIDString(), localEID.getEIDString());
        } catch(EID.EIDFormatException ignore) {
            // sould not happen
            //todo: create a safe EID constructor by encoding URI
        }
    }

}
