package io.disruptedsystems.libdtn.module.cla.bows;

import io.disruptedsystems.libdtn.common.data.eid.ClaEidParser;
import io.disruptedsystems.libdtn.common.data.eid.EidFormatException;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;

/**
 * Parser for the cla-specific part of a ClaEid.
 *
 * @author Lucien Loiseau on 28/11/18.
 */
public class ClaBowsEidParser implements ClaEidParser {

    @Override
    public ClaBowsEid createClaEid(String claName, String claSpecific)
            throws EidFormatException {
        try {
            URI uri = URI.create(claSpecific);
            String query = uri.getQuery();

            // extract the sink from the query (if any)
            if (query != null) {
                StringBuilder newQuery = new StringBuilder();
                String sink = null;
                int i = 0;
                for (String pair : query.split("&")) {
                    int idx = pair.indexOf("=");
                    String parameter = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                    String value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                    if (!parameter.equals("sink")) {
                        if (i++ > 0) {
                            pair = "&" + pair;
                        }
                        newQuery.append(pair);
                    } else {
                        sink = value;
                    }
                }

                URI claParameter = new URI(uri.getScheme(), uri.getAuthority(),
                        uri.getPath(), newQuery.toString().equals("") ? null : newQuery.toString(),
                        uri.getFragment());
                return new ClaBowsEid(claParameter, sink);
            } else {
                return new ClaBowsEid(uri, null);
            }
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new EidFormatException(e.getMessage());
        }
    }
}
