package io.disruptedsystems.libdtn.common.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.disruptedsystems.libdtn.common.data.eid.ApiEid;
import io.disruptedsystems.libdtn.common.data.eid.BaseEidFactory;
import io.disruptedsystems.libdtn.common.data.eid.BaseDtnEid;
import io.disruptedsystems.libdtn.common.data.eid.ClaEid;
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
            ApiEid apiEid2 = new ApiEid();
            assertEquals("dtn://api:me/", apiEid2.getEidString());
            assertEquals("dtn", apiEid2.getUri().getScheme());
            assertEquals("//api:me/", apiEid2.getUri().getSchemeSpecificPart());
            assertEquals("api:me", apiEid2.getNodeName());
            assertEquals("", apiEid2.getPath());

            ApiEid apiEid3 = new ApiEid("");
            assertEquals("dtn://api:me/", apiEid3.getEidString());
            assertEquals("dtn", apiEid3.getUri().getScheme());
            assertEquals("//api:me/", apiEid3.getUri().getSchemeSpecificPart());
            assertEquals("api:me", apiEid3.getNodeName());
            assertEquals("", apiEid3.getPath());

            ApiEid apiEid4 = new ApiEid("/");
            assertEquals("dtn://api:me/", apiEid4.getEidString());
            assertEquals("dtn", apiEid4.getUri().getScheme());
            assertEquals("//api:me/", apiEid4.getUri().getSchemeSpecificPart());
            assertEquals("api:me", apiEid4.getNodeName());
            assertEquals("", apiEid4.getPath());

            ApiEid apiEid5 = new ApiEid("/hello");
            assertEquals("dtn://api:me/hello", apiEid5.getEidString());
            assertEquals("dtn", apiEid5.getUri().getScheme());
            assertEquals("//api:me/hello", apiEid5.getUri().getSchemeSpecificPart());
            assertEquals("api:me", apiEid5.getNodeName());
            assertEquals("hello", apiEid5.getPath());

            ApiEid apiEid6 = new ApiEid("/hello/world");
            assertEquals("dtn://api:me/hello/world", apiEid6.getEidString());
            assertEquals("dtn", apiEid6.getUri().getScheme());
            assertEquals("//api:me/hello/world", apiEid6.getUri().getSchemeSpecificPart());
            assertEquals("api:me", apiEid6.getNodeName());
            assertEquals("hello/world", apiEid6.getPath());

            Eid eid = eidFactory.create("dtn://api:me/null/");
            assertEquals("dtn://api:me/null/", eid.getEidString());
            assertEquals(true, eid instanceof ApiEid);
        } catch (EidFormatException e) {
            e.printStackTrace();
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
            DtnEid dtnnone = DtnEid.nullEid();
            assertEquals("dtn:none", dtnnone.getEidString());
            assertEquals(dtnnone.getEidString(), DtnEid.nullEid().getEidString());
            assertEquals(true, dtnnone.isNullEndPoint());

            DtnEid dtn = new BaseDtnEid("marsOrbital");
            assertEquals("dtn://marsOrbital/", dtn.getEidString());
            assertEquals("", dtn.getPath());

            DtnEid dtnping = new BaseDtnEid("marsOrbital","pingservice");
            assertEquals("dtn://marsOrbital/pingservice", dtnping.getEidString());
            assertTrue(dtn.isAuthoritativeOver(dtnping));
            assertTrue(dtnping.isSingleton());
            assertEquals("pingservice",dtnping.getPath());

            DtnEid dtnall = new BaseDtnEid("marsOrbital","~all");
            assertEquals("dtn://marsOrbital/~all", dtnall.getEidString());
            assertFalse(dtnall.isSingleton());
            assertEquals("~all",dtnall.getPath());

            DtnEid colonyall = new BaseDtnEid("nasa.gov","~mars/colony/1/all");
            assertEquals("dtn://nasa.gov/~mars/colony/1/all", colonyall.getEidString());
            assertFalse(colonyall.isSingleton());
            assertEquals("~mars/colony/1/all",colonyall.getPath());

            Eid testParse = eidFactory.create("dtn://nasa.gov/~mars/colony/1/all");
            assertEquals("dtn://nasa.gov/~mars/colony/1/all", testParse.getEidString());
            assertFalse(colonyall.isSingleton());
            assertEquals("~mars/colony/1/all",colonyall.getPath());

            Eid testParse2 = eidFactory.create("dtn://[stcp:1.2.3.4:5]/hello");
            assertEquals("dtn://[stcp:1.2.3.4:5]/hello", testParse2.getEidString());
            assertTrue(testParse2 instanceof DtnEid);
            assertTrue(testParse2 instanceof ClaEid);
            assertEquals("hello",((DtnEid)testParse2).getPath());

            Eid testParse3 = eidFactory.create("dtn://[:]/hello");
            assertEquals("dtn://[:]/hello", testParse3.getEidString());
            assertTrue(testParse3 instanceof DtnEid);
            assertFalse(testParse3 instanceof ClaEid);
            assertEquals("hello",((DtnEid)testParse3).getPath());

            Eid testParse4 = eidFactory.create("dtn://node1/dtnping?seq=12&timestamp=6572657653");
            assertEquals("dtn://node1/dtnping?seq=12&timestamp=6572657653", testParse4.getEidString());
            assertTrue(testParse4 instanceof DtnEid);
            assertEquals("dtnping?seq=12&timestamp=6572657653",((DtnEid)testParse4).getPath());

            Eid testParse5 = eidFactory.create("dtn://node1/dtnping?seq=12&timestamp=6572657653#fragment");
            assertEquals("dtn://node1/dtnping?seq=12&timestamp=6572657653#fragment", testParse5.getEidString());
            assertTrue(testParse5 instanceof DtnEid);
            assertEquals("dtnping?seq=12&timestamp=6572657653#fragment",((DtnEid)testParse5).getPath());

            Eid testParse6 = eidFactory.create("dtn://[stcp:1.2.3.4:8080]/dtnping?seq=12&timestamp=6572657653#fragment");
            assertEquals("dtn://[stcp:1.2.3.4:8080]/dtnping?seq=12&timestamp=6572657653#fragment", testParse6.getEidString());
            assertTrue(testParse6 instanceof DtnEid);
            assertEquals("[stcp:1.2.3.4:8080]",((DtnEid)testParse6).getNodeName());
            assertEquals("dtnping?seq=12&timestamp=6572657653#fragment",((DtnEid)testParse6).getPath());
        } catch (EidFormatException efe) {
            efe.printStackTrace();
            fail(efe.getMessage());
        }
    }

}
