package io.disruptedsystems.libdtn.common.data.bundlev7.parser;

import io.disruptedsystems.libdtn.common.data.security.CipherSuites;
import io.disruptedsystems.libdtn.common.data.security.IntegrityResult;
import io.disruptedsystems.libdtn.common.data.security.SecurityResult;
import io.disruptedsystems.libdtn.common.utils.Log;
import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborParser;

/**
 * SecurityResultItem is a CborParser.ParseableItem for a SecurityResult.
 *
 * @author Lucien Loiseau on 06/11/18.
 */
public class SecurityResultItem implements CborParser.ParseableItem {

    SecurityResultItem(int cipherId, int resultId, Log logger) {
        this.cipherId = cipherId;
        this.resultId = resultId;
        this.logger = logger;
    }

    public SecurityResult securityResult;

    private int cipherId;
    private int resultId;
    private Log logger;

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
                                logger.v(BundleV7Item.TAG, ".... result_id="
                                        + securityResult.getResultId());
                                logger.v(BundleV7Item.TAG, ".... result_value="
                                        + new String(chunk.array()));
                            },
                            (p) -> {
                            });


        }
        return CBOR.parser();
    }
}
