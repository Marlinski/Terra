package io.disruptedsystems.libdtn.module.cla.stcp;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.disruptedsystems.libdtn.common.data.eid.Cla;
import io.disruptedsystems.libdtn.common.data.eid.Dtn;

import static io.disruptedsystems.libdtn.common.data.eid.Cla.getClaScheme;

/**
 * Parser for the cla-specific part of a ClaEid.
 *
 * @author Lucien Loiseau on 28/11/18.
 */
public interface ClaStcp {

    String STCP_PARAM_REGEX = "^([^:/?#]+):([0-9]+)$";
    String STCP_MODULE_NAME = "stcp";

    class InvalidClaStcpEid extends Exception {
    }

    static void checkValidClaStcpEid(URI uri, Matcher matcher) throws Cla.InvalidClaEid, InvalidClaStcpEid, Dtn.InvalidDtnEid {
        if(!getClaScheme(uri).equals(STCP_MODULE_NAME)) {
            throw new InvalidClaStcpEid();
        }
        if (!matcher.reset(Cla.getClaParameters(uri)).matches()) {
            throw new InvalidClaStcpEid();
        }
    }

    static boolean isClaStcpEid(URI uri) {
        try {
            Matcher matcher = Pattern.compile(STCP_PARAM_REGEX).matcher("");
            checkValidClaStcpEid(uri, matcher);
            return true;
        } catch (Cla.InvalidClaEid | Dtn.InvalidDtnEid | InvalidClaStcpEid e) {
            return false;
        }
    }

    static String getStcpHost(URI uri) throws Dtn.InvalidDtnEid, Cla.InvalidClaEid, InvalidClaStcpEid {
        Matcher matcher = Pattern.compile(STCP_PARAM_REGEX).matcher("");
        checkValidClaStcpEid(uri, matcher);
        return matcher.group(1);
    }

    static String getStcpHostUnsafe(URI uri) {
        try {
            Matcher matcher = Pattern.compile(STCP_PARAM_REGEX).matcher("");
            checkValidClaStcpEid(uri, matcher);
            return matcher.group(1);
        } catch( Dtn.InvalidDtnEid | Cla.InvalidClaEid | InvalidClaStcpEid e) {
            throw new IllegalArgumentException(e);
        }
    }

    static int getStcpPort(URI uri) throws Cla.InvalidClaEid, InvalidClaStcpEid, Dtn.InvalidDtnEid {
        Matcher matcher = Pattern.compile(STCP_PARAM_REGEX).matcher("");
        checkValidClaStcpEid(uri, matcher);
        return Integer.parseInt(matcher.group(2));
    }

    static int getStcpPortUnsafe(URI uri) {
        try {
            Matcher matcher = Pattern.compile(STCP_PARAM_REGEX).matcher("");
            checkValidClaStcpEid(uri, matcher);
            return Integer.parseInt(matcher.group(2));
        } catch( Dtn.InvalidDtnEid | Cla.InvalidClaEid | InvalidClaStcpEid e) {
            throw new IllegalArgumentException(e);
        }
    }

    static URI create(String host, int port) throws URISyntaxException, Dtn.InvalidDtnEid, Cla.InvalidClaEid {
        return create(host, port, "/", null, null);
    }

    static URI create(String host, int port, String path) throws URISyntaxException, Dtn.InvalidDtnEid, Cla.InvalidClaEid {
        return create(host, port, path, null, null);
    }

    static URI create(String host, int port, String path, String query) throws URISyntaxException, Dtn.InvalidDtnEid, Cla.InvalidClaEid {
        return create(host, port, path, query, null);
    }

    static URI create(String host, int port, String path, String query, String fragment) throws URISyntaxException, Dtn.InvalidDtnEid, Cla.InvalidClaEid {
        return Cla.create(STCP_MODULE_NAME, host+":"+port, path, query, fragment);
    }
}
