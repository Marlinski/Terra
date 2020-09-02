package io.disruptedsystems.libdtn.module.cla.stcp;

import io.disruptedsystems.libdtn.common.data.eid.ClaEidParser;
import io.disruptedsystems.libdtn.common.data.eid.EidFormatException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for the cla-specific part of a ClaEid.
 *
 * @author Lucien Loiseau on 28/11/18.
 */
public class ClaStcpEidParser implements ClaEidParser {

    public static final String STCP_PARAM_REGEX = "^([^:/?#]+):([0-9]+)$";
    public static final Matcher stcpMatcher = Pattern.compile(STCP_PARAM_REGEX).matcher("");

    @Override
    public ClaStcpEid createClaEid(String claName, String claSpecific, String demux)
            throws EidFormatException {
        stcpMatcher.reset(claSpecific);
        if (!stcpMatcher.matches()) {
            throw new EidFormatException("not an stcp-specific eid: " + claSpecific);
        }
        String host = stcpMatcher.group(1);
        int port = Integer.valueOf(stcpMatcher.group(2));
        return new ClaStcpEid(host, port, demux);
    }
}
