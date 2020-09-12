package io.disruptedsystems.libdtn.common.data.bundlev7.processor;

import io.disruptedsystems.libdtn.common.utils.Log;
import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.CanonicalBlock;

/**
 * BlockProcessor describes all the steps in the processing of a block during the lifecycle
 * of a Bundle.
 *
 * @author Lucien Loiseau on 26/11/18.
 */
public interface BlockProcessor {

    /**
     * This is called just before being queued for transmission
     * If it returns true, the whole bundle will be reprocess again.
     *
     * @param block being processed
     * @param bundle being processed
     * @param logger instance.
     * @return true if the bundle needs another processing pass, false otherwise
     * @throws ProcessingException if there is any issue during processings
     */
    boolean onPrepareForTransmission(CanonicalBlock block, Bundle bundle, Log logger)
            throws ProcessingException;

    /**
     * This is called during deserialization.
     *
     * @param block being processed
     * @throws ProcessingException if there is any issue during processing
     */
    void onBlockDeserialized(CanonicalBlock block) throws ProcessingException;

    /**
     * This is called during bundle processing step 5.6.3 when the bundle is being processed by the
     * receiving node. Some block may modify other block and may require the bundle to be reprocess.
     * If it returns true, the whole bundle will be reprocessed.
     *
     * @param block being processed
     * @param bundle being processed
     * @param logger instance.
     * @return true if the bundle needs another processing pass, false otherwise
     * @throws ProcessingException if there is any issue during processing
     */
    boolean onReceptionProcessing(CanonicalBlock block, Bundle bundle, Log logger)
            throws ProcessingException;

    /**
     * This is called when the bundle is being parked into cold storage.
     * If it returns true, the whole bundle will be reprocess again.
     *
     * @param block being processed
     * @param bundle being processed
     * @param logger instance.
     * @return true if the bundle needs another processing pass, false otherwise
     * @throws ProcessingException if there is any issue during processing
     */
    boolean onPutOnStorage(CanonicalBlock block, Bundle bundle, Log logger)
            throws ProcessingException;

    /**
     * This is called when the bundle was pulled from storage.
     * If it returns true, the whole bundle will be reprocess again.
     *
     * @param block being processed
     * @param bundle being processed
     * @param logger instance.
     * @return true if the bundle needs another processing pass, false otherwise
     * @throws ProcessingException if there is any issue during processing
     */
    boolean onPullFromStorage(CanonicalBlock block, Bundle bundle, Log logger)
            throws ProcessingException;

}
