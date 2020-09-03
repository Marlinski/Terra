package io.disruptedsystems.libdtn.common.data.eid;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.disruptedsystems.libdtn.common.data.eid.ClaEid.CLA_NODE_REGEXP;
import static io.disruptedsystems.libdtn.common.data.eid.Eid.RFC3986_URI_REGEXP_SSP;

/**
 * @author Lucien Loiseau on 02/09/20.
 */
public class BaseDtnEidFactory implements EidSspParser {

    public static final Matcher sspMatcher = Pattern.compile(RFC3986_URI_REGEXP_SSP).matcher("");
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

        sspMatcher.reset(ssp);
        if (!sspMatcher.matches()) {
            throw new EidFormatException("not a dtn EID");
        }
        String nodeName = sspMatcher.group(1);
        String demux = sspMatcher.group(2);
        return create(nodeName, demux);
    }

    private DtnEid create(String nodeName, String demux) throws EidFormatException {
        try {
            if (nodeName.equals("api:me")) {
                return new ApiEid(demux);
            }
            if (claMatcher.reset(nodeName).matches()) {
                return claEidParser.createClaEid(claMatcher.group(1), claMatcher.group(2), demux);
            }
        } catch(EidFormatException e) {
            e.printStackTrace();
            // ignore
        }
        return new BaseDtnEid(nodeName, demux);
    }
}
