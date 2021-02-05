package io.disruptedsystems.libdtn.common.data.bundlev7.processor;


import io.disruptedsystems.libdtn.common.data.AgeBlock;
import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.CanonicalBlock;
import io.disruptedsystems.libdtn.common.data.FlowLabelBlock;
import io.disruptedsystems.libdtn.common.data.ManifestBlock;
import io.disruptedsystems.libdtn.common.data.PayloadBlock;
import io.disruptedsystems.libdtn.common.data.PreviousNodeBlock;
import io.disruptedsystems.libdtn.common.data.RoutingBlock;
import io.disruptedsystems.libdtn.common.data.HopCountBlock;

import java.util.Arrays;

/**
 * BaseBlockProcessorFactory implements BlockProcessorFactory for all the basic blocks.
 *
 * @author Lucien Loiseau on 26/11/18.
 */
public class BaseBlockProcessorFactory implements BlockProcessorFactory {

    static BlockProcessor nullProcessor = new BlockProcessor() {
        @Override
        public void onBlockDeserialized(CanonicalBlock block) throws ProcessingException {
        }

        @Override
        public boolean onReceptionProcessing(CanonicalBlock block, Bundle bundle)
                throws ProcessingException {
            return false;
        }

        @Override
        public boolean onPutOnStorage(CanonicalBlock block, Bundle bundle)
                throws ProcessingException {
            return false;
        }

        @Override
        public boolean onPullFromStorage(CanonicalBlock block, Bundle bundle)
                throws ProcessingException {
            return false;
        }

        @Override
        public boolean onPrepareForTransmission(CanonicalBlock block, Bundle bundle)
                throws ProcessingException {
            return false;
        }
    };

    static Integer[] basicBlockTypes = {
            PayloadBlock.PAYLOAD_BLOCK_TYPE,
            RoutingBlock.ROUTING_BLOCK_TYPE,
            ManifestBlock.MANIFEST_BLOCK_TYPE,
            FlowLabelBlock.FLOW_LABEL_BLOCK_TYPE,
            PreviousNodeBlock.PREVIOUS_NODE_BLOCK_TYPE,
            AgeBlock.AGE_BLOCK_TYPE,
            HopCountBlock.HOP_COUNT_BLOCK_TYPE
    };

    @Override
    public BlockProcessor create(int type) throws ProcessorNotFoundException {
        if (Arrays.asList(basicBlockTypes).contains(type)) {
            return nullProcessor;
        } else {
            throw new ProcessorNotFoundException();
        }
    }
}
