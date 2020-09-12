package io.disruptedsystems.libdtn.common.data.bundlev7.parser;

import io.disruptedsystems.libdtn.common.data.StatusReport;
import io.disruptedsystems.libdtn.common.utils.Log;
import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborParser;
import io.marlinski.libcbor.rxparser.RxParserException;

import static io.disruptedsystems.libdtn.common.data.bundlev7.parser.AdministrativeRecordItem.TAG;

/**
 * StatusReportParser parses a StatusReport.
 *
 * @author Lucien Loiseau on 10/11/18.
 */
public class StatusReportParser {

    // CHECKSTYLE IGNORE LineLength
    static CborParser getParser(StatusReport report, Log logger) {
        return CBOR.parser()
                .cbor_open_array((p, t, i) -> {
                    logger.v(TAG, ".. status_report_array size=" + i);
                    if (i != 4 && i != 6) {
                        throw new RxParserException("wrong number of element in status report");
                    }
                    if (i == 6) {
                        report.subjectBundleIsFragment = true;
                    }
                })
                .cbor_parse_linear_array(
                        (pos) -> { /* array in array */
                            return () -> { /* status assertion item: [true, timestamp] or [false] */
                                StatusReport.StatusAssertion assertion = StatusReport.StatusAssertion.values()[(int)pos];
                                return CBOR.parser()
                                        .cbor_open_array((p, t, i) -> {
                                            if (i == 1) {
                                                p.insert_now(CBOR.parser().cbor_parse_boolean((p2, b) -> {
                                                    logger.v(TAG, ".... " + assertion + "=false");
                                                }));
                                            } else if (i == 2) {
                                                p.insert_now(CBOR.parser()
                                                        .cbor_parse_boolean((p2, b) -> {
                                                            logger.v(TAG, ".... " + assertion + "=true");
                                                        })
                                                        .cbor_parse_int((p2, t2, timestamp) -> {
                                                            logger.v(TAG, ".... timestamp=" + timestamp);
                                                            report.statusInformation.put(assertion, timestamp);
                                                        })
                                                );
                                            } else {
                                                throw new RxParserException("wrong number of element in status report");
                                            }
                                        });
                            };
                        },
                        (p, t, size) -> {
                            logger.v(TAG, "... status_assertion_array_size=" + size);
                            if (size != StatusReport.StatusAssertion.values().length) {
                                throw new RxParserException("wrong number of status assertion");
                            }
                        },
                        (p, t, item) -> { /* ignore, already dealt with in item factory */
                        },
                        (p, t, a) -> { /* ignore, already dealt with in item factory */
                        })
                .cbor_parse_int((p, t, error) -> {
                    logger.v(TAG, ".. error_code=" + error);
                    if (error > StatusReport.ReasonCode.values().length) {
                        report.code = StatusReport.ReasonCode.Other;
                    } else {
                        report.code = StatusReport.ReasonCode.values()[(int) error];
                    }
                })
                .cbor_parse_custom_item(() -> new EidItem(logger), (p, t, item) -> {
                    logger.v(TAG, ".. subject_source_eid=" + item.eid.toString());
                    report.source = item.eid;
                })
                .cbor_parse_int((p, t, timestamp) -> {
                    logger.v(TAG, ".. subject_creation_timestamp=" + timestamp);
                    report.creationTimestamp = timestamp;
                }); //todo fragmented bundle is not supported
    }
    // CHECKSTYLE END IGNORE LineLength
}