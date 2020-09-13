package io.disruptedsystems.libdtn.common.data.bundlev7;

import org.junit.Test;

import java.net.URI;

import io.disruptedsystems.libdtn.common.BaseExtensionToolbox;
import io.disruptedsystems.libdtn.common.data.AgeBlock;
import io.disruptedsystems.libdtn.common.data.BlockHeader;
import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.CanonicalBlock;
import io.disruptedsystems.libdtn.common.data.HopCountBlock;
import io.disruptedsystems.libdtn.common.data.PayloadBlock;
import io.disruptedsystems.libdtn.common.data.PreviousNodeBlock;
import io.disruptedsystems.libdtn.common.data.PrimaryBlock;
import io.disruptedsystems.libdtn.common.data.blob.BaseBlobFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.parser.BundleV7Item;
import io.disruptedsystems.libdtn.common.data.bundlev7.serializer.BaseBlockDataSerializerFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.serializer.BundleV7Serializer;
import io.disruptedsystems.libdtn.common.data.eid.Dtn;
import io.disruptedsystems.libdtn.common.utils.SimpleLogger;
import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborParser;
import io.marlinski.libcbor.rxparser.RxParserException;
import io.reactivex.rxjava3.core.Flowable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test class to test serialization and parsing of a Bundle.
 *
 * @author Lucien Loiseau on 20/09/18.
 */
public class BundleV7Test {

    public static String testPayload = "This is a test for bundle serialization";

    private static URI makeSourceEid() {
        return URI.create("dtn://source/");
    }

    /**
     * create a simple test Bundle with no payload.
     *
     * @return a Bundle
     */
    public static Bundle testBundle0() {
        Bundle bundle = new Bundle();
        bundle.setDestination(URI.create("ipn:5.12"));
        bundle.setSource(makeSourceEid());
        bundle.setReportTo(Dtn.nullEid());
        return bundle;
    }

    /**
     * create a simple test Bundle with a payload.
     *
     * @return a Bundle
     */
    public static Bundle testBundle1() {
        Bundle bundle = testBundle0();
        bundle.addBlock(new PayloadBlock(testPayload));
        return bundle;
    }

    /**
     * create a simple test Bundle with payload and an ageblock.
     *
     * @return a Bundle
     */
    public static Bundle testBundle2() {
        Bundle bundle = testBundle1();
        bundle.addBlock(new AgeBlock());
        return bundle;
    }

    /**
     * create a simple test Bundle with payload, an ageblock and a scopecontrolhoplimit.
     *
     * @return a Bundle.
     */
    public static Bundle testBundle3() {
        Bundle bundle = testBundle1();
        bundle.addBlock(new AgeBlock());
        bundle.addBlock(new HopCountBlock());
        return bundle;
    }

    /**
     * create a simple test Bundle with payload, an ageblock and a scopecontrolhoplimit
     * and previous node block.
     *
     * @return a Bundle
     */
    public static Bundle testBundle4() {
        Bundle bundle = testBundle1();
        bundle.addBlock(new AgeBlock());
        bundle.addBlock(new HopCountBlock());
        bundle.addBlock(new PreviousNodeBlock(Dtn.generate()));
        return bundle;
    }

    /**
     * create a simple test Bundle with payload, an ageblock and a scopecontrolhoplimit,
     * previous node block and enable crc on primary block.
     *
     * @return a Bundle
     */
    public static Bundle testBundle5() {
        Bundle bundle = testBundle1();
        bundle.addBlock(new AgeBlock());
        bundle.addBlock(new HopCountBlock());
        bundle.addBlock(new PreviousNodeBlock(Dtn.generate()));
        bundle.setCrcType(PrimaryBlock.CrcFieldType.CRC_32);
        return bundle;
    }

    /**
     * create a simple test Bundle with payload, an ageblock and a scopecontrolhoplimit,
     * previous node block and enable crc on all block.
     *
     * @return a Bundle
     */
    public static Bundle testBundle6() {
        Bundle bundle = testBundle0();
        bundle.setCrcType(PrimaryBlock.CrcFieldType.CRC_32);

        CanonicalBlock age = new AgeBlock();
        age.crcType = BlockHeader.CrcFieldType.CRC_16;

        CanonicalBlock scope = new HopCountBlock();
        scope.crcType = BlockHeader.CrcFieldType.CRC_16;

        CanonicalBlock payload = new PayloadBlock(testPayload);
        payload.crcType = BlockHeader.CrcFieldType.CRC_32;

        CanonicalBlock previous = new PreviousNodeBlock();
        previous.crcType = BlockHeader.CrcFieldType.CRC_32;

        bundle.addBlock(age);
        bundle.addBlock(scope);
        bundle.addBlock(payload);
        bundle.addBlock(previous);
        return bundle;
    }

    @Test
    public void testSimpleBundleSerialization() {
        System.out.println("[+] bundle: testing serialization and parsing with 6 test bundles");

        Bundle[] bundles = {
                testBundle1(),
                testBundle2(),
                testBundle3(),
                testBundle4(),
                testBundle5(),
                testBundle6()
        };

        Bundle[] res = {null};
        CborParser parser = CBOR.parser().cbor_parse_custom_item(
                () -> new BundleV7Item(
                        new SimpleLogger(),
                        new BaseExtensionToolbox(),
                        new BaseBlobFactory().setVolatileMaxSize(100000)),
                (p, t, item) ->
                        res[0] = item.bundle);

        Flowable
                .fromArray(bundles)
                .flatMap(bundle -> BundleV7Serializer
                        .encode(bundle, new BaseBlockDataSerializerFactory()).observe(10))
                .subscribe(
                        buf -> {
                            try {
                                while (buf.hasRemaining()) {
                                    if (parser.read(buf)) {
                                        // check payload
                                        checkBundlePayload(res[0]);
                                        parser.reset();
                                    }
                                }
                            } catch (RxParserException rpe) {
                                rpe.printStackTrace();
                                fail();
                            }
                        },
                        e -> {
                            System.out.println(e.getMessage());
                            e.printStackTrace();
                        });
    }


    /**
     * check that the payload of the bundle is correct.
     *
     * @param bundle to check
     */
    public static void checkBundlePayload(Bundle bundle) {
        // assert
        assertNotNull(bundle);
        String[] payload = {null};
        for (CanonicalBlock block : bundle.getBlocks()) {
            assertTrue(block.isTagged("crc_check"));
            assertTrue(block.<Boolean>getTagAttachment("crc_check"));
        }

        bundle.getPayloadBlock().data.observe().subscribe(
                buffer -> {
                    byte[] arr = new byte[buffer.remaining()];
                    buffer.get(arr);
                    payload[0] = new String(arr);
                });

        assertNotNull(payload[0]);
        assertEquals(testPayload, payload[0]);
    }
}
