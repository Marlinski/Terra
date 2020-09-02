package io.disruptedsystems.libdtn.common.data.eid;

/**
 * BaseClaEid implements some of the common ClaEid operations.
 *
 * @author Lucien Loiseau on 17/10/18.
 */
public class BaseClaEid extends BaseDtnEid implements ClaEid {

    protected String claName;
    protected String claParameters;

    private BaseClaEid(ClaEid eid) {
        super(eid);
        this.claName = eid.getClaName();
        this.claParameters = eid.getClaParameters();
    }

    public BaseClaEid(String claName, String claParameters, String demux)
            throws EidFormatException {
        super("["+claName+":"+claParameters+"]", demux);
        this.claName = claName;
        this.claParameters = claParameters == null ? "" : claParameters;
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
    public ClaEid copy() {
        return new BaseClaEid(this);
    }
}
