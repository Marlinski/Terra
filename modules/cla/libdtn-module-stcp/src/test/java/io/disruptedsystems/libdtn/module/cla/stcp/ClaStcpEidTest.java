package io.disruptedsystems.libdtn.module.cla.stcp;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.disruptedsystems.libdtn.common.data.eid.BaseClaEid;
import io.disruptedsystems.libdtn.common.data.eid.Eid;
import io.disruptedsystems.libdtn.common.data.eid.EidFormatException;

import org.junit.Test;

/**
 * Test class for the ClaStcpEid.
 *
 * @author Lucien Loiseau on 28/11/18.
 */
public class ClaStcpEidTest {

    @Test
    public void testClaStcpEid() {
        System.out.println("[+] eid: testing BaseClaEid Scheme");

        try {
            BaseClaEid cla = new ClaStcpEid("google.com", 4556, "/");
            assertEquals("dtn://[stcp:google.com:4556]/", cla.getEidString());
            assertEquals("stcp", cla.getClaName());
            assertEquals("google.com:4556", cla.getClaParameters());
            assertEquals("", cla.getDemux());

            Eid eid = (new ClaStcpEidParser()).createClaEid("stcp","google.com:4556","");
            assertEquals("dtn://[stcp:google.com:4556]/", eid.getEidString());

            Eid path = (new ClaStcpEidParser()).createClaEid("stcp", "google.com:4556", "/pingservice");
            assertEquals("dtn://[stcp:google.com:4556]/pingservice", path.getEidString());
            assertTrue(eid.isAuthoritativeOver(path));

        } catch (EidFormatException eid) {
            fail(eid.getMessage());
        }
    }

}
