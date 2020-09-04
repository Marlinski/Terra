package io.disruptedsystems.libdtn.module.cla.stcp;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import io.disruptedsystems.libdtn.common.data.eid.Cla;
import io.disruptedsystems.libdtn.common.data.eid.Dtn;

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
            URI cla = ClaStcp.create("google.com", 4556);
            assertEquals("dtn://@stcp:google.com:4556/", cla.toString());
        } catch (URISyntaxException | Dtn.InvalidDtnEid | Cla.InvalidClaEid eid) {
            fail(eid.getMessage());
        }
    }

}
