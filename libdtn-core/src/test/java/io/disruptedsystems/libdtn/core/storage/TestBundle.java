package io.disruptedsystems.libdtn.core.storage;

import io.disruptedsystems.libdtn.common.data.AgeBlock;
import io.disruptedsystems.libdtn.common.data.BlockHeader;
import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.BundleId;
import io.disruptedsystems.libdtn.common.data.CanonicalBlock;
import io.disruptedsystems.libdtn.common.data.PayloadBlock;
import io.disruptedsystems.libdtn.common.data.PreviousNodeBlock;
import io.disruptedsystems.libdtn.common.data.PrimaryBlock;
import io.disruptedsystems.libdtn.common.data.ScopeControlHopLimitBlock;
import io.disruptedsystems.libdtn.common.data.eid.BaseDtnEid;
import io.disruptedsystems.libdtn.common.data.eid.DtnEid;
import io.disruptedsystems.libdtn.common.data.eid.EidFormatException;
import io.disruptedsystems.libdtn.common.data.eid.IpnEid;

import static org.junit.Assert.assertEquals;

/**
 * @author Lucien Loiseau on 21/10/18.
 */
public class TestBundle {

    public static String testPayload = "This is a test for bundle serialization";

    private static DtnEid createSourceEid() {
        try {
            return new BaseDtnEid("source");
        } catch (EidFormatException e) {
            return DtnEid.nullEid();
        }
    }

    public static Bundle testBundle0() {
        Bundle bundle = new Bundle();
        bundle.setDestination(new IpnEid(5, 12));
        bundle.setSource(createSourceEid());
        bundle.setReportto(DtnEid.nullEid());
        bundle.bid = BundleId.create(bundle);
        return bundle;
    }

    public static Bundle testBundle1() {
        Bundle bundle = testBundle0();
        bundle.addBlock(new PayloadBlock(new String(testPayload)));
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
