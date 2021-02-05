package io.disruptedsystems.libdtn.common.data.bundlev7.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.disruptedsystems.libdtn.common.data.security.CipherSuites;
import io.disruptedsystems.libdtn.common.data.security.IntegrityResult;
import io.disruptedsystems.libdtn.common.data.security.SecurityResult;

import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborParser;

/**
 * SecurityResultItem is a CborParser.ParseableItem for a SecurityResult.
 *
 * @author Lucien Loiseau on 06/11/18.
 */
public class SecurityResultItem implements CborParser.ParseableItem {

    private static final Logger log = LoggerFactory.getLogger(SecurityResultItem.class);

    SecurityResultItem(int cipherId, int resultId) {
        this.cipherId = cipherId;
        this.resultId = resultId;
    }

    public SecurityResult securityResult;

    private final int cipherId;
    private int resultId;

    @Override
    public CborParser getItemParser() {
        if (cipherId == CipherSuites.BIB_SHA256.getId()) {
            return CBOR.parser()
                    .cbor_parse_byte_string(
                            (p, t, size) -> {
                                /* size of the checksum */
                            },
                            (p, chunk) -> {
                                securityResult = new IntegrityResult(chunk.array());
                                log.trace(".... result_id="
                                        + securityResult.getResultId());
                                log.trace(".... result_value="
                                        + new String(chunk.array()));
                            },
                            (p) -> {
                            });


        }
        return CBOR.parser();
    }
}
