package io.disruptedsystems.libdtn.common.data.bundlev7.parser;

import io.disruptedsystems.libdtn.common.data.security.AbstractSecurityBlock;
import io.disruptedsystems.libdtn.common.data.security.SecurityBlock;
import io.disruptedsystems.libdtn.common.utils.Log;
import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborParser;

import java.util.LinkedList;

/**
 * SecurityBlockParser parses the data-specific part of an {@link SecurityBlock}.
 *
 * @author Lucien Loiseau on 04/11/18.
 */
public class SecurityBlockParser {

    static CborParser getParser(AbstractSecurityBlock block, Log logger) {
        return CBOR.parser()
                .cbor_open_array(5)
                .cbor_parse_linear_array(
                        CBOR.IntegerItem::new,
                        (p, t, i) -> {
                            logger.v(BundleV7Item.TAG, ".. nb_of_targets=" + i);
                        },
                        (p, t, item) -> {
                            logger.v(BundleV7Item.TAG, ".. target=" + item.value());
                            block.securityTargets.add((int) item.value());
                        },
                        (p, t, a) -> {
                        })
                .cbor_parse_int((p, t, i) -> {
                    logger.v(BundleV7Item.TAG, ".. cipherSuiteId=" + i);
                    block.cipherSuiteId = (int) i;
                })
                .cbor_parse_int((p, t, i) -> {
                    logger.v(BundleV7Item.TAG, ".. securityBlockFlag=" + i);
                    block.securityBlockFlag = (int) i;
                })
                .do_insert_if(
                        (p) -> block.getSaFlag(
                                SecurityBlock.SecurityBlockFlags.SECURITY_SOURCE_PRESENT),
                        CBOR.parser().cbor_parse_custom_item(
                                () -> new EidItem(logger),
                                (p, t, item) -> {
                                    logger.v(BundleV7Item.TAG, ".. securitySource="
                                            + item.eid.toString());
                                    block.securitySource = item.eid;
                                }))
                .cbor_parse_linear_array(
                        (pos1) ->   /* array in array */
                                () -> {
                                    logger.v(BundleV7Item.TAG, "... target="
                                            + block.securityResults.size());
                                    block.securityResults.add(new LinkedList<>());
                                    return CBOR.parser()
                                            .cbor_parse_linear_array(
                                                    (pos2) -> new SecurityResultItem(
                                                            block.cipherSuiteId,
                                                            block.securityResults.getLast().size(),
                                                            logger),
                                                    (p, t, size) ->
                                                            logger.v(BundleV7Item.TAG,
                                                                    "... target_results="
                                                                            + size),
                                                    (p, t, item) -> {
                                                        block.securityResults.getLast()
                                                                .add(item.securityResult);
                                                    },
                                                    (p, t, s) -> {
                                                    });
                                },
                        (p, t, size) -> logger.v(BundleV7Item.TAG, ".. security_results=" + size),
                        (p, t, item) -> {
                        },
                        (p, t, a) -> {
                        });
    }

}
