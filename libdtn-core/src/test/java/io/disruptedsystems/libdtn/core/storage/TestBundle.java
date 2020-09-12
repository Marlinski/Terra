package io.disruptedsystems.libdtn.core.storage;

import java.net.URI;

import io.disruptedsystems.libdtn.common.data.AgeBlock;
import io.disruptedsystems.libdtn.common.data.BlockHeader;
import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.CanonicalBlock;
import io.disruptedsystems.libdtn.common.data.PayloadBlock;
import io.disruptedsystems.libdtn.common.data.PreviousNodeBlock;
import io.disruptedsystems.libdtn.common.data.PrimaryBlock;
import io.disruptedsystems.libdtn.common.data.HopCountBlock;
import io.disruptedsystems.libdtn.common.data.eid.Dtn;

import static org.junit.Assert.assertEquals;

/**
 * @author Lucien Loiseau on 21/10/18.
 */
public class TestBundle {

    public static String testPayload = "This is a test for bundle serialization";

    public static Bundle testBundle0() {
        Bundle bundle = new Bundle();
        bundle.setDestination(URI.create("ipn:5.12"));
        bundle.setSource(URI.create("dtn://source/"));
        bundle.setReportTo(Dtn.nullEid());
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
        bundle.addBlock(new HopCountBlock());
        return bundle;
    }

    public static Bundle testBundle4() {
        Bundle bundle = testBundle1();
        bundle.addBlock(new AgeBlock());
        bundle.addBlock(new HopCountBlock());
        bundle.addBlock(new PreviousNodeBlock(Dtn.generate()));
        return bundle;
    }


    public static Bundle testBundle5() {
        Bundle bundle = testBundle1();
        bundle.addBlock(new AgeBlock());
        bundle.addBlock(new HopCountBlock());
        bundle.addBlock(new PreviousNodeBlock(Dtn.generate()));
        bundle.setCrcType(PrimaryBlock.CrcFieldType.CRC_32);
        return bundle;
    }

    public static Bundle testBundle6() {
        Bundle bundle = testBundle0();
        bundle.setCrcType(PrimaryBlock.CrcFieldType.CRC_32);

        CanonicalBlock age = new AgeBlock();
        CanonicalBlock scope = new HopCountBlock();
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
            if (payload[0] != null) {
                assertEquals(testPayload, payload[0]);
            }
        }
    }


}
