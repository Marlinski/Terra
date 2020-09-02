package io.disruptedsystems.libdtn.common.data.eid;

/**
 * UnknowEid is a helper class to hold an Eid whose scheme is unknown and so the scheme-specific
 * part could not be parsed.
 *
 * @author Lucien Loiseau on 17/10/18.
 */
public class UnknowEid extends BaseEid {

    public static final String EID_UNK_SCHEME = "unk";

    private int iana;
    private String ssp;

    /* unsafe constructor - no validity check */
    private UnknowEid() {
    }

    public UnknowEid(UnknowEid o) {
        this.iana = o.iana;
        this.ssp = o.ssp;
    }

    /**
     * create a new UnknownEid by supplying the iana value and the scheme specific part.
     *
     * @param ianaValue of the unknown eid
     * @param ssp scheme-specific part of the unknow eid.
     * @throws EidFormatException if the scheme-specific part is invalid.
     */
    public UnknowEid(int ianaValue, String ssp) throws EidFormatException {
        this.iana = ianaValue;
        this.ssp = ssp;
        if (!Eid.isValidEid(this.getEidString())) {
            throw new EidFormatException("not an URI");
        }
    }

    @Override
    public UnknowEid copy() {
        return new UnknowEid(this);
    }

    @Override
    public int ianaNumber() {
        return iana;
    }

    @Override
    public String getScheme() {
        return EID_UNK_SCHEME;
    }

    @Override
    public String getSsp() {
        return ssp;
    }

    @Override
    public boolean isAuthoritativeEid() {
        return false;
    }

    @Override
    public boolean isAuthoritativeOver(Eid other) {
        return false;
    }
}
