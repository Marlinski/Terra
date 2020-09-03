package io.disruptedsystems.libdtn.common.data.bundlev7.parser;

import io.disruptedsystems.libdtn.common.utils.Log;
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

    static final String TAG = "AdministrativeRecordItem";

    public AdministrativeRecordItem(Log logger) {
        this.logger = logger;
    }

    public AdministrativeRecord record;

    private Log logger;
    private CborParser body;

    @Override
    public CborParser getItemParser() {
        return CBOR.parser()
                .cbor_open_array((parser, tags, i) -> {
                    logger.v(TAG, ". array size=" + i);
                    if (i != 2) {
                        throw new RxParserException("wrong number of element in canonical block");
                    }
                })
                .cbor_parse_int((parser, tags, i) -> { // block PAYLOAD_BLOCK_TYPE
                    logger.v(TAG, ". PAYLOAD_BLOCK_TYPE=" + i);
                    switch ((int) i) {
                        case StatusReport.STATUS_REPORT_ADM_TYPE:
                            record = new StatusReport();
                            body = StatusReportParser
                                    .getParser((StatusReport) record, logger);
                            break;
                        default:
                            throw new RxParserException("administrative record type unknown: " + i);
                    }
                })
                .do_here(p -> p.insert_now(body));
    }

}
