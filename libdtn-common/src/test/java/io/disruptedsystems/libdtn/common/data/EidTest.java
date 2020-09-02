package io.disruptedsystems.libdtn.common.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.disruptedsystems.libdtn.common.data.eid.ApiEid;
import io.disruptedsystems.libdtn.common.data.eid.BaseEidFactory;
import io.disruptedsystems.libdtn.common.data.eid.DtnEid;
import io.disruptedsystems.libdtn.common.data.eid.Eid;
import io.disruptedsystems.libdtn.common.data.eid.EidFactory;
import io.disruptedsystems.libdtn.common.data.eid.EidFormatException;
import io.disruptedsystems.libdtn.common.data.eid.IpnEid;

import org.junit.Test;

/**
 * Test class for Endpoint IDs.
 *
 * @author Lucien Loiseau on 20/09/18.
 */
public class EidTest {

    EidFactory eidFactory = new BaseEidFactory();

    @Test
    public void testApiEid() {
        System.out.println("[+] eid: testing ApiEid Scheme");
        try {
            ApiEid apiEid2 = new ApiEid(null);
            assertEquals("api:me", apiEid2.getEidString());
            assertEquals("api", apiEid2.getScheme());
            assertEquals("me", apiEid2.getSsp());
            assertEquals("", apiEid2.getSink());

            ApiEid apiEid3 = new ApiEid("");
            assertEquals("api:me", apiEid3.getEidString());
            assertEquals("api", apiEid3.getScheme());
            assertEquals("me", apiEid3.getSsp());
            assertEquals("", apiEid3.getSink());

            ApiEid apiEid4 = new ApiEid("/");
            assertEquals("api:me", apiEid4.getEidString());
            assertEquals("api", apiEid4.getScheme());
            assertEquals("me", apiEid4.getSsp());
            assertEquals("", apiEid4.getSink());

            ApiEid apiEid5 = new ApiEid("/hello");
            assertEquals("api:me/hello", apiEid5.getEidString());
            assertEquals("api", apiEid5.getScheme());
            assertEquals("me/hello", apiEid5.getSsp());
            assertEquals("hello", apiEid5.getSink());

            ApiEid apiEid6 = new ApiEid("/hello/world");
            assertEquals("api:me/hello/world", apiEid6.getEidString());
            assertEquals("api", apiEid6.getScheme());
            assertEquals("me/hello/world", apiEid6.getSsp());
            assertEquals("hello/world", apiEid6.getSink());

            Eid eid = eidFactory.create("api:me/null/");
            assertEquals("api:me/null/", eid.getEidString());
            assertEquals(true, eid instanceof ApiEid);
        } catch (EidFormatException e) {
            fail();
        }
    }

    @Test
    public void testIpnEid() {
        System.out.println("[+] eid: testing IpnEid Scheme");
        IpnEid ipnEid = new IpnEid(0, 0);
        assertEquals("ipn:0.0", ipnEid.getEidString());
        assertEquals(0, ipnEid.nodeNumber);
        assertEquals(0, ipnEid.serviceNumber);

        ipnEid = new IpnEid(15, 32);
        assertEquals("ipn:15.32", ipnEid.getEidString());
        assertEquals(15, ipnEid.nodeNumber);
        assertEquals(32, ipnEid.serviceNumber);

        try {
            Eid eid = eidFactory.create("ipn:0.0");
            assertEquals("ipn:0.0", eid.getEidString());
        } catch (EidFormatException eid) {
            fail(eid.getMessage());
        }
    }

    @Test
    public void testDtnEid() {
        System.out.println("[+] eid: testing DtnEid Scheme");
        try {
            DtnEid dtnnone = new DtnEid("none");
            assertEquals("dtn:none", dtnnone.getEidString());
            assertEquals(dtnnone.getEidString(), DtnEid.nullEid().getEidString());
            assertEquals(true, dtnnone.isNullEndPoint());

            DtnEid dtn = new DtnEid("//marsOrbital/");
            assertEquals("dtn://marsOrbital/", dtn.getEidString());
            assertEquals("", dtn.getDemux());

            DtnEid dtnping = new DtnEid("//marsOrbital/pingservice");
            assertEquals("dtn://marsOrbital/pingservice", dtnping.getEidString());
            assertTrue(dtnping.matches(dtn));
            assertTrue(dtnping.isSingleton());
            assertEquals("pingservice",dtnping.getDemux());

            DtnEid dtnall = new DtnEid("//marsOrbital/~all");
            assertEquals("dtn://marsOrbital/~all", dtnall.getEidString());
            assertFalse(dtnall.isSingleton());
            assertEquals("~all",dtnall.getDemux());

            DtnEid colonyall = new DtnEid("//nasa.gov/~mars/colony/1/all");
            assertEquals("dtn://nasa.gov/~mars/colony/1/all", colonyall.getEidString());
            assertFalse(colonyall.isSingleton());
            assertEquals("~mars/colony/1/all",colonyall.getDemux());

        } catch (EidFormatException efe) {
            efe.printStackTrace();
            fail(efe.getMessage());
        }
    }
}
