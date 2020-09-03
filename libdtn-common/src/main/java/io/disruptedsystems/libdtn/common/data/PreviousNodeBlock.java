package io.disruptedsystems.libdtn.common.data;

import java.net.URI;

import io.disruptedsystems.libdtn.common.data.eid.Dtn;

/**
 * PreviousNodeBlock holds information about the previous node holding this bundle.
 *
 * @author Lucien Loiseau on 17/09/18.
 */
public class PreviousNodeBlock extends CanonicalBlock {

    public static final int PREVIOUS_NODE_BLOCK_TYPE = 7;

    public URI previous;

    public PreviousNodeBlock() {
        super(PREVIOUS_NODE_BLOCK_TYPE);
        previous = Dtn.nullEid();
    }

    public PreviousNodeBlock(URI previous) {
        super(7);
        this.previous = previous;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PreviousNodeBlock");
        if (previous != null) {
            sb.append(": previous node=").append(previous.toString());
        } else {
            sb.append(": previous node is unset");
        }
        return sb.toString();
    }

}

