package io.disruptedsystems.libdtn.common.data.eid;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IpnEid is a class of Eid whose scheme is "ipn". it identifies both endpoint node and
 * endpoint service with an integer.
 *
 * @author Lucien Loiseau on 17/10/18.
 */
public class IpnEid extends BaseEid {

    public static final int EID_IPN_IANA_VALUE = 2;
    public static final String EID_IPN_SCHEME = "ipn";

    public int nodeNumber;
    public int serviceNumber;

    /**
     * Class to create a new IpnEid after parsing the ipn specific part.
     */
    public static class IpnParser implements EidSspParser {
        @Override
        public Eid create(String ssp) throws EidFormatException {
            final String regex = "^([0-9]+)\\.([0-9]+)";
            Pattern r = Pattern.compile(regex);
            Matcher m = r.matcher(ssp);
            if (m.find()) {
                String node = m.group(1);
                String service = m.group(2);
                int nodeNumber = Integer.valueOf(node);
                int serviceNumber = Integer.valueOf(service);
                return new IpnEid(nodeNumber, serviceNumber);
            } else {
                throw new EidFormatException("not an EidIpn");
            }
        }
    }


    public IpnEid(IpnEid o) {
        this.nodeNumber = o.nodeNumber;
        this.serviceNumber = o.serviceNumber;
    }

    public IpnEid(int node, int service) {
        this.nodeNumber = node;
        this.serviceNumber = service;
    }

    public int getNodeNumber() {
        return nodeNumber;
    }

    public int getServiceNumber() {
        return serviceNumber;
    }

    @Override
    public IpnEid copy() {
        return new IpnEid(this);
    }

    @Override
    public int ianaNumber() {
        return EID_IPN_IANA_VALUE;
    }

    @Override
    public String getScheme() {
        return EID_IPN_SCHEME;
    }

    @Override
    public String getSsp() {
        return nodeNumber + "." + serviceNumber;
    }

    @Override
    public boolean isAuthoritativeEid() {
        return serviceNumber == 0;
    }

    @Override
    public boolean isAuthoritativeOver(Eid other) {
        return isAuthoritativeEid()
                && (other instanceof IpnEid)
                && nodeNumber == ((IpnEid) other).nodeNumber;
    }
}