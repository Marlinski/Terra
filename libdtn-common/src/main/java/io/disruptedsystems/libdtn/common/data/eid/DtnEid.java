package io.disruptedsystems.libdtn.common.data.eid;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DtnEid is a class of Eid whose scheme is "dtn".
 *
 * @author Lucien Loiseau on 17/10/18.
 */
public class DtnEid extends BaseEid {

    public static final int EID_DTN_IANA_VALUE = 1;
    public static final String EID_DTN_SCHEME = "dtn";

    private String ssp = "";
    private String nodeName = "";
    private String demux = "";
    private boolean singleton = false;

    private DtnEid() {
    }

    /**
     * generate a random and valid DtnEid.
     *
     * @return a new DtnEid.
     */
    public static DtnEid generate() {
        DtnEid ret = new DtnEid();
        final String uuid = UUID.randomUUID().toString().replace("-", "");
        ret.ssp = "//" + uuid + "/";
        ret.singleton = false;
        return ret;
    }

    /**
     * returns a NULL Endpoint ID.
     *
     * @return Eid
     */
    public static DtnEid nullEid() {
        DtnEid ret = new DtnEid();
        ret.ssp = "none";
        return ret;
    }

    /**
     * Class to create a new DtnEid after parsing the dtn specific part.
     */
    public static class DtnParser implements EidSspParser {
        @Override
        public Eid create(String ssp) throws EidFormatException {
            return new DtnEid(ssp);
        }
    }

    /**
     * safe constructor that creates a DtnEid from a dtn specific part and checks for validity.
     *
     * @param ssp dtn scheme specific part.
     * @throws EidFormatException if the eid is invalid.
     */
    public DtnEid(String ssp) throws EidFormatException {
        if (ssp.equals("none")) {
            this.ssp = ssp;
            return;
        }

        final String regex = "^//([\\p{Alnum}\\.\\_\\-\\~\\+]+)/([\\p{Graph}\\p{Punct}]*)$";
        Pattern r = Pattern.compile(regex);
        Matcher m = r.matcher(ssp);
        if (m.find()) {
            this.nodeName = m.group(1);
            this.demux = m.group(2);
            this.ssp = ssp;
            this.singleton = !demux.startsWith("~");
        } else {
            throw new EidFormatException("not a dtn EID");
        }
        checkValidity();
    }

    @Override
    public Eid copy() {
        DtnEid ret = new DtnEid();
        ret.ssp = ssp;
        ret.nodeName = nodeName;
        ret.demux = demux;
        ret.singleton = singleton;
        return ret;
    }

    @Override
    public int ianaNumber() {
        return EID_DTN_IANA_VALUE;
    }

    @Override
    public String getScheme() {
        return EID_DTN_SCHEME;
    }

    @Override
    public String getSsp() {
        return ssp;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getDemux() {
        return demux;
    }

    public boolean isNullEndPoint() {
        return ssp.equals("none");
    }

    public boolean isSingleton() {
        return singleton;
    }

    @Override
    public boolean matches(Eid other) {
        if (other == null) {
            return false;
        }
        if (other instanceof DtnEid) {
            return ssp.startsWith(((DtnEid) other).ssp);
        }
        return false;
    }

}