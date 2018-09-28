package io.left.rightmesh.libdtn.core.processor;

import io.left.rightmesh.libdtn.DTNConfiguration;
import io.left.rightmesh.libdtn.core.routing.LocalEIDTable;
import io.left.rightmesh.libdtn.data.CanonicalBlock;
import io.left.rightmesh.libdtn.data.BlockHeader;
import io.left.rightmesh.libdtn.data.Bundle;
import io.left.rightmesh.libdtn.data.PrimaryBlock;
import io.left.rightmesh.libdtn.utils.ClockUtil;

/**
 * CoreProcessor processes a Bundle during the different step of its lifetime.
 *
 * @author Lucien Loiseau on 05/09/18.
 */
public class CoreProcessor {

    /**
     * Deserializer MUST call this method to ensure validity of the received PrimaryBlock early on.
     *
     * @param block to test validity
     * @throws RejectedException if the bundle is to be rejected
     */
    public static void onDeserialized(PrimaryBlock block) throws RejectedException {
        if (ClockUtil.isExpired(block.creationTimestamp, block.lifetime)) {
            throw new RejectedException("bundle is expired");
        }

        if (!DTNConfiguration.<Boolean>get(DTNConfiguration.Entry.ENABLE_FORWARD).value()
                && !LocalEIDTable.isLocal(block.destination)) {
            throw new RejectedException("forward isn't enabled and bundle is not local");
        }

        if (DTNConfiguration.<Boolean>get(DTNConfiguration.Entry.EID_SINGLETON_ONLY).value()
                && !block.getV6Flag(PrimaryBlock.BundleV6Flags.DESTINATION_IS_SINGLETON)) {
            throw new RejectedException("bundle is not addressed to a singleton endpoint");
        }

        if (block.lifetime
                > DTNConfiguration.<Integer>get(DTNConfiguration.Entry.MAX_LIFETIME).value()) {
            throw new RejectedException("lifetime is too long");
        }

        if (DTNConfiguration.<Integer>get(
                DTNConfiguration.Entry.MAX_TIMESTAMP_FUTURE).value() > 0
                && block.creationTimestamp > ClockUtil.getCurrentTime()
                        + DTNConfiguration.<Integer>get(
                        DTNConfiguration.Entry.MAX_TIMESTAMP_FUTURE).value()) {
            throw new RejectedException("timestamp too far in the future");
        }
    }

    /**
     * Deserializer MUST call this method to ensure validity of the received BlockHeader.
     *
     * @param block to test validity
     * @throws RejectedException if the bundle is to be rejected
     */
    public static void onDeserialized(BlockHeader block) throws RejectedException {
        if (block.dataSize
                > DTNConfiguration.<Long>get(DTNConfiguration.Entry.LIMIT_BLOCKSIZE).value()) {
            throw new RejectedException("blocksize exceed limit");
        }
    }

    /**
     * Deserializer MUST call this method to ensure validity of the received CanonicalBlock.
     *
     * @param block to test validity
     * @throws RejectedException if the bundle is to be rejected
     */
    public static void onDeserialized(CanonicalBlock block) throws RejectedException {
        try {
            block.onBlockDataDeserialized();
        } catch (ProcessorNotFoundException pne) {
            if (block.getV6Flag(BlockHeader.BlockV6Flags.DELETE_BUNDLE_IF_NOT_PROCESSED)) {
                throw new RejectedException("mandatory block cannot be processed");
            }
        } catch (ProcessingException pe) {
            throw new RejectedException(pe.reason);
        }
    }

    // CHECKSTYLE IGNORE LineLength
    /**
     * This method must be called whenever the bundle is to be actually processed.
     *
     * @param bundle to process
     * @throws RejectedException if the bundle is to be rejected
     */
    public static void onBundleProcessing(Bundle bundle) throws RejectedException {
        boolean reprocess = false;
        while (reprocess) {
            for (CanonicalBlock block : bundle.getBlocks()) {
                try {
                    reprocess |= block.onBundleProcessing(bundle);
                } catch (ProcessorNotFoundException pe) {
                    if (block.getV6Flag(BlockHeader.BlockV6Flags.DELETE_BUNDLE_IF_NOT_PROCESSED)) {
                        throw new RejectedException("");
                    }
                    if (block.getV6Flag(BlockHeader.BlockV6Flags.DISCARD_IF_NOT_PROCESSED)) {
                        bundle.delBlock(block);
                    }
                    if (block.getV6Flag(BlockHeader.BlockV6Flags.TRANSMIT_STATUSREPORT_IF_NOT_PROCESSED)) {
                        // todo create a status report
                    }
                    block.setV6Flag(BlockHeader.BlockV6Flags.FORWARDED_WITHOUT_PROCESSED, true);
                } catch (ProcessingException e) {
                    throw new RejectedException(e.reason);
                }
            }
        }
    }
    // CHECKSTYLE END IGNORE

    /**
     * This method must be called before the bundle is queued for transmission.
     *
     * @param bundle to process
     * @throws RejectedException if the bundle is to be rejected
     */
    public static void onPrepareForTransmission(Bundle bundle) throws RejectedException {
        boolean reprocess = false;
        while (reprocess) {
            for (CanonicalBlock block : bundle.getBlocks()) {
                try {
                    reprocess |= block.onPrepareForTransmission(bundle);
                } catch (ProcessorNotFoundException pe) {
                    // ignore
                } catch (ProcessingException e) {
                    throw new RejectedException(e.reason);
                }
            }
        }
    }

    /**
     * This method must be called before storing the bundle.
     *
     * @param bundle to process
     * @throws RejectedException if the bundle is to be rejected
     */
    public static void onPutOnStorage(Bundle bundle) throws RejectedException {
        boolean reprocess = false;
        while (reprocess) {
            for (CanonicalBlock block : bundle.getBlocks()) {
                try {
                    reprocess |= block.onPrepareForTransmission(bundle);
                } catch (ProcessorNotFoundException pe) {
                    // ignore
                } catch (ProcessingException e) {
                    throw new RejectedException(e.reason);
                }
            }
        }
    }

    /**
     * This method must be called after the bundle is taken from storage.
     *
     * @param bundle to process
     * @throws RejectedException if the bundle is to be rejected
     */
    public static void onPullFromStorage(Bundle bundle) throws RejectedException {
        boolean reprocess = false;
        while (reprocess) {
            for (CanonicalBlock block : bundle.getBlocks()) {
                try {
                    reprocess |= block.onPrepareForTransmission(bundle);
                } catch (ProcessorNotFoundException pe) {
                    // ignore
                } catch (ProcessingException e) {
                    throw new RejectedException(e.reason);
                }
            }
        }

    }
}
