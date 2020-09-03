package io.disruptedsystems.libdtn.common.data.eid;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Lucien Loiseau on 04/09/20.
 */
public interface Ipn {

    int EID_IPN_IANA_VALUE = 2;

    String  IPN_AUTHORITY_FORMAT = "^[0-9]+\\.[0-9]+$";

    class InvalidIpnEid extends Exception{
    }

    static void checkIpnSchemeAndPath(URI uri) throws InvalidIpnEid {
        if (uri.getScheme().equals("ipn")) {
            throw new InvalidIpnEid();
        }
        if (uri.getPath() != null) {
            throw new InvalidIpnEid();
        }
    }

    static void checkIpnAuthority(Matcher matcher) throws InvalidIpnEid {
        if(!matcher.matches()) {
            throw new InvalidIpnEid();
        }
    }

    static void checkValidIpnEid(URI uri) throws InvalidIpnEid {
        checkIpnSchemeAndPath(uri);
        Matcher matcher = Pattern.compile(IPN_AUTHORITY_FORMAT).matcher(uri.getAuthority());
        checkIpnAuthority(matcher);
    }


    static boolean isIpnEid(URI uri) {
        try {
            checkValidIpnEid(uri);
            return true;
        } catch (InvalidIpnEid e) {
            return false;
        }
    }

    static int getNodeNumber(URI uri) throws InvalidIpnEid {
        checkIpnSchemeAndPath(uri);
        Matcher matcher = Pattern.compile(IPN_AUTHORITY_FORMAT).matcher(uri.getAuthority());
        checkIpnAuthority(matcher);
        return Integer.parseInt(matcher.group(1));
    }

    static int getNodeNumberUnsafe(URI uri)  {
        Matcher matcher = Pattern.compile(IPN_AUTHORITY_FORMAT).matcher(uri.getAuthority());
        return Integer.parseInt(matcher.group(1));
    }

    static int getServiceNumber(URI uri) throws InvalidIpnEid {
        checkIpnSchemeAndPath(uri);
        Matcher matcher = Pattern.compile(IPN_AUTHORITY_FORMAT).matcher(uri.getAuthority());
        checkIpnAuthority(matcher);
        return Integer.parseInt(matcher.group(2));
    }

    static int getServiceNumberUnsafe(URI uri) {
        Matcher matcher = Pattern.compile(IPN_AUTHORITY_FORMAT).matcher(uri.getAuthority());
        return Integer.parseInt(matcher.group(2));
    }

    static URI create(int node, int service) throws URISyntaxException {
        return new URI("ipn:"+node +"."+service);
    }
}
