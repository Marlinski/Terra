package io.disruptedsystems.libdtn.common.data;

import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import io.disruptedsystems.libdtn.common.data.eid.Api;
import io.disruptedsystems.libdtn.common.data.eid.Cla;
import io.disruptedsystems.libdtn.common.data.eid.Dtn;
import io.disruptedsystems.libdtn.common.data.eid.Ipn;

import static io.disruptedsystems.libdtn.common.data.eid.Api.isApiEid;
import static io.disruptedsystems.libdtn.common.data.eid.Api.swapApiMe;
import static io.disruptedsystems.libdtn.common.data.eid.Cla.isClaEid;
import static io.disruptedsystems.libdtn.common.data.eid.Dtn.isDtnEid;
import static io.disruptedsystems.libdtn.common.data.eid.Ipn.isIpnEid;
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
    public void testIpnEid() {
        try {
            System.out.println("[+] eid: testing IpnEid Scheme");
            URI ipn = new URI("ipn:0.0");
            assertEquals("ipn:0.0", ipn.toString());
            assertEquals(0, Ipn.getNodeNumber(ipn));
            assertEquals(0, Ipn.getServiceNumber(ipn));
            assertEquals(0, Ipn.getNodeNumberUnsafe(ipn));
            assertEquals(0, Ipn.getServiceNumberUnsafe(ipn));

            ipn = new URI("ipn:5.12");
            assertEquals("ipn:5.12", ipn.toString());
            assertEquals(5, Ipn.getNodeNumber(ipn));
            assertEquals(12, Ipn.getServiceNumber(ipn));
            assertEquals(5, Ipn.getNodeNumberUnsafe(ipn));
            assertEquals(12, Ipn.getServiceNumberUnsafe(ipn));

            ipn = new URI("ipn:node.32");
            assertEquals("ipn:node.32", ipn.toString());
            assertFalse(isIpnEid(ipn));
        } catch (URISyntaxException | Ipn.InvalidIpnEid e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        try {
            URI ipn = new URI("ipn:node.32");
            int x = Ipn.getNodeNumber(ipn);
            fail();
        } catch (URISyntaxException | Ipn.InvalidIpnEid e) {
            assertTrue(true);
        }

        try {
            URI ipn = new URI("ipn:node.32");
            int x = Ipn.getServiceNumber(ipn);
            fail();
        } catch (URISyntaxException | Ipn.InvalidIpnEid e) {
            assertTrue(true);
        }
    }

    @Test
    public void testDtnEid() {
        System.out.println("[+] eid: testing dtn Scheme");
        try {
            URI dtn = Dtn.nullEid();
            assertTrue(Dtn.isDtnEid(dtn));
            assertTrue(Dtn.isNullEid(dtn));
            assertEquals("dtn:none", dtn.toString());

            dtn = Dtn.generate();
            assertTrue(Dtn.isDtnEid(dtn));

            dtn = Dtn.create("com.example");
            assertEquals("dtn://com.example/", dtn.toString());
            assertTrue(Dtn.isDtnEid(dtn));
            assertEquals("/", dtn.getPath());
            assertTrue(Dtn.isSingleton(dtn));

            dtn = Dtn.create("com.example", "/sink");
            assertEquals("dtn://com.example/sink", dtn.toString());
            assertTrue(Dtn.isDtnEid(dtn));
            assertTrue(Dtn.isSingleton(dtn));

            dtn = Dtn.create("com.example", "/sink", "x=1&y=2");
            assertEquals("dtn://com.example/sink?x=1&y=2", dtn.toString());
            assertTrue(Dtn.isDtnEid(dtn));
            assertTrue(Dtn.isSingleton(dtn));

            dtn = Dtn.create("com.example", "/sink", "x=1&y=2", "fragment1");
            assertEquals("dtn://com.example/sink?x=1&y=2#fragment1", dtn.toString());
            assertTrue(Dtn.isDtnEid(dtn));
            assertTrue(Dtn.isSingleton(dtn));

            dtn = Dtn.create("com.example", "/~group", "x=1&y=2", "fragment1");
            assertEquals("dtn://com.example/~group?x=1&y=2#fragment1", dtn.toString());
            assertTrue(Dtn.isDtnEid(dtn));
            assertFalse(Dtn.isSingleton(dtn));
        } catch (URISyntaxException | Dtn.InvalidDtnEid efe) {
            efe.printStackTrace();
            fail(efe.getMessage());
        }
    }


    @Test
    public void testApiEid() {
        System.out.println("[+] eid: testing api-dtn eid");

        try {
            URI api = Api.me();
            assertEquals("dtn://api:me/", api.toString());
            assertTrue(isApiEid(api));

            api = Api.me("/path");
            assertEquals("dtn://api:me/path", api.toString());
            assertTrue(isApiEid(api));

            URI dtn = URI.create("dtn://node1/path2/path3");
            assertFalse(isApiEid(dtn));

            api = swapApiMe(api, dtn);
            assertEquals("dtn://node1/path", api.toString());
            assertFalse(isApiEid(api));
            assertTrue(isDtnEid(api));
        } catch (URISyntaxException | Dtn.InvalidDtnEid | Api.InvalidApiEid e) {
            e.printStackTrace();
            fail();
        }

        // cannot swap api:me with non-dtn URI
        try {
            URI api = Api.me("/path");
            api = swapApiMe(api, URI.create("ipn:0.1"));
            fail();
        } catch (URISyntaxException | Dtn.InvalidDtnEid | Api.InvalidApiEid e) {
            assertTrue(true);
        }
    }

    @Test
    public void testClaEid() {
        System.out.println("[+] eid: testing cla-dtn eid");
        try {
            URI cla = Cla.create("camera", "");
            assertEquals("dtn://@camera/", cla.toString());
            assertTrue(isClaEid(cla));
            assertEquals("camera", Cla.getClaScheme(cla));
            assertEquals(null, Cla.getClaParameters(cla));

            cla = Cla.create("stcp", "1.2.3.4:8118");
            assertEquals("dtn://@stcp:1.2.3.4:8118/", cla.toString());
            assertTrue(isClaEid(cla));
            assertEquals("stcp", Cla.getClaScheme(cla));
            assertEquals("1.2.3.4:8118", Cla.getClaParameters(cla));

            cla = Cla.create("stcp", "1.2.3.4:8118", "/sink");
            assertEquals("dtn://@stcp:1.2.3.4:8118/sink", cla.toString());
            assertTrue(isClaEid(cla));
        } catch (URISyntaxException | Cla.InvalidClaEid | Dtn.InvalidDtnEid e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }


}
