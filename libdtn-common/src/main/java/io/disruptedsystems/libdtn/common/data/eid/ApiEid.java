package io.disruptedsystems.libdtn.common.data.eid;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ApiEid are a class of Eid used for "api" related services. For instance api:me always refer
 * to the local eid configured by the current node administrator.
 *
 * @author Lucien Loiseau on 29/10/18.
 */
public class ApiEid extends BaseEid {

    public static final int EID_API_IANA_VALUE = 251;  // not actually an ianaNumber value
    public static final String EID_API_SCHEME = "api";  // n

    private String sink;

    private ApiEid() {
        this.sink = "";
    }

    public static ApiEid me() {
        return new ApiEid();
    }

    /**
     * Scheme-specific parser for the "api" scheme.
     */
    public static class ApiParser implements EidSspParser {
        @Override
        public Eid create(String ssp) throws EidFormatException {
            final String regex = "me(/.*)?";
            Pattern r = Pattern.compile(regex);
            Matcher m = r.matcher(ssp);
            if (m.find()) {
                String sink = m.group(1) == null ? "" : m.group(1);
                return new ApiEid(sink);
            } else {
                throw new EidFormatException("not an ApiEid");
            }
        }
    }

    /**
     * Constructor. Creates an ApiEid after parsing the api-specific part.
     *
     * @param sink sink
     * @throws EidFormatException if the api-specific part is not recognized.
     */
    public ApiEid(String sink) throws EidFormatException {
        if(sink == null) {
            sink = "";
        }
        this.sink = sink.startsWith("/") ? sink.replaceFirst("/","") : sink;
        checkValidity();
    }

    @Override
    public Eid copy() {
        ApiEid me = me();
        me.sink = sink;
        return me;
    }

    @Override
    public int ianaNumber() {
        return EID_API_IANA_VALUE;
    }

    @Override
    public String getScheme() {
        return EID_API_SCHEME;
    }

    @Override
    public String getSsp() {
        return "me" + (sink.equals("") ? "" : "/" + sink);
    }

    public String getSink() {
        return sink;
    }

    public void setSink(String sink) throws EidFormatException {
        this.sink = sink;
        checkValidity();
    }

    @Override
    public boolean matches(Eid other) {
        if (other == null) {
            return false;
        }
        if (other instanceof ApiEid) {
            return true;
        }
        return false;
    }

}
