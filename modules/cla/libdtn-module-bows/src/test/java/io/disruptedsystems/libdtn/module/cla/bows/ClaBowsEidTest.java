package io.disruptedsystems.libdtn.module.cla.bows;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.disruptedsystems.libdtn.common.data.eid.BaseClaEid;
import io.disruptedsystems.libdtn.common.data.eid.ClaEid;
import io.disruptedsystems.libdtn.common.data.eid.Eid;
import io.disruptedsystems.libdtn.common.data.eid.EidFormatException;

import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Test class for the ClaStcpEid.
 *
 * @author Lucien Loiseau on 28/11/18.
 */
public class ClaBowsEidTest {

    @Test
    public void testClaBowsEid() {
        System.out.println("[+] eid: testing BaseClaEid Scheme");

        try {
            BaseClaEid cla = new ClaBowsEid(new URI("ws://google.com"), "/");
            assertEquals("dtn://[bows:d3M6Ly9nb29nbGUuY29t]/", cla.getEidString());
            assertEquals("bows", cla.getClaName());
            assertEquals("d3M6Ly9nb29nbGUuY29t", cla.getClaParameters());
            assertEquals("", cla.getDemux());

            ClaEid eid = (new ClaBowsEidParser()).createClaEid("bows","d3NzOi8vZ29vZ2xlLmNvbTo0NTU2","");
            assertEquals("dtn://[bows:d3NzOi8vZ29vZ2xlLmNvbTo0NTU2]/", eid.getEidString());
            assertEquals("d3NzOi8vZ29vZ2xlLmNvbTo0NTU2", eid.getClaParameters());
            assertEquals("", eid.getDemux());

            ClaEid path = (new ClaBowsEidParser()).createClaEid("bows", "d3NzOi8vZ29vZ2xlLmNvbTo0NTU2", "pingservice");
            assertEquals("dtn://[bows:d3NzOi8vZ29vZ2xlLmNvbTo0NTU2]/pingservice", path.getEidString());
            assertEquals("d3NzOi8vZ29vZ2xlLmNvbTo0NTU2", path.getClaParameters());
            assertEquals("pingservice", path.getDemux());
            assertEquals("wss://google.com:4556", ((ClaBowsEid)path).getUri().toASCIIString());

            assertTrue(eid.isAuthoritativeOver(path));

            BaseClaEid complex = new ClaBowsEid(new URI("ws://google.com/path/sub?query=12&35#fragment01"), "/service1");
            assertEquals("dtn://[bows:d3M6Ly9nb29nbGUuY29tL3BhdGgvc3ViP3F1ZXJ5PTEyJjM1I2ZyYWdtZW50MDE-]/service1", complex.getEidString());
            BaseClaEid complexi = new ClaBowsEid("d3M6Ly9nb29nbGUuY29tL3BhdGgvc3ViP3F1ZXJ5PTEyJjM1I2ZyYWdtZW50MDE-", "/service1");
            assertEquals("dtn://[bows:d3M6Ly9nb29nbGUuY29tL3BhdGgvc3ViP3F1ZXJ5PTEyJjM1I2ZyYWdtZW50MDE-]/service1", complexi.getEidString());
            assertTrue(complex.equals(complexi));
        } catch (EidFormatException | URISyntaxException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

}
