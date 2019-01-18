package io.left.rightmesh.libdtn.common.data.bundlev7;

import org.junit.Test;

import io.left.rightmesh.libcbor.CBOR;
import io.left.rightmesh.libcbor.CborEncoder;
import io.left.rightmesh.libcbor.CborParser;
import io.left.rightmesh.libcbor.rxparser.RxParserException;
import io.left.rightmesh.libdtn.common.BaseExtensionToolbox;
import io.left.rightmesh.libdtn.common.data.AgeBlock;
import io.left.rightmesh.libdtn.common.data.BundleId;
import io.left.rightmesh.libdtn.common.data.CanonicalBlock;
import io.left.rightmesh.libdtn.common.data.BlockHeader;
import io.left.rightmesh.libdtn.common.data.Bundle;
import io.left.rightmesh.libdtn.common.data.blob.BaseBlobFactory;
import io.left.rightmesh.libdtn.common.data.bundlev7.parser.BundleV7Item;
import io.left.rightmesh.libdtn.common.data.bundlev7.serializer.BaseBlockDataSerializerFactory;
import io.left.rightmesh.libdtn.common.data.bundlev7.serializer.BundleV7Serializer;
import io.left.rightmesh.libdtn.common.data.eid.DtnEid;
import io.left.rightmesh.libdtn.common.data.PayloadBlock;
import io.left.rightmesh.libdtn.common.data.PreviousNodeBlock;
import io.left.rightmesh.libdtn.common.data.PrimaryBlock;
import io.left.rightmesh.libdtn.common.data.ScopeControlHopLimitBlock;
import io.left.rightmesh.libdtn.common.data.eid.EidIpn;
import io.left.rightmesh.libdtn.common.utils.NullLogger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Lucien Loiseau on 20/09/18.
 */
public class BundleV7Test {

    public static String testPayload = "This is a test for bundle serialization";

    public static Bundle testBundle0() {
        Bundle bundle = new Bundle();
        bundle.setDestination(new EidIpn(5, 12));
        bundle.setSource(DtnEid.unsafe("source"));
        bundle.setReportto(DtnEid.nullEid());
        bundle.bid = BundleId.create(bundle);
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
        bundle.addBlock(new PreviousNodeBlock(DtnEid.generate()));
        return bundle;
    }


    public static Bundle testBundle5() {
        Bundle bundle = testBundle1();
        bundle.addBlock(new AgeBlock());
        bundle.addBlock(new ScopeControlHopLimitBlock());
        bundle.addBlock(new PreviousNodeBlock(DtnEid.generate()));
        bundle.setCrcType(PrimaryBlock.CrcFieldType.CRC_32);
        return bundle;
    }

    public static Bundle testBundle6() {
        Bundle bundle = testBundle0();
        bundle.setCrcType(PrimaryBlock.CrcFieldType.CRC_32);

        CanonicalBlock age = new AgeBlock();
        CanonicalBlock scope = new ScopeControlHopLimitBlock();
        CanonicalBlock payload = new PayloadBlock(testPayload);
        CanonicalBlock previous = new PreviousNodeBlock();

        age.crcType = BlockHeader.CrcFieldType.CRC_16;
        scope.crcType = BlockHeader.CrcFieldType.CRC_16;
        payload.crcType = BlockHeader.CrcFieldType.CRC_32;
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

        for (Bundle bundle : bundles) {
            Bundle[] res = {null};

            // prepare serializer
            CborEncoder enc = BundleV7Serializer.encode(bundle,
                    new BaseBlockDataSerializerFactory());

            // prepare parser
            BundleV7Item bundleParser = new BundleV7Item(
                    new NullLogger(),
                    new BaseExtensionToolbox(),
                    new BaseBlobFactory().enableVolatile(100000).disablePersistent());

            CborParser p = CBOR.parser().cbor_parse_custom_item(
                    () -> new BundleV7Item(
                            new NullLogger(),
                            new BaseExtensionToolbox(),
                            new BaseBlobFactory().enableVolatile(100000).disablePersistent()),
                    (__, ___, item) ->
                            res[0] = item.bundle);

            // serialize and parse
            enc.observe(10).subscribe(
                    buf -> {
                        try {
                            if (p.read(buf)) {
                                assertEquals(false, buf.hasRemaining());
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

            // check payload
            checkBundlePayload(res[0]);
        }
    }


    public static void checkBundlePayload(Bundle bundle) {
        // assert
        assertEquals(true, bundle != null);
        String[] payload = {null};
        if (bundle != null) {
            for (CanonicalBlock block : bundle.getBlocks()) {
                assertEquals(true, block.isTagged("crc_check"));
                assertEquals(true, block.<Boolean>getTagAttachment("crc_check"));
            }

            bundle.getPayloadBlock().data.observe().subscribe(
                    buffer -> {
                        byte[] arr = new byte[buffer.remaining()];
                        buffer.get(arr);
                        payload[0] = new String(arr);
                    });

            assertEquals(true, payload[0] != null);
            assertEquals(testPayload, payload[0]);
        }
    }
}