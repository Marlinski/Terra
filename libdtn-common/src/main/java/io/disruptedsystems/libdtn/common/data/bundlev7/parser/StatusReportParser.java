package io.disruptedsystems.libdtn.common.data.bundlev7.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.disruptedsystems.libdtn.common.data.StatusReport;

import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborParser;
import io.marlinski.libcbor.rxparser.RxParserException;

/**
 * StatusReportParser parses a StatusReport.
 *
 * @author Lucien Loiseau on 10/11/18.
 */
public class StatusReportParser {

    private static final Logger log = LoggerFactory.getLogger(StatusReportParser.class);

    // CHECKSTYLE IGNORE LineLength
    static CborParser getParser(StatusReport report) {
        return CBOR.parser()
                .cbor_open_array((p, t, i) -> {
                    log.trace(".. status_report_array size=" + i);
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
                                                    log.trace(".... " + assertion + "=false");
                                                }));
                                            } else if (i == 2) {
                                                p.insert_now(CBOR.parser()
                                                        .cbor_parse_boolean((p2, b) -> {
                                                            log.trace( ".... " + assertion + "=true");
                                                        })
                                                        .cbor_parse_int((p2, t2, timestamp) -> {
                                                            log.trace( ".... timestamp=" + timestamp);
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
                            log.trace( "... status_assertion_array_size=" + size);
                            if (size != StatusReport.StatusAssertion.values().length) {
                                throw new RxParserException("wrong number of status assertion");
                            }
                        },
                        (p, t, item) -> { /* ignore, already dealt with in item factory */
                        },
                        (p, t, a) -> { /* ignore, already dealt with in item factory */
                        })
                .cbor_parse_int((p, t, error) -> {
                    log.trace( ".. error_code=" + error);
                    if (error > StatusReport.ReasonCode.values().length) {
                        report.code = StatusReport.ReasonCode.Other;
                    } else {
                        report.code = StatusReport.ReasonCode.values()[(int) error];
                    }
                })
                .cbor_parse_custom_item(EidItem::new, (p, t, item) -> {
                    log.trace( ".. subject_source_eid=" + item.eid.toString());
                    report.source = item.eid;
                })
                .cbor_parse_int((p, t, timestamp) -> {
                    log.trace( ".. subject_creation_timestamp=" + timestamp);
                    report.creationTimestamp = timestamp;
                }); //todo fragmented bundle is not supported
    }
    // CHECKSTYLE END IGNORE LineLength
}