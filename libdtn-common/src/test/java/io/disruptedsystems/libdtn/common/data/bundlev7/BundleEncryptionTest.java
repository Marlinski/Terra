package io.disruptedsystems.libdtn.common.data.bundlev7;

import static io.disruptedsystems.libdtn.common.data.bundlev7.BundleV7Test.checkBundlePayload;
import static io.disruptedsystems.libdtn.common.data.bundlev7.BundleV7Test.testBundle1;
import static io.disruptedsystems.libdtn.common.data.bundlev7.BundleV7Test.testBundle2;
import static io.disruptedsystems.libdtn.common.data.bundlev7.BundleV7Test.testBundle3;
import static io.disruptedsystems.libdtn.common.data.bundlev7.BundleV7Test.testBundle4;
import static io.disruptedsystems.libdtn.common.data.bundlev7.BundleV7Test.testBundle5;
import static io.disruptedsystems.libdtn.common.data.bundlev7.BundleV7Test.testBundle6;
import static io.disruptedsystems.libdtn.common.data.security.CipherSuites.BCB_AES128_CBC_PKCS5;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import io.disruptedsystems.libdtn.common.utils.NullLogger;
import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborEncoder;
import io.marlinski.libcbor.CborParser;
import io.marlinski.libcbor.rxparser.RxParserException;
import io.disruptedsystems.libdtn.common.BaseExtensionToolbox;
import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.CanonicalBlock;
import io.disruptedsystems.libdtn.common.data.blob.BaseBlobFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.parser.BundleV7Item;
import io.disruptedsystems.libdtn.common.data.bundlev7.serializer.BaseBlockDataSerializerFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.serializer.BundleV7Serializer;
import io.disruptedsystems.libdtn.common.data.security.BlockConfidentialityBlock;
import io.disruptedsystems.libdtn.common.data.security.SecurityBlock;
import io.disruptedsystems.libdtn.common.data.security.SecurityContext;
import io.disruptedsystems.libdtn.common.utils.Log;

import org.junit.Test;

/**
 * Test class to test Bundle Encryption Block.
 *
 * @author Lucien Loiseau on 06/11/18.
 */
public class BundleEncryptionTest {

    @Test
    public void testSimpleBundleEncryption() {
        System.out.println("[+] bundle: testing encryption");
        Log logger = new NullLogger();
        Bundle[] bundles = {
                testBundle1(),
                testBundle2(),
                testBundle3(),
                testBundle4(),
                testBundle5(),
                testBundle6()
        };

        /* create security context */
        SecurityContext context = SecurityContextTest.mockSecurityContext();

        for (Bundle b : bundles) {
            /* encrypt the payload of the bundles */
            BlockConfidentialityBlock bcb = new BlockConfidentialityBlock();
            bcb.addTarget(0);
            bcb.setCipherSuite(BCB_AES128_CBC_PKCS5);

            try {
                // offer confidentiality block
                bcb.addTo(b);
            } catch (SecurityBlock.NoSuchBlockException foe) {
                fail();
            }

            try {
                // perform confidentiality
                bcb.applyTo(b, context, new BaseBlockDataSerializerFactory(), logger);
            } catch (SecurityBlock.SecurityOperationException soe) {
                System.out.println(soe.getMessage());
                soe.printStackTrace();
                fail();
            }
        }

        for (Bundle bundle : bundles) {
            Bundle[] res = {null};

            // prepare serializer
            CborEncoder enc = BundleV7Serializer.encode(bundle,
                    new BaseBlockDataSerializerFactory());

            // prepare parser
            CborParser parser = CBOR.parser().cbor_parse_custom_item(
                    () -> new BundleV7Item(
                            logger,
                            new BaseExtensionToolbox(),
                            new BaseBlobFactory().setVolatileMaxSize(100000)),
                    (p, t, item) ->
                            res[0] = item.bundle);

            // serialize and parse
            enc.observe(10).subscribe(
                    buf -> {
                        try {
                            if (parser.read(buf)) {
                                assertEquals(false, buf.hasRemaining());
                            }
                        } catch (RxParserException rpe) {
                            rpe.printStackTrace();
                        }
                    },
                    e -> {
                        System.out.println(e.getMessage());
                        e.printStackTrace();
                    });

            logger.v("BundleEncryptionTest", " > decryption <");
            for (CanonicalBlock block : res[0].blocks) {
                if (block.type == BlockConfidentialityBlock.BLOCK_CONFIDENTIALITY_BLOCK_TYPE) {
                    try {
                        ((BlockConfidentialityBlock) block).applyFrom(
                                res[0],
                                context,
                                new BaseExtensionToolbox(),
                                logger);
                    } catch (SecurityBlock.SecurityOperationException e) {
                        e.printStackTrace();
                        fail();
                    }
                }
            }

            // check the payload
            checkBundlePayload(res[0]);
        }

    }

}
