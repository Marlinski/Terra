package io.disruptedsystems.libdtn.common.data.eid;

/**
 * ClaEid are a specific class of Eid that maps directly to a convergence layer channel.
 * The semantic of a CLA-Eid is defined in the DtnEid draft written by Lucien Loiseau:
 *
 * https://tools.ietf.org/html/draft-loiseau-dtn-cla-eid-00
 *
 * @author Lucien Loiseau on 28/11/18.
 */
public interface ClaEid extends Eid {

    int EID_CLA_IANA_VALUE = 3;  // not actually an IANA number (yet)
    String EID_CLA_SCHEME = "cla";

    /**
     * get the Convergence Layer name for this ClaEid.
     *
     * @return String representing the Convergence Layer Name
     */
    String getClaName();

    /**
     * get the Convergence Layer specific parameters
     *
     * @return String representing the Convergence Layer specific parameters
     */
    String getClaParameters();

    /**
     * get the Convergence Layer specific part of this ClaEid.
     *
     * @return String representing the Convergence Layer specific part
     */
    String getClaSpecificPart();

    /**
     * get the Sink part of this ClaEid.
     *
     * @return String representing the Sink (also called Path)
     */
    String getSink();

    /**
     * set the Sink part of this ClaEid.
     *
     * @param sink representing the Sink (also called Path)
     */
    ClaEid setSink(String sink) throws EidFormatException;

    /**
     * copy this claeid
     * @return a copy of the ClaEid
     */
    ClaEid copy();

}
