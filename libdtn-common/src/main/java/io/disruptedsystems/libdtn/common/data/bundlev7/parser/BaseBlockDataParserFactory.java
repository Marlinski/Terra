package io.disruptedsystems.libdtn.common.data.bundlev7.parser;

import io.disruptedsystems.libdtn.common.data.security.BlockAuthenticationBlock;
import io.disruptedsystems.libdtn.common.data.security.BlockConfidentialityBlock;
import io.disruptedsystems.libdtn.common.data.security.BlockIntegrityBlock;
import io.disruptedsystems.libdtn.common.utils.Log;
import io.marlinski.libcbor.CborParser;
import io.disruptedsystems.libdtn.common.data.AgeBlock;
import io.disruptedsystems.libdtn.common.data.BlockBlob;
import io.disruptedsystems.libdtn.common.data.CanonicalBlock;
import io.disruptedsystems.libdtn.common.data.FlowLabelBlock;
import io.disruptedsystems.libdtn.common.data.ManifestBlock;
import io.disruptedsystems.libdtn.common.data.PayloadBlock;
import io.disruptedsystems.libdtn.common.data.PreviousNodeBlock;
import io.disruptedsystems.libdtn.common.data.RoutingBlock;
import io.disruptedsystems.libdtn.common.data.ScopeControlHopLimitBlock;
import io.disruptedsystems.libdtn.common.data.UnknownExtensionBlock;
import io.disruptedsystems.libdtn.common.data.blob.BlobFactory;

/**
 * BaseBlockDataParserFactory implements BlockDataParserFactory
 * and provides parser for all the basic blocks.
 *
 * @author Lucien Loiseau on 21/11/18.
 */
public class BaseBlockDataParserFactory implements BlockDataParserFactory {

    @Override
    public CborParser create(int type,
                             CanonicalBlock block,
                             BlobFactory blobFactory,
                             Log logger) throws UnknownBlockTypeException {
        switch (type) {
            case PayloadBlock.PAYLOAD_BLOCK_TYPE:
                return BlockBlobParser
                        .getParser((BlockBlob) block, blobFactory, logger);
            case RoutingBlock.ROUTING_BLOCK_TYPE:
                return RoutingBlockParser
                        .getParser((RoutingBlock) block, logger);
            case ManifestBlock.MANIFEST_BLOCK_TYPE:
                return ManifestBlockParser
                        .getParser((ManifestBlock) block, logger);
            case FlowLabelBlock.FLOW_LABEL_BLOCK_TYPE:
                return FlowLabelBlockParser
                        .getParser((FlowLabelBlock) block, logger);
            case PreviousNodeBlock.PREVIOUS_NODE_BLOCK_TYPE:
                return PreviousNodeBlockParser
                        .getParser((PreviousNodeBlock) block, logger);
            case AgeBlock.AGE_BLOCK_TYPE:
                return AgeBlockParser
                        .getParser((AgeBlock) block, logger);
            case ScopeControlHopLimitBlock.SCOPE_CONTROL_HOP_LIMIT_BLOCK_TYPE:
                return ScopeControlHopLimitBlockParser
                        .getParser((ScopeControlHopLimitBlock) block, logger);
            case BlockAuthenticationBlock.BLOCK_AUTHENTICATION_BLOCK_TYPE:
                return SecurityBlockParser
                        .getParser((BlockAuthenticationBlock) block, logger);
            case BlockIntegrityBlock.BLOCK_INTEGRITY_BLOCK_TYPE:
                return SecurityBlockParser
                        .getParser((BlockIntegrityBlock) block, logger);
            case BlockConfidentialityBlock.BLOCK_CONFIDENTIALITY_BLOCK_TYPE:
                return SecurityBlockParser
                        .getParser((BlockConfidentialityBlock) block, logger);
            default:
                if (block instanceof UnknownExtensionBlock) {
                    return BlockBlobParser
                            .getParser((BlockBlob) block, blobFactory, logger);
                } else {
                    throw new UnknownBlockTypeException();
                }
        }
    }

}
