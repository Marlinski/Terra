package io.disruptedsystems.libdtn.module.cla.bows;

import io.disruptedsystems.libdtn.common.data.eid.ClaEidParser;
import io.disruptedsystems.libdtn.common.data.eid.EidFormatException;

/**
 * Parser for the cla-specific part of a ClaEid.
 *
 * @author Lucien Loiseau on 28/11/18.
 */
public class ClaBowsEidParser implements ClaEidParser {
    @Override
    public ClaBowsEid createClaEid(String claName, String claSpecific, String demux)
            throws EidFormatException {
        return new ClaBowsEid(claSpecific, demux);
    }
}
