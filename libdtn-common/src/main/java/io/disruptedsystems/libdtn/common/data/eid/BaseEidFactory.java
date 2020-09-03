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

    private static final Matcher rfc3986 = Pattern
            .compile(RFC3986_URI_REGEXP)
            .matcher("");

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
        rfc3986.reset(str);
        if (rfc3986.matches()) {
            scheme = rfc3986.group(2);
            String slashedAuthority = rfc3986.group(3) == null ? "" : rfc3986.group(3);
            String authority = rfc3986.group(4) == null ? "" : rfc3986.group(4);
            String path = rfc3986.group(5) == null ? "" : rfc3986.group(5);
            String undef = rfc3986.group(6) == null ? "" : rfc3986.group(6);
            String query = rfc3986.group(7) == null ? "" : rfc3986.group(7);
            String related = rfc3986.group(8) == null ? "" : rfc3986.group(8);
            String fragment = rfc3986.group(9) == null ? "" : rfc3986.group(9);
            ssp = slashedAuthority + path + undef + related;
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
