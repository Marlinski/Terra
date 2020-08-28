package io.disruptedsystems.libdtn.common.data.security;

import io.disruptedsystems.libdtn.common.data.BlockBlob;
import io.disruptedsystems.libdtn.common.data.CanonicalBlock;

/**
 * EncryptedBlock is a BlockBlob that holds encrypted data.
 *
 * @author Lucien Loiseau on 04/11/18.
 */
public class EncryptedBlock extends BlockBlob {

    public EncryptedBlock(CanonicalBlock block) {
        super(block);
    }

}
