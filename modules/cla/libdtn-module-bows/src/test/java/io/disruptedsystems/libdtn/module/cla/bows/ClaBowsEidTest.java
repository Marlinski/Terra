package io.disruptedsystems.libdtn.module.cla.bows;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import io.disruptedsystems.libdtn.common.data.eid.Cla;
import io.disruptedsystems.libdtn.common.data.eid.Dtn;
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
            URI cla = ClaBows.create(URI.create("ws://google.com"));
            assertEquals("dtn://@bows:d3M6Ly9nb29nbGUuY29t/", cla.toString());
            assertEquals("ws://google.com", ClaBows.getWebsocketUrl(cla).toString());

            URI complex = ClaBows.create(URI.create("ws://google.com/path/sub?query=12&35#fragment01"), "/service1");
            assertEquals("dtn://@bows:d3M6Ly9nb29nbGUuY29tL3BhdGgvc3ViP3F1ZXJ5PTEyJjM1I2ZyYWdtZW50MDE-/service1", complex.toString());
            complex = URI.create("dtn://@bows:d3M6Ly9nb29nbGUuY29tL3BhdGgvc3ViP3F1ZXJ5PTEyJjM1I2ZyYWdtZW50MDE-/service1");
            assertEquals("ws://google.com/path/sub?query=12&35#fragment01",  ClaBows.getWebsocketUrl(complex).toString());
        } catch (URISyntaxException | Dtn.InvalidDtnEid | Cla.InvalidClaEid | ClaBows.InvalidClaBowsEid e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

}
