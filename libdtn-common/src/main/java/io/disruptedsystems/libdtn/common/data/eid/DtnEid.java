package io.disruptedsystems.libdtn.common.data.eid;

import java.util.UUID;

/**
 * @author Lucien Loiseau on 03/09/20.
 */
public interface DtnEid extends Eid {

    int EID_DTN_IANA_VALUE = 1;
    String EID_DTN_SCHEME = "dtn";

    static DtnEid nullEid() {
        BaseDtnEid ret = new BaseDtnEid();
        ret.ssp = "none";
        return ret;
    }

    static DtnEid generate() {
        BaseDtnEid ret = new BaseDtnEid();
        final String uuid = UUID.randomUUID().toString().replace("-", "");
        ret.ssp = "//" + uuid + "/";
        return ret;
    }

    /**
     * check wether this dtn eid is the null endpoint (dtn:none)
     *
     * @return true if null endpoint, false otherwise
     */
    boolean isNullEndPoint();

    /**
     * return the node-name part of this 'dtn' eid
     *
     * @return a String containing the node-name of this eid.
     */
    String getNodeName();

    /**
     * return the demux part of this 'dtn' eid
     *
     * @return a String containing the demux of this eid.
     */
    String getPath();

    /**
     * todo
     * @return
     */
    boolean isSingleton();

    /**
     * todo
     * @return
     */
    boolean isAuthoritativeEid();

    /**
     * todo
     * @return
     */
    boolean isAuthoritativeOver(Eid other);

    /**
     * copy this eid.
     *
     * @return a new DtnEid.
     */
    @Override
    DtnEid copy();


    /**
     * returns a copy of this eid with the demux part of this 'dtn' eid set
     * the current Eid will not be modified.
     *
     * @throws EidFormatException if this breaks the dtn scheme specification
     */
    DtnEid copyWithDemuxSetTo(String demux) throws EidFormatException;
}
