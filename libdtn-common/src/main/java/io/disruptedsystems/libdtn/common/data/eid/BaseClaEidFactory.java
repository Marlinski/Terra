package io.disruptedsystems.libdtn.common.data.eid;

/**
 * @author Lucien Loiseau on 02/09/20.
 */
public class BaseClaEidFactory implements ClaEidParser {

    private boolean throwException = false;

    public BaseClaEidFactory() {
    }

    public BaseClaEidFactory(boolean throwException) {
        this.throwException = throwException;
    }

    @Override
    public ClaEid createClaEid(String claName, String claSpecific, String demux) throws EidFormatException {
        if(throwException) {
            throw new UnknownClaName(claName);
        }
        return new BaseClaEid(claName, claSpecific, demux);
    }
}
