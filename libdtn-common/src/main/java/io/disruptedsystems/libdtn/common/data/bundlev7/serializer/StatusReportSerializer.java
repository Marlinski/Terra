package io.disruptedsystems.libdtn.common.data.bundlev7.serializer;

import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborEncoder;
import io.disruptedsystems.libdtn.common.data.StatusReport;

/**
 * StatusReportSerializer serializes a {@link StatusReport}.
 *
 * @author Lucien Loiseau on 10/11/18.
 */
public class StatusReportSerializer {

    /**
     * serializes a {@link StatusReport}.
     *
     * @param report to serialize.
     * @return a Cbor-encoded serialized StatusReport.
     */
    static CborEncoder encode(StatusReport report) {

        /* /!\ fragmentation is not yet supported so there should be no fragment */
        CborEncoder enc = CBOR.encoder()
                .cbor_start_array(report.subjectBundleIsFragment ? 6 : 4);

        /* encode first item - status assertion array*/
        enc.cbor_start_array(StatusReport.StatusAssertion.values().length);
        for (StatusReport.StatusAssertion assertion : StatusReport.StatusAssertion.values()) {
            if (report.statusInformation.containsKey(assertion)) {
                enc.cbor_start_array(2)
                        .cbor_encode_boolean(true)
                        .cbor_encode_int(report.statusInformation.get(assertion));
            } else {
                enc.cbor_start_array(1)
                        .cbor_encode_boolean(false);
            }
        }

        /* encode second item - status error code */
        enc.cbor_encode_int(report.code.ordinal());

        /* encode third item - subject bundle source Eid */
        enc.merge(EidSerializer.encode(report.source));

        /* encode fourth item - subject bundle creation timestamp */
        enc.cbor_encode_int(report.creationTimestamp);

        /* todo fragmentation not supported yet */
        return enc;
    }

}
