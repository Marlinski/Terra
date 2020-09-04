package io.disruptedsystems.libdtn.module.cla.bows;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.disruptedsystems.libdtn.common.data.eid.Cla;
import io.disruptedsystems.libdtn.common.data.eid.Dtn;

import static io.disruptedsystems.libdtn.common.data.eid.Cla.getClaScheme;

/**
 * ClaStcpEid is a special Eid whose scheme is "cla:stcp".
 * It is used to identify ConvergenceLayerStcp Channel.
 *
 * @author Lucien Loiseau on 17/10/18.
 */
public interface ClaBows {

    String BOWS_PARAM_REGEX = "^([a-zA-Z0-9-_.]+)$";
    String BOWS_MODULE_NAME = "bows";

    class InvalidClaBowsEid extends Exception {
    }

    static void checkValidClaBowsEid(URI uri, Matcher matcher) throws Cla.InvalidClaEid, InvalidClaBowsEid, Dtn.InvalidDtnEid {
        if(!getClaScheme(uri).equals(BOWS_MODULE_NAME)) {
            throw new InvalidClaBowsEid();
        }
        if (!matcher.reset(Cla.getClaParameters(uri)).matches()) {
            throw new InvalidClaBowsEid();
        }
    }

    static boolean isClaBowsEid(URI uri) {
        try {
            Matcher matcher = Pattern.compile(BOWS_PARAM_REGEX).matcher("");
            checkValidClaBowsEid(uri, matcher);
            return true;
        } catch (Cla.InvalidClaEid | Dtn.InvalidDtnEid | InvalidClaBowsEid e) {
            return false;
        }
    }

    static URI getWebsocketUrl(URI uri) throws Dtn.InvalidDtnEid, Cla.InvalidClaEid, InvalidClaBowsEid, URISyntaxException {
        Matcher matcher = Pattern.compile(BOWS_PARAM_REGEX).matcher("");
        checkValidClaBowsEid(uri, matcher);
        return decode(matcher.group(1));
    }

    static URI getWebsocketUrlUnsafe(URI uri) {
        try {
            Matcher matcher = Pattern.compile(BOWS_PARAM_REGEX).matcher("");
            checkValidClaBowsEid(uri, matcher);
            return decode(matcher.group(1));
        } catch(Dtn.InvalidDtnEid | Cla.InvalidClaEid | InvalidClaBowsEid | URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    static URI create(URI url) throws URISyntaxException, Dtn.InvalidDtnEid, Cla.InvalidClaEid {
        return create(url, "/", null, null);
    }

    static URI create(URI url, String path) throws URISyntaxException, Dtn.InvalidDtnEid, Cla.InvalidClaEid {
        return create(url, path, null, null);
    }

    static URI create(URI url, String path, String query) throws URISyntaxException, Dtn.InvalidDtnEid, Cla.InvalidClaEid {
        return create(url, path, query, null);
    }

    static URI create(URI url, String path, String query, String fragment) throws URISyntaxException, Dtn.InvalidDtnEid, Cla.InvalidClaEid {
        return Cla.create(BOWS_MODULE_NAME, encode(url), path, query, fragment);
    }

    static String encode(URI in) {
        return Base64.getEncoder().encodeToString(in.toASCIIString().getBytes())
                .replaceAll("\\+", ".")
                .replaceAll("/", "_")
                .replaceAll("=", "-");
    }

    static URI decode(String in) throws URISyntaxException {
        return new URI(new String(Base64.getDecoder().decode(in
                .replaceAll("\\.", "+")
                .replaceAll("_", "/")
                .replaceAll("-", "="))));
    }


}
