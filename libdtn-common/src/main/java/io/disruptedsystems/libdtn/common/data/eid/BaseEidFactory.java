package io.disruptedsystems.libdtn.common.data.eid;

import static io.disruptedsystems.libdtn.common.data.eid.Eid.RFC3986_URI_REGEXP;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BaseEidFactory implements EidFactory and provides a factory for all the basic Eid scheme,
 * namely "api", "dtn" and "ipn".
 *
 * @author Lucien Loiseau on 28/11/18.
 */
public class BaseEidFactory implements EidFactory {

    private EidSspParser ipnParser = new IpnEid.IpnParser();
    private EidSspParser dtnParser;

    public BaseEidFactory() {
        this(new BaseDtnEidFactory());
    }

    public BaseEidFactory(EidSspParser dtnParser) {
        this.dtnParser = dtnParser;
    }

    @Override
    public String getIanaScheme(int ianaScheme) throws UnknownIanaNumber {
        switch (ianaScheme) {
            case BaseDtnEid.EID_DTN_IANA_VALUE:
                return BaseDtnEid.EID_DTN_SCHEME;
            case IpnEid.EID_IPN_IANA_VALUE:
                return IpnEid.EID_IPN_SCHEME;
            default:
                throw new UnknownIanaNumber(ianaScheme);
        }
    }

    @Override
    public Eid create(String str) throws EidFormatException {
        String scheme;
        String ssp;
        Pattern r = Pattern.compile(RFC3986_URI_REGEXP);
        Matcher m = r.matcher(str);
        if (m.find()) {
            scheme = m.group(2);
            String slashedAuthority = m.group(3) == null ? "" : m.group(3);
            String authority = m.group(4) == null ? "" : m.group(4);
            String path = m.group(5) == null ? "" : m.group(5);
            String undef = m.group(6) == null ? "" : m.group(6);
            String query = m.group(7) == null ? "" : m.group(7);
            String related = m.group(8) == null ? "" : m.group(8);
            String fragment = m.group(9) == null ? "" : m.group(9);
            ssp = slashedAuthority + path + undef + query + related;
        } else {
            throw new EidFormatException("not a URI");
        }
        return create(scheme, ssp);
    }

    @Override
    public Eid create(String scheme, String ssp) throws EidFormatException {
        if (scheme.equals(IpnEid.EID_IPN_SCHEME)) {
            return ipnParser.create(ssp);
        }
        if (scheme.equals(BaseDtnEid.EID_DTN_SCHEME)) {
            return dtnParser.create(ssp);
        }
        throw new UnknownEidScheme(scheme);
    }
}
