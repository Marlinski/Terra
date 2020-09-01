package io.disruptedsystems.libdtn.module.cla.bows;

import io.disruptedsystems.libdtn.common.data.eid.BaseClaEid;
import io.disruptedsystems.libdtn.common.data.eid.ClaEid;
import io.disruptedsystems.libdtn.common.data.eid.Eid;
import io.disruptedsystems.libdtn.common.data.eid.EidFormatException;

/**
 * ClaStcpEid is a special Eid whose scheme is "cla:stcp".
 * It is used to identify ConvergenceLayerStcp Channel.
 *
 * @author Lucien Loiseau on 17/10/18.
 */
public class ClaStcpEid extends BaseClaEid {

    String host;
    int port;

    // unsafe constructor
    private ClaStcpEid(String host, int port) {
        super("stcp", host + ":" + port);
        this.host = host;
        this.port = port;
    }

    static ClaStcpEid unsafe(String host, int port) {
        return new ClaStcpEid(host, port);
    }

    /**
     * Constructor. A stcp eid follows the following pattern "cla:stcp:host:port/sink/"
     *
     * @param host stcp host
     * @param port stcp port
     * @param sink eid path
     * @throws EidFormatException if the supplied parameters are not valid
     */
    public ClaStcpEid(String host, int port, String sink) throws EidFormatException {
        super("stcp", host + ":" + port, checkRoot(sink));
        this.host = host;
        this.port = port;
    }

    @Override
    public int ianaNumber() {
        return EID_CLA_IANA_VALUE;
    }

    @Override
    public String getClaSpecificPart() {
        return claParameters + claSink;
    }

    @Override
    public ClaStcpEid copy() {
        ClaStcpEid ret = new ClaStcpEid(host, port);
        ret.claSink = this.claSink;
        return ret;
    }

    private static String checkRoot(String s) {
        if (s == null) return "/";
        if (s.startsWith("/")) return s;
        return "/" + s;
    }
}
