package io.disruptedsystems.libdtn.common.data.eid;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Lucien Loiseau on 02/09/20.
 */
public class BaseDtnEidFactory implements EidSspParser {

    public static final String DTN_SSP_REGEXP = "^//([^/?#]*)?/(.*)?";
    public static final String CLA_NODE_REGEXP = "^\\[([^\\[\\]:]+):([^\\[\\]]+)\\]$";
    public static final Matcher dtnMatcher = Pattern.compile(DTN_SSP_REGEXP).matcher("");
    public static final Matcher claMatcher = Pattern.compile(CLA_NODE_REGEXP).matcher("");

    private ClaEidParser claEidParser;

    public BaseDtnEidFactory() {
        this(new BaseClaEidFactory());
    }

    public BaseDtnEidFactory(ClaEidParser claEidParser) {
        this.claEidParser = claEidParser;
    }

    @Override
    public DtnEid create(String ssp) throws EidFormatException {
        if (ssp.equals("none")) {
            return DtnEid.nullEid();
        }

        dtnMatcher.reset(ssp);
        if (!dtnMatcher.matches()) {
            throw new EidFormatException("not a dtn EID");
        }
        String nodeName = dtnMatcher.group(1);
        String demux = dtnMatcher.group(2);
        return create(nodeName, demux);
    }

    public DtnEid create(String nodeName, String demux) throws EidFormatException {
        try {
            if (nodeName.equals("api:me")) {
                return new ApiEid(demux);
            }
            if (claMatcher.reset(nodeName).matches()) {
                return claEidParser.createClaEid(claMatcher.group(1), claMatcher.group(2), demux);
            }
        } catch(EidFormatException e) {
            // ignore
        }
        return new BaseDtnEid(nodeName, demux);
    }
}
