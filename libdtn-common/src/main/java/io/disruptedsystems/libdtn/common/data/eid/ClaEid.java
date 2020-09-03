package io.disruptedsystems.libdtn.common.data.eid;

/**
 * ClaEid are a specific class of Eid that maps directly to a convergence layer channel.
 * The semantic of a CLA-Eid is defined in the DtnEid draft written by Lucien Loiseau:
 *
 * https://tools.ietf.org/html/draft-loiseau-dtn-cla-eid-00
 *
 * @author Lucien Loiseau on 28/11/18.
 */
public interface ClaEid extends DtnEid {

    String CLA_NODE_REGEXP = "^\\[([^\\[\\]:]+):([^\\[\\]]+)\\]$";

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

    @Override
    ClaEid copyWithDemuxSetTo(String demux) throws EidFormatException;

    @Override
    ClaEid copy();


}
