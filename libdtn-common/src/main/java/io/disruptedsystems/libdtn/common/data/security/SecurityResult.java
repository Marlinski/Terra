package io.disruptedsystems.libdtn.common.data.security;

import io.marlinski.libcbor.CborEncoder;
import io.marlinski.libcbor.CborParser;

/**
 * SecurityResult holds the result of a SecurityBlock.
 *
 * @author Lucien Loiseau on 03/11/18.
 */
public interface SecurityResult {

    int getResultId();

    CborEncoder getValueEncoder();

}
