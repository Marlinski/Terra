package io.disruptedsystems.libdtn.core.processor;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.CanonicalBlock;
import io.disruptedsystems.libdtn.common.data.PayloadBlock;
import io.disruptedsystems.libdtn.common.data.PrimaryBlock;
import io.disruptedsystems.libdtn.common.data.StatusReport;
import io.disruptedsystems.libdtn.common.data.blob.VolatileBlob;
import io.disruptedsystems.libdtn.common.data.blob.WritableBlob;
import io.disruptedsystems.libdtn.common.data.bundlev7.processor.BlockProcessorFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.processor.ProcessingException;
import io.disruptedsystems.libdtn.common.data.bundlev7.serializer.AdministrativeRecordSerializer;

import io.disruptedsystems.libdtn.common.data.eid.Dtn;
import io.disruptedsystems.libdtn.core.api.BundleProtocolApi;
import io.disruptedsystems.libdtn.core.api.ConfigurationApi;
import io.disruptedsystems.libdtn.core.api.CoreApi;
import io.disruptedsystems.libdtn.core.api.DeliveryApi;
import io.disruptedsystems.libdtn.core.api.LocalEidApi;
import io.disruptedsystems.libdtn.core.utils.ClockUtil;
import io.marlinski.libcbor.CborEncoder;

import static io.disruptedsystems.libdtn.common.data.BlockHeader.BlockV7Flags.DELETE_BUNDLE_IF_NOT_PROCESSED;
import static io.disruptedsystems.libdtn.common.data.BlockHeader.BlockV7Flags.DISCARD_IF_NOT_PROCESSED;
import static io.disruptedsystems.libdtn.common.data.BlockHeader.BlockV7Flags.TRANSMIT_STATUSREPORT_IF_NOT_PROCESSED;
import static io.disruptedsystems.libdtn.common.data.PrimaryBlock.BundleV7Flags.DELETION_REPORT;
import static io.disruptedsystems.libdtn.common.data.PrimaryBlock.BundleV7Flags.DELIVERY_REPORT;
import static io.disruptedsystems.libdtn.common.data.PrimaryBlock.BundleV7Flags.RECEPTION_REPORT;
import static io.disruptedsystems.libdtn.common.data.StatusReport.ReasonCode.BlockUnintelligible;
import static io.disruptedsystems.libdtn.common.data.StatusReport.ReasonCode.LifetimeExpired;
import static io.disruptedsystems.libdtn.common.data.StatusReport.ReasonCode.NoAdditionalInformation;
import static io.disruptedsystems.libdtn.common.data.StatusReport.ReasonCode.NoKnownRouteForDestination;
import static io.disruptedsystems.libdtn.common.data.StatusReport.StatusAssertion.ReportingNodeDeletedBundle;
import static io.disruptedsystems.libdtn.common.data.StatusReport.StatusAssertion.ReportingNodeDeliveredBundle;
import static io.disruptedsystems.libdtn.common.data.StatusReport.StatusAssertion.ReportingNodeForwardedBundle;
import static io.disruptedsystems.libdtn.common.data.StatusReport.StatusAssertion.ReportingNodeReceivedBundle;

/**
 * BundleProtocol is the entry point of all Bundle (either from Application Agent or
 * Convergence Layer) and follows the processing instruction described in the RFC.
 *
 * @author Lucien Loiseau on 28/09/18.
 */
public class BundleProtocol implements BundleProtocolApi {

    private static final String TAG = "BundleProtocol";

    private CoreApi core;

    public BundleProtocol(CoreApi core) {
        this.core = core;
    }

    private boolean reporting() {
        return core.getConf().<Boolean>get(ConfigurationApi.CoreEntry.ENABLE_STATUS_REPORTING).value();
    }

    /* 5.2 */
    @Override
    public void bundleTransmission(Bundle bundle) {
        /* 5.2 - step 1 */
        core.getLogger().v(TAG, "5.2-1 " + bundle.bid.toString());
        if (!Dtn.isNullEid(bundle.getSource())
                && (core.getLocalEidTable().isEidNodeId(bundle.getSource()) != null)) {
            bundle.setSource(core.getLocalEidTable().nodeId());
        }
        bundle.tag(TAG_DISPATCH_PENDING);

        /* 5.2 - step 2 */
        core.getLogger().v(TAG, "5.2-2 " + bundle.bid.toString());
        bundleForwarding(bundle);
    }

    /* 5.3 */
    @Override
    public void bundleDispatching(Bundle bundle) {
        core.getLogger().i(TAG, "dispatching bundle: " + bundle.bid.toString()
                + " to Eid: " + bundle.getDestination());

        /* 5.3 - step 1 */
        core.getLogger().v(TAG, "5.3-1: " + bundle.bid);
        LocalEidApi.LookUpResult isLocal = core.getLocalEidTable().isEidLocal(bundle.getDestination());
        if (isLocal != LocalEidApi.LookUpResult.eidIsNotLocal) {
            bundleLocalDelivery(isLocal, bundle);
            return;
        }

        if (core.getConf().<Boolean>get(ConfigurationApi.CoreEntry.ENABLE_FORWARDING).value()) {
            /* 5.3 - step 2 */
            core.getLogger().v(TAG, "5.3-2: " + bundle.bid.toString());
            bundleForwarding(bundle);
        } else {
            bundle.tag("reason_code", NoKnownRouteForDestination);
            bundleDeletion(bundle);
        }
    }

    /* 5.4 */
    private void bundleForwarding(Bundle bundle) {
        core.getLogger().d(TAG, "forwarding bundle: " + bundle.bid.toString());

        /* 5.4 - step 1 */
        bundle.removeTag(TAG_DISPATCH_PENDING);
        bundle.tag(TAG_FORWARD_PENDING);

        /* 5.4 - step 2 */
        core.getRoutingEngine().route(bundle).subscribe(
                (routingResult) -> {
                    switch (routingResult) {
                        case Forwarded:
                            bundleForwardingSuccessful(bundle);
                            break;
                        case CustodyRefused:
                            bundleForwardingFailed(bundle);
                            break;
                        case CustodyAccepted:
                            endProcessing(bundle);
                            break;
                        default:
                    }
                },
                routingError -> bundleForwardingFailed(bundle));
    }

    /* 5.4 - step 5 */
    @Override
    public void bundleForwardingSuccessful(Bundle bundle) {
        core.getLogger().d(TAG, "forwarding successful: " + bundle.bid.toString());
        bundle.removeTag(TAG_FORWARD_PENDING);
        createStatusReport(ReportingNodeForwardedBundle, bundle, NoAdditionalInformation);
        bundleDiscarding(bundle);
    }

    /* 5.4.2 */
    private void bundleForwardingFailed(Bundle bundle) {
        /* 5.4.2 - step 1 */
        core.getLogger().v(TAG, "5.4.2-1 " + bundle.bid.toString());
        // atm we never send the bundle back to the source

        /* 5.4.2 - step 2 */
        core.getLogger().v(TAG, "5.4.2-2 " + bundle.bid.toString());
        if (core.getLocalEidTable().isEidLocal(bundle.getDestination()) != null) {
            bundle.removeTag(TAG_FORWARD_PENDING);
            bundleDiscarding(bundle);
        } else {
            bundleDeletion(bundle);
        }

    }

    /* 5.5 */
    @Override
    public void bundleExpired(Bundle bundle) {
        core.getLogger().v(TAG, "5.5 " + bundle.bid.toString());
        bundle.tag("reason_code", LifetimeExpired);
        bundleDeletion(bundle);
    }

    /* 5.6 */
    @Override
    public void bundleReception(Bundle bundle) {
        /* 5.6 - step 1 */
        core.getLogger().v(TAG, "5.6-1 " + bundle.bid.toString());
        bundle.tag(TAG_DISPATCH_PENDING);

        /* 5.6 - step 2 */
        core.getLogger().v(TAG, "5.6-2 " + bundle.bid.toString());
        if (bundle.getV7Flag(RECEPTION_REPORT) && reporting()) {
            createStatusReport(ReportingNodeReceivedBundle, bundle, NoAdditionalInformation);
        }

        /* 5.6 - step 3 */
        core.getLogger().v(TAG, "5.6-3 " + bundle.bid.toString());
        try {
            for (CanonicalBlock block : bundle.getBlocks()) {
                try {
                    core.getExtensionManager().getBlockProcessorFactory().create(block.type)
                            .onReceptionProcessing(block, bundle, core.getLogger());
                } catch (BlockProcessorFactory.ProcessorNotFoundException pe) {
                    if (block.getV7Flag(TRANSMIT_STATUSREPORT_IF_NOT_PROCESSED) && reporting()) {
                        createStatusReport(
                                ReportingNodeReceivedBundle,
                                bundle,
                                BlockUnintelligible);
                    }
                    if (block.getV7Flag(DELETE_BUNDLE_IF_NOT_PROCESSED)) {
                        bundle.tag("reason_code", BlockUnintelligible);
                        throw new ProcessingException();
                    }
                    if (block.getV7Flag(DISCARD_IF_NOT_PROCESSED)) {
                        bundle.delBlock(block);
                    }
                }
            }
        } catch (ProcessingException e) {
            bundleDeletion(bundle);
            return;
        }

        /* 5.6 - step 4 */
        core.getLogger().v(TAG, "5.6-4 " + bundle.bid.toString());
        bundleDispatching(bundle);
    }

    /* 5.7 */
    private void bundleLocalDelivery(LocalEidApi.LookUpResult localMatch, Bundle bundle) {
        bundle.tag(TAG_DELIVERY_PENDING);
        /* 5.7 - step 1 */
        core.getLogger().v(TAG, "5.7-1 " + bundle.bid);
        // todo: support fragmentation

        /* 5.7 - step 2 */
        core.getLogger().v(TAG, "5.7-2 " + bundle.bid);
        core.getDelivery().deliver(localMatch, bundle).subscribe(
                () -> bundleLocalDeliverySuccessful(bundle),
                deliveryFailure -> bundleLocalDeliveryFailure(bundle, deliveryFailure));

    }

    /* 5.7 - step 3 */
    @Override
    public void bundleLocalDeliverySuccessful(Bundle bundle) {
        core.getLogger().i(TAG, "bundle successfully delivered: " + bundle.bid);
        bundle.removeTag(TAG_DELIVERY_PENDING);
        if (bundle.getV7Flag(DELIVERY_REPORT) && reporting()) {
            createStatusReport(ReportingNodeDeliveredBundle, bundle, NoAdditionalInformation);
        }
        bundleDeletion(bundle);
    }

    /* 5.7 - step 2 - delivery failure */
    @Override
    public void bundleLocalDeliveryFailure(Bundle bundle, Throwable reason) {
        core.getLogger().i(TAG, "bundle could not be delivered to: "
                + bundle.getDestination()
                + "  reason=" + ((reason instanceof DeliveryApi.DeliveryFailure)
                    ? ((DeliveryApi.DeliveryFailure)reason).reason.name()
                    : reason.getMessage())
                + "  bundleID=" + bundle.bid + "]");

        if (!bundle.isTagged("in_storage")) {
            core.getStorage().store(bundle).subscribe(
                    () -> {
                        /* register for event and deliver later */
                        core.getDelivery().deliverLater(bundle);
                        endProcessing(bundle);
                    },
                    storageFailure -> {
                        storageFailure.printStackTrace();
                        /* abandon delivery */
                        core.getLogger().w(TAG, "storage failure. "
                                + storageFailure.getMessage());
                        bundleDeletion(bundle);
                    });
        } else {
            /* register for event and deliver later */
            core.getDelivery().deliverLater(bundle);
        }
    }

    /* 5.8 */
    private void bundleFragmentation(Bundle bundle) {
        // not supported atm
        core.getLogger().v(TAG, "5.8 " + bundle.bid.toString());
    }

    /* 5.10 */
    private void bundleDeletion(Bundle bundle) {
        core.getLogger().i(TAG, "deleting bundle ("
                + bundle.<StatusReport.ReasonCode>getTagAttachment("reason_code") + "): "
                + bundle.bid.toString());

        /* 5.10 - step 1 */
        core.getLogger().v(TAG, "5.10-2 " + bundle.bid);
        if (bundle.getV7Flag(DELETION_REPORT) && reporting()) {
            createStatusReport(ReportingNodeDeletedBundle, bundle, NoAdditionalInformation);
        }

        /* 5.10 - step 2 */
        core.getLogger().v(TAG, "5.10-2 " + bundle.bid);
        bundle.removeTag(TAG_DISPATCH_PENDING);
        bundle.removeTag(TAG_FORWARD_PENDING);
        bundle.removeTag(TAG_DELIVERY_PENDING);
        bundleDiscarding(bundle);
    }

    /* 5.11 */
    private void bundleDiscarding(Bundle bundle) {
        core.getLogger().i(TAG, "discarding bundle: " + bundle.bid.toString());
        core.getStorage().remove(bundle.bid).subscribe(
                bundle::clearBundle,
                e -> bundle.clearBundle());
        endProcessing(bundle);
    }

    /* not in RFC - end processing for this bundle, send all status report if any */
    private void endProcessing(Bundle bundle) {
        if (bundle.isTagged("status-reports")) {
            List<Bundle> reports = bundle.getTagAttachment("status-reports");
            for (Bundle report : reports) {
                core.getLogger().i(TAG, "sending status report to: "
                        + report.getDestination());
                report.setSource(core.getLocalEidTable().nodeId());
                bundleDispatching(report);
            }
        }
    }

    /* create status report */
    private void createStatusReport(StatusReport.StatusAssertion assertion,
                                    Bundle bundle,
                                    StatusReport.ReasonCode reasonCode) {
        if (Dtn.isNullEid(bundle.getReportTo())) {
            return;
        }

        StatusReport statusReport = new StatusReport(reasonCode);
        statusReport.source = bundle.getSource();
        statusReport.creationTimestamp = bundle.getCreationTimestamp();
        if (assertion.equals(ReportingNodeDeletedBundle)
                && bundle.getV7Flag(PrimaryBlock.BundleV7Flags.DELETION_REPORT)) {
            statusReport.statusInformation
                    .put(ReportingNodeDeletedBundle, ClockUtil.getCurrentTime());
        } else if (assertion.equals(ReportingNodeForwardedBundle)
                && bundle.getV7Flag(PrimaryBlock.BundleV7Flags.FORWARD_REPORT)) {
            statusReport.statusInformation
                    .put(ReportingNodeForwardedBundle, ClockUtil.getCurrentTime());
        } else if (assertion.equals(ReportingNodeReceivedBundle)
                && bundle.getV7Flag(PrimaryBlock.BundleV7Flags.RECEPTION_REPORT)) {
            statusReport.statusInformation
                    .put(ReportingNodeReceivedBundle, ClockUtil.getCurrentTime());
        } else if (assertion.equals(ReportingNodeDeliveredBundle)
                && bundle.getV7Flag(PrimaryBlock.BundleV7Flags.DELIVERY_REPORT)) {
            statusReport.statusInformation
                    .put(ReportingNodeDeliveredBundle, ClockUtil.getCurrentTime());
        } else {
            return;
        }

        /* create the bundle that will carry this status report back to the reporting node */
        Bundle report = new Bundle(bundle.getReportTo());

        /* get size of status report for the payload */
        CborEncoder enc = AdministrativeRecordSerializer.encode(statusReport);
        long size = enc.observe()
                .map(ByteBuffer::remaining)
                .reduce(0, (a, b) -> a + b)
                .blockingGet();

        /* serialize the status report into the bundle payload */
        VolatileBlob blobReport = new VolatileBlob((int) size);
        final WritableBlob wblob = blobReport.getWritableBlob();
        enc.observe()
                .map(wblob::write)
                .doOnComplete(wblob::close)
                .subscribe();
        report.addBlock(new PayloadBlock(blobReport));

        /* attach the status report to the bundle (it will be send during endProcessing) */
        List<Bundle> reports = bundle.getTagAttachment("status-reports");
        if (reports == null) {
            reports = new LinkedList<>();
            bundle.tag("status-reports", reports);
        }
        reports.add(report);
    }

}
