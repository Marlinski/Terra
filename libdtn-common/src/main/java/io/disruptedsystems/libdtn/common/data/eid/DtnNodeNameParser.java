package io.disruptedsystems.libdtn.common.data.eid;

/**
 * EidSspParser is an interface to create an Eid after parsing the scheme-specific part.
 *
 * @author Lucien Loiseau on 28/11/18.
 */
public interface DtnNodeNameParser {

    /**
     * parse a scheme-specific part and return a new Eid if valid.
     *
     * @param dsp Dtn-Specific part
     * @return new Eid
     * @throws EidFormatException if scheme is unknown or if scheme-specific part is invalid.
     */
    BaseDtnEid create(String dsp) throws EidFormatException;

}
