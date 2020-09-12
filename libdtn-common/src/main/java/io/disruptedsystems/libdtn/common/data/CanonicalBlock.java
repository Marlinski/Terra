package io.disruptedsystems.libdtn.common.data;

/**
 * Abstract generic CanonicalBlock object.
 *
 * @author Lucien Loiseau on 20/07/18.
 */
public abstract class CanonicalBlock extends BlockHeader {

    protected CanonicalBlock(int type) {
        super(type);
    }

    CanonicalBlock(CanonicalBlock block) {
        super(block);
    }

    /**
     * CanonicalBlock Factory.
     *
     * @param type of the block to create
     * @return an instance of a block for the given PAYLOAD_BLOCK_TYPE
     */
    public static CanonicalBlock create(int type) {
        switch (type) {
            case PayloadBlock.PAYLOAD_BLOCK_TYPE:
                return new PayloadBlock();
            case AgeBlock.AGE_BLOCK_TYPE:
                return new AgeBlock();
            case HopCountBlock.HOP_COUNT_BLOCK_TYPE:
                return new HopCountBlock();
            default:
                return new UnknownExtensionBlock(type);
        }
    }
}
