package io.disruptedsystems.libdtn.common.data.bundlev7.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.disruptedsystems.libdtn.common.data.security.AbstractSecurityBlock;
import io.disruptedsystems.libdtn.common.data.security.SecurityBlock;

import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborParser;

import java.util.LinkedList;

/**
 * SecurityBlockParser parses the data-specific part of an {@link SecurityBlock}.
 *
 * @author Lucien Loiseau on 04/11/18.
 */
public class SecurityBlockParser {

    private static final Logger log = LoggerFactory.getLogger(SecurityBlockParser.class);

    static CborParser getParser(AbstractSecurityBlock block) {
        return CBOR.parser()
                .cbor_open_array(5)
                .cbor_parse_linear_array(
                        CBOR.IntegerItem::new,
                        (p, t, i) -> {
                            log.trace(".. nb_of_targets=" + i);
                        },
                        (p, t, item) -> {
                            log.trace(".. target=" + item.value());
                            block.securityTargets.add((int) item.value());
                        },
                        (p, t, a) -> {
                        })
                .cbor_parse_int((p, t, i) -> {
                    log.trace(".. cipherSuiteId=" + i);
                    block.cipherSuiteId = (int) i;
                })
                .cbor_parse_int((p, t, i) -> {
                    log.trace(".. securityBlockFlag=" + i);
                    block.securityBlockFlag = (int) i;
                })
                .do_insert_if(
                        (p) -> block.getSaFlag(
                                SecurityBlock.SecurityBlockFlags.SECURITY_SOURCE_PRESENT),
                        CBOR.parser().cbor_parse_custom_item(
                                EidItem::new,
                                (p, t, item) -> {
                                    log.trace(".. securitySource="
                                            + item.eid.toString());
                                    block.securitySource = item.eid;
                                }))
                .cbor_parse_linear_array(
                        (pos1) ->   /* array in array */
                                () -> {
                                    log.trace("... target="
                                            + block.securityResults.size());
                                    block.securityResults.add(new LinkedList<>());
                                    return CBOR.parser()
                                            .cbor_parse_linear_array(
                                                    (pos2) -> new SecurityResultItem(
                                                            block.cipherSuiteId,
                                                            block.securityResults.getLast().size()),
                                                    (p, t, size) ->
                                                            log.trace("... target_results="
                                                                            + size),
                                                    (p, t, item) -> {
                                                        block.securityResults.getLast()
                                                                .add(item.securityResult);
                                                    },
                                                    (p, t, s) -> {
                                                    });
                                },
                        (p, t, size) -> log.trace(".. security_results=" + size),
                        (p, t, item) -> {
                        },
                        (p, t, a) -> {
                        });
    }

}
