package io.disruptedsystems.libdtn.common.data.bundlev7.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborParser;
import io.marlinski.libcbor.rxparser.RxParserException;
import io.disruptedsystems.libdtn.common.data.AdministrativeRecord;
import io.disruptedsystems.libdtn.common.data.StatusReport;

/**
 * Parser for an {@link AdministrativeRecord}.
 *
 * @author Lucien Loiseau on 10/11/18.
 */
public class AdministrativeRecordItem implements CborParser.ParseableItem {

    private static final Logger log = LoggerFactory.getLogger(AdministrativeRecordItem.class);

    public AdministrativeRecord record;

    private CborParser body;

    @Override
    public CborParser getItemParser() {
        return CBOR.parser()
                .cbor_open_array((parser, tags, i) -> {
                    log.trace(". array size=" + i);
                    if (i != 2) {
                        throw new RxParserException("wrong number of element in canonical block");
                    }
                })
                .cbor_parse_int((parser, tags, i) -> { // block PAYLOAD_BLOCK_TYPE
                    log.trace(". PAYLOAD_BLOCK_TYPE=" + i);
                    switch ((int) i) {
                        case StatusReport.STATUS_REPORT_ADM_TYPE:
                            record = new StatusReport();
                            body = StatusReportParser
                                    .getParser((StatusReport) record);
                            break;
                        default:
                            throw new RxParserException("administrative record type unknown: " + i);
                    }
                })
                .do_here(p -> p.insert_now(body));
    }

}
