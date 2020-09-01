package io.disruptedsystems.libdtn.module.cla.bows;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.disruptedsystems.libdtn.common.data.eid.BaseClaEid;
import io.disruptedsystems.libdtn.common.data.eid.ClaEid;
import io.disruptedsystems.libdtn.common.data.eid.Eid;
import io.disruptedsystems.libdtn.common.data.eid.EidFormatException;

import org.junit.Test;

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
            BaseClaEid cla = new ClaBowsEid("ws://google.com", "/");
            assertEquals("cla:bows:ws://google.com?sink=/", cla.getEidString());
            assertEquals("bows", cla.claName);
            assertEquals("ws://google.com", cla.claParameters);
            assertEquals("/", cla.claSink);

            ClaEid eid = (new ClaBowsEidParser()).createClaEid("bows","wss://google.com:4556");
            ClaEid path = (new ClaBowsEidParser()).createClaEid("bows", "wss://google.com:4556?sink=/pingservice");
            assertEquals("cla:bows:wss://google.com:4556", eid.getEidString());
            assertEquals("wss://google.com:4556", eid.getClaParameters());
            assertEquals(null, eid.getSink());
            assertEquals("cla:bows:wss://google.com:4556?sink=/pingservice", path.getEidString());
            assertEquals("wss://google.com:4556", path.getClaParameters());
            assertEquals("/pingservice", path.getSink());
            assertTrue(path.matches(eid));
        } catch (EidFormatException eid) {
            fail(eid.getMessage());
        }
    }

}
