package io.disruptedsystems.libdtn.common.data.eid;

/**
 * BaseClaEid implements some of the common ClaEid operations.
 *
 * @author Lucien Loiseau on 17/10/18.
 */
public abstract class BaseClaEid extends BaseEid implements ClaEid {

    public String claName;
    public String claParameters;
    public String claSink;

    // should only be called by safe constructor, no validity check
    protected BaseClaEid(String claName, String claParameters) {
        this.claName = claName;
        this.claParameters = claParameters;
        this.claSink = "";
    }

    protected BaseClaEid(String claName, String claParameters, String sink)
            throws EidFormatException {
        this.claName = claName;
        this.claParameters = claParameters;
        this.claSink = sink;
        checkValidity();
    }

    @Override
    public int ianaNumber() {
        return ClaEid.EID_CLA_IANA_VALUE;
    }

    @Override
    public String getScheme() {
        return "cla";
    }

    @Override
    public String getSsp() {
        return claName + ":" + getClaSpecificPart();
    }

    @Override
    public String getClaName() {
        return claName;
    }

    @Override
    public String getClaParameters() {
        return claParameters;
    }

    @Override
    public String getSink() {
        return claSink;
    }

    @Override
    public ClaEid setSink(String sink) throws EidFormatException {
        this.claSink = sink;
        checkValidity();
        return this;
    }


    @Override
    public boolean matches(Eid other) {
        if (other == null) {
            return false;
        }
        if (other instanceof ClaEid) {
            return claParameters.equals(((BaseClaEid) other).claParameters)
                    && claName.equals(((BaseClaEid) other).claName);
        }
        return false;
    }
}
