package io.disruptedsystems.libdtn.common.data;

import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import io.disruptedsystems.libdtn.common.data.eid.Api;
import io.disruptedsystems.libdtn.common.data.eid.Dtn;
import io.disruptedsystems.libdtn.common.data.eid.Ipn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test class for Endpoint IDs.
 *
 * @author Lucien Loiseau on 20/09/18.
 */
public class EidTest {

    @Test
    public void testApiEid() {
        System.out.println("[+] eid: testing ApiEid Scheme");
        try {
            URI apiEid2 = Api.me();
            assertEquals("dtn://api:me/", apiEid2.toString());
            assertEquals("dtn", apiEid2.getScheme());
            assertEquals("//api:me/", apiEid2.getSchemeSpecificPart());
            assertEquals("api:me", apiEid2.getAuthority());
            assertEquals("/", apiEid2.getPath());

            URI apiEid3 = new URI("");
            assertEquals("dtn://api:me/", apiEid3.toString());
            assertEquals("dtn", apiEid3.getScheme());
            assertEquals("//api:me/", apiEid3.getSchemeSpecificPart());
            assertEquals("api:me", apiEid3.getAuthority());
            assertEquals("/", apiEid3.getPath());

            URI apiEid4 = new URI("/");
            assertEquals("dtn://api:me/", apiEid4.toString());
            assertEquals("dtn", apiEid4.getScheme());
            assertEquals("//api:me/", apiEid4.getSchemeSpecificPart());
            assertEquals("api:me", apiEid4.getAuthority());
            assertEquals("", apiEid4.getPath());

            URI apiEid5 = new URI("/hello");
            assertEquals("dtn://api:me/hello", apiEid5.toString());
            assertEquals("dtn", apiEid5.getScheme());
            assertEquals("//api:me/hello", apiEid5.getSchemeSpecificPart());
            assertEquals("api:me", apiEid5.getAuthority());
            assertEquals("hello", apiEid5.getPath());

            URI apiEid6 = new URI("/hello/world");
            assertEquals("dtn://api:me/hello/world", apiEid6.toString());
            assertEquals("dtn", apiEid6.getScheme());
            assertEquals("//api:me/hello/world", apiEid6.getSchemeSpecificPart());
            assertEquals("api:me", apiEid6.getAuthority());
            assertEquals("hello/world", apiEid6.getPath());

            URI eid = new URI("dtn://api:me/null/");
            assertEquals("dtn://api:me/null/", eid.toString());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testIpnEid() {
        try {
            System.out.println("[+] eid: testing IpnEid Scheme");
            URI ipnEid = new URI("ipn:0.0");
            assertEquals("ipn:0.0", ipnEid.toString());
            assertEquals(0, Ipn.getNodeNumber(ipnEid));
            assertEquals(0, Ipn.getServiceNumber(ipnEid));

            ipnEid = new URI("ipn:15.32");
            assertEquals("ipn:15.32", ipnEid.toString());
            assertEquals(15, Ipn.getNodeNumber(ipnEid));
            assertEquals(32, Ipn.getServiceNumber(ipnEid));
        } catch (URISyntaxException | Ipn.InvalidIpnEid eid) {
            fail(eid.getMessage());
        }
    }

    @Test
    public void testDtnEid() {
        System.out.println("[+] eid: testing DtnEid Scheme");
        try {
            URI dtnnone = Dtn.nullEid();
            assertEquals("dtn:none", dtnnone.toString());
            assertEquals(true, Dtn.isNullEid(dtnnone));


            URI dtnping = new URI("dtn", "//marsOrbital/pingservice/", null);
            assertEquals("dtn://marsOrbital/pingservice/", dtnping.toString());
            assertTrue(Dtn.isSingleton(dtnping));
            assertEquals("pingservice", dtnping.getPath());
/*
            URI dtnall = new URI("marsOrbital", "~all");
            assertEquals("dtn://marsOrbital/~all", dtnall.toString());
            assertFalse(dtnall.isSingleton());
            assertEquals("~all", dtnall.getPath());

            URI colonyall = new URI("nasa.gov", "~mars/colony/1/all");
            assertEquals("dtn://nasa.gov/~mars/colony/1/all", colonyall.toString());
            assertFalse(colonyall.isSingleton());
            assertEquals("~mars/colony/1/all", colonyall.getPath());

            URI testParse = eidFactory.create("dtn://nasa.gov/~mars/colony/1/all");
            assertEquals("dtn://nasa.gov/~mars/colony/1/all", testParse.toString());
            assertFalse(colonyall.isSingleton());
            assertEquals("~mars/colony/1/all", colonyall.getPath());

            URI testParse2 = eidFactory.create("dtn://[stcp:1.2.3.4:5]/hello");
            assertEquals("dtn://[stcp:1.2.3.4:5]/hello", testParse2.toString());
            assertTrue(testParse2 instanceof DtnEid);
            assertTrue(testParse2 instanceof ClaEid);
            assertEquals("hello", ((DtnEid) testParse2).getPath());

            URI testParse3 = eidFactory.create("dtn://[:]/hello");
            assertEquals("dtn://[:]/hello", testParse3.toString());
            assertTrue(testParse3 instanceof DtnEid);
            assertFalse(testParse3 instanceof ClaEid);
            assertEquals("hello", ((DtnEid) testParse3).getPath());

            URI testParse4 = eidFactory.create("dtn://node1/dtnping?seq=12&timestamp=6572657653");
            assertEquals("dtn://node1/dtnping?seq=12&timestamp=6572657653", testParse4.toString());
            assertTrue(testParse4 instanceof DtnEid);
            assertEquals("dtnping?seq=12&timestamp=6572657653", ((DtnEid) testParse4).getPath());

            URI testParse5 = eidFactory.create("dtn://node1/dtnping?seq=12&timestamp=6572657653#fragment");
            assertEquals("dtn://node1/dtnping?seq=12&timestamp=6572657653#fragment", testParse5.toString());
            assertTrue(testParse5 instanceof DtnEid);
            assertEquals("dtnping?seq=12&timestamp=6572657653#fragment", ((DtnEid) testParse5).getPath());

            URI testParse6 = eidFactory.create("dtn://[stcp:1.2.3.4:8080]/dtnping?seq=12&timestamp=6572657653#fragment");
            assertEquals("dtn://[stcp:1.2.3.4:8080]/dtnping?seq=12&timestamp=6572657653#fragment", testParse6.toString());
            assertTrue(testParse6 instanceof DtnEid);
            assertEquals("[stcp:1.2.3.4:8080]", ((DtnEid) testParse6).getAuthority());
            assertEquals("dtnping?seq=12&timestamp=6572657653#fragment", ((DtnEid) testParse6).getPath());

 */
        } catch (URISyntaxException efe) {
            efe.printStackTrace();
            fail(efe.getMessage());
        }
    }

}
