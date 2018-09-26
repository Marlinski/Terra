package io.left.rightmesh.libdtn.data.bundleV7;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Formatter;

import io.left.rightmesh.libcbor.CborEncoder;
import io.left.rightmesh.libcbor.CborParser;
import io.left.rightmesh.libcbor.rxparser.RxParserException;
import io.left.rightmesh.libdtn.data.AgeBlock;
import io.left.rightmesh.libdtn.data.Block;
import io.left.rightmesh.libdtn.data.BlockHeader;
import io.left.rightmesh.libdtn.data.Bundle;
import io.left.rightmesh.libdtn.data.EID;
import io.left.rightmesh.libdtn.data.PayloadBlock;
import io.left.rightmesh.libdtn.data.PreviousNodeBlock;
import io.left.rightmesh.libdtn.data.PrimaryBlock;
import io.left.rightmesh.libdtn.data.ScopeControlHopLimitBlock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Lucien Loiseau on 20/09/18.
 */
public class BundleV7Test {

    public static String testPayload = "This is a test for bundle serialization";


    public static Bundle testBundle0() {
        Bundle bundle = new Bundle();
        bundle.destination = EID.createIPN(5, 12);
        bundle.source = EID.createDTN("source");
        bundle.reportto = EID.NullEID();
        return bundle;
    }

    public static Bundle testBundle1() {
        Bundle bundle = testBundle0();
        bundle.addBlock(new PayloadBlock(testPayload));
        return bundle;
    }

    public static Bundle testBundle2() {
        Bundle bundle = testBundle1();
        bundle.addBlock(new AgeBlock());
        return bundle;
    }

    public static Bundle testBundle3() {
        Bundle bundle = testBundle1();
        bundle.addBlock(new AgeBlock());
        bundle.addBlock(new ScopeControlHopLimitBlock());
        return bundle;
    }

    public static Bundle testBundle4() {
        Bundle bundle = testBundle1();
        bundle.addBlock(new AgeBlock());
        bundle.addBlock(new ScopeControlHopLimitBlock());
        bundle.addBlock(new PreviousNodeBlock(EID.generate()));
        return bundle;
    }


    public static Bundle testBundle5() {
        Bundle bundle = testBundle1();
        bundle.addBlock(new AgeBlock());
        bundle.addBlock(new ScopeControlHopLimitBlock());
        bundle.addBlock(new PreviousNodeBlock(EID.generate()));
        bundle.crcType = PrimaryBlock.CRCFieldType.CRC_32;
        return bundle;
    }

    public static Bundle testBundle6() {
        Bundle bundle = testBundle0();
        bundle.crcType = PrimaryBlock.CRCFieldType.CRC_32;

        Block age = new AgeBlock();
        Block scope = new ScopeControlHopLimitBlock();
        Block payload = new PayloadBlock(testPayload);
        Block previous = new PreviousNodeBlock();

        age.crcType = BlockHeader.CRCFieldType.CRC_16;
        scope.crcType = BlockHeader.CRCFieldType.CRC_16;
        payload.crcType = BlockHeader.CRCFieldType.CRC_32;
        previous.crcType = BlockHeader.CRCFieldType.CRC_32;

        bundle.addBlock(age);
        bundle.addBlock(scope);
        bundle.addBlock(payload);
        bundle.addBlock(previous);
        return bundle;
    }

    @Test
    public void testSimpleBundleSerialization() {
        System.out.println("[+] testing bundle serialization and parsing with 6 test bundles");

        Bundle[] bundles = {
                testBundle1(),
                testBundle2(),
                testBundle3(),
                testBundle4(),
                testBundle5(),
                testBundle6()
        };

        for(Bundle bundle : bundles) {
            Bundle[] res = {null};

            // prepare serializer
            CborEncoder enc = BundleV7Serializer.encode(bundle);

            // prepare parser
            CborParser p = BundleV7Parser.create(b -> {
                res[0] = b;
            });

            // serialize and parse
            enc.observe(10).subscribe(buf -> {
                try {
                    if (p.read(buf)) {
                        assertEquals(false, buf.hasRemaining());
                    }
                } catch (RxParserException rpe) {
                    rpe.printStackTrace();
                    fail();
                }
            });

            // check payload
            checkBundlePayload(res[0]);
        }
    }



    void checkBundlePayload(Bundle bundle) {
        // assert
        assertEquals(true, bundle != null);
        String[] payload = {null};
        if (bundle != null) {
            for(Block block : bundle.getBlocks()) {
                assertEquals(true, block.crc_ok);
            }

            bundle.getPayloadBlock().data.observe().subscribe(
                    buffer -> {
                        byte[] arr = new byte[buffer.remaining()];
                        buffer.get(arr);
                        payload[0] = new String(arr);
                    });

            assertEquals(true, payload[0] != null);
            if (payload[0] != null) {
                assertEquals(testPayload, payload[0]);
            }
        }
    }


    // debug
    private String getEncodedString(CborEncoder enc) {
        // get all in one buffer
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        enc.observe().subscribe(b -> {
            while (b.hasRemaining()) {
                baos.write(b.get());
            }
        });

        // return the string
        Formatter formatter = new Formatter();
        formatter.format("0x");
        for (byte b : baos.toByteArray()) {
            formatter.format("%02x", b);
        }
        return (formatter.toString());
    }

    private void showRemaining(String prefix, ByteBuffer buf) {
        Formatter formatter = new Formatter();
        formatter.format(prefix + " remaining (" + buf.remaining() + "): 0x");
        while (buf.hasRemaining()) {
            formatter.format("%02x", buf.get());
        }
        System.out.println(formatter.toString());
    }

}
