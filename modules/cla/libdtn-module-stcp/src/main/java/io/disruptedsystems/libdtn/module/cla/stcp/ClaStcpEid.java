package io.disruptedsystems.libdtn.module.cla.stcp;

import io.disruptedsystems.libdtn.common.data.eid.BaseClaEid;
import io.disruptedsystems.libdtn.common.data.eid.EidFormatException;

/**
 * ClaStcpEid is a special Eid whose scheme is "cla:stcp".
 * It is used to identify ConvergenceLayerStcp Channel.
 *
 * @author Lucien Loiseau on 17/10/18.
 */
public class ClaStcpEid extends BaseClaEid {

    public static String EID_CLA_STCP_SCHEME = "stcp";

    String host;
    int port;

    /**
     * Constructor. A stcp eid follows the following pattern "cla:stcp:host:port/sink/"
     *
     * @param host  stcp host
     * @param port  stcp port
     * @param demux dtn demux
     * @throws EidFormatException if the supplied parameters are not valid
     */
    public ClaStcpEid(String host, int port, String demux) throws EidFormatException {
        super(EID_CLA_STCP_SCHEME, host + ":" + port, demux);
        this.host = host;
        this.port = port;
    }

    public ClaStcpEid(String host, int port) throws EidFormatException {
        this(host, port, "");
    }
}
