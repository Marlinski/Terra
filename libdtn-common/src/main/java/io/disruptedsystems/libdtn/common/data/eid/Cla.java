package io.disruptedsystems.libdtn.common.data.eid;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.disruptedsystems.libdtn.common.data.eid.Dtn.checkValidDtnEid;

/**
 * @author Lucien Loiseau on 04/09/20.
 */
public interface Cla {

    String CLA_AUTHORITY_FORMAT = "^@([^:])+(:.+)?";

    class InvalidClaEid extends Exception {
    }

    static void checkValidClaEid(URI uri) throws Dtn.InvalidDtnEid, InvalidClaEid {
        checkValidDtnEid(uri);
        if (!uri.getAuthority().startsWith("@")) {
            throw new InvalidClaEid();
        }
    }

    static void checkClaAuthority(Matcher matcher) throws InvalidClaEid {
        if (!matcher.matches()) {
            throw new InvalidClaEid();
        }
    }

    static boolean isClaEid(URI uri) {
        try {
            checkValidClaEid(uri);
            return true;
        } catch (InvalidClaEid | Dtn.InvalidDtnEid e) {
            return false;
        }
    }

    static String getClaScheme(URI uri) throws Dtn.InvalidDtnEid, InvalidClaEid {
        checkValidClaEid(uri);
        Matcher matcher = Pattern.compile(CLA_AUTHORITY_FORMAT).matcher(uri.getAuthority());
        checkClaAuthority(matcher);
        return matcher.group(1);
    }

    static String getClaParameters(URI uri) throws Dtn.InvalidDtnEid, InvalidClaEid {
        checkValidClaEid(uri);
        Matcher matcher = Pattern.compile(CLA_AUTHORITY_FORMAT).matcher(uri.getAuthority());
        checkClaAuthority(matcher);
        return matcher.group(2);
    }

}
