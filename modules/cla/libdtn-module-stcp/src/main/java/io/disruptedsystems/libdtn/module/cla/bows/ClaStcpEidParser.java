package io.disruptedsystems.libdtn.module.cla.bows;

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

    @Override
    public ClaStcpEid createClaEid(String claName, String claSpecific)
            throws EidFormatException {
        final String regex = "^([^:/?#]+):([0-9]+)(/.*)?";
        Pattern r = Pattern.compile(regex);
        Matcher m = r.matcher(claSpecific);
        if (!m.find()) {
            throw new EidFormatException("not an stcp-specific eid: " + claSpecific);
        }
        String host = m.group(1);
        int port = Integer.valueOf(m.group(2));
        String sink = m.group(3);
        return new ClaStcpEid(host, port, sink);
    }
}
