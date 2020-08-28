package io.disruptedsystems.libdtn.common.data.security;

import io.marlinski.libcbor.CborEncoder;
import io.marlinski.libcbor.CborParser;

/**
 * SecurityAssociationParameter.
 *
 * @author Lucien Loiseau on 03/11/18.
 */
public interface SecurityAssociationParameter {

    int getParameterId();

    CborEncoder getValueEncoder();

    CborParser  getValueParser();
}
