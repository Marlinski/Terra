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

    String getNodeName();

    String getDemux();

    /**
     * copy the demux from another dtn eid to this one
     *
     * @param other eid
     * @return current eid
     */
    DtnEid copyDemux(DtnEid other);

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

    @Override
    DtnEid copy();
}
