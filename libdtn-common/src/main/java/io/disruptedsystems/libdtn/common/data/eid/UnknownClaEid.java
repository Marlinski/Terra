package io.disruptedsystems.libdtn.common.data.eid;

/**
 * UnknownClaEid is a helper class to hold a ClaEid whose cla-scheme is unknown and so the
 * ClaEid-scheme-specific part could not be parsed.
 *
 * @author Lucien Loiseau on 28/11/18.
 */
public class UnknownClaEid extends BaseClaEid {

    /**
     * create a new UnknownEid by supplying the iana value and the scheme specific part.
     *
     * @param claName     cla scheme
     * @param claSpecific ClaEid scheme-specific part
     */
    public UnknownClaEid(String claName, String claSpecific) {
        super(claName, claSpecific);
    }

    @Override
    public String getClaSpecificPart() {
        return claParameters;
    }

    @Override
    public UnknownClaEid copy() {
        return new UnknownClaEid(claName, claParameters);
    }
}
