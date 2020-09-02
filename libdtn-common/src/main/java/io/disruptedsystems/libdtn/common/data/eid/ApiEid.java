package io.disruptedsystems.libdtn.common.data.eid;

/**
 * ApiEid are a class of Eid used for "api" related services. For instance api:me always refer
 * to the local eid configured by the current node administrator.
 *
 * @author Lucien Loiseau on 29/10/18.
 */
public class ApiEid extends BaseDtnEid {

    public static ApiEid me() {
        return new ApiEid();
    }

    public ApiEid() {
        super();
        ssp = "//api:me/";
        nodeName = "api:me";
    }

    public ApiEid(String demux) throws EidFormatException{
        super("api:me", demux);
    }
}
