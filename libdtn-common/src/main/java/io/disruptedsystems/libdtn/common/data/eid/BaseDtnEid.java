package io.disruptedsystems.libdtn.common.data.eid;

/**
 * DtnEid is a class of Eid whose scheme is "dtn".
 *
 * @author Lucien Loiseau on 17/10/18.
 */
public class BaseDtnEid extends BaseEid implements DtnEid {

    protected String ssp = "";
    protected String nodeName = "";
    protected String demux = "";

    protected BaseDtnEid() {
    }

    public BaseDtnEid(DtnEid o) {
        this.ssp = o.getSsp();
        this.nodeName = o.getNodeName();
        this.demux = o.getDemux();
    }

    public BaseDtnEid(String nodeName) throws EidFormatException {
        this(nodeName, "");
    }

    public BaseDtnEid(String nodeName, String demux) throws EidFormatException {
        this.nodeName = nodeName == null ? "" : nodeName;
        this.demux = (demux == null ? "" : demux);
        this.demux = (this.demux.startsWith("/") ? this.demux.substring(1) : this.demux);
        this.ssp = "//" + this.nodeName + "/" + this.demux;
        checkValidity();
    }

    @Override
    public DtnEid copy() {
        return new BaseDtnEid(this);
    }

    @Override
    public int ianaNumber() {
        return EID_DTN_IANA_VALUE;
    }

    @Override
    public String getScheme() {
        return EID_DTN_SCHEME;
    }

    @Override
    public String getSsp() {
        return ssp;
    }

    @Override
    public String getNodeName() {
        return nodeName;
    }

    @Override
    public String getDemux() {
        return demux;
    }

    @Override
    public DtnEid copyDemux(DtnEid other) {
        this.demux = other.getDemux();
        return this;
    }

    @Override
    public boolean isNullEndPoint() {
        return ssp.equals("none");
    }

    @Override
    public boolean isSingleton() {
        return !demux.startsWith("~");
    }

    @Override
    public boolean isAuthoritativeEid() {
        return !isNullEndPoint() && demux.equals("");
    }

    @Override
    public boolean isAuthoritativeOver(Eid other) {
        return isAuthoritativeEid()
                && (other instanceof BaseDtnEid)
                && nodeName.equals(((BaseDtnEid) other).nodeName);
    }
}