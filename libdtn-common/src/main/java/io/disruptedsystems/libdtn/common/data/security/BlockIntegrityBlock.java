package io.disruptedsystems.libdtn.common.data.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.marlinski.libcbor.CborEncoder;
import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.CanonicalBlock;
import io.disruptedsystems.libdtn.common.data.bundlev7.serializer.BlockDataSerializerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.crypto.NoSuchPaddingException;

/**
 * BlockIntegrityBlock is an ExtensionBlock for integrity in the bpsec extension.
 *
 * @author Lucien Loiseau on 03/11/18.
 */
public class BlockIntegrityBlock extends AbstractSecurityBlock {

    private static final Logger log = LoggerFactory.getLogger(BlockIntegrityBlock.class);

    public static final int BLOCK_INTEGRITY_BLOCK_TYPE = 193;

    public BlockIntegrityBlock() {
        super(BLOCK_INTEGRITY_BLOCK_TYPE);
    }

    BlockIntegrityBlock(BlockIntegrityBlock bib) {
        super(bib);
    }

    /**
     * select the Digest algorithm.
     *
     * @param cipherSuite digest algorithm to use.
     */
    public void setDigestSuite(CipherSuites cipherSuite) {
        this.cipherSuiteId = cipherSuite.id;
    }

    /**
     * check how this BIB block integrate with another BCB block and throws an exception if the
     * constraints are violated.
     *
     * @param bcb to check against.
     */
    private void checkBcbInteraction(BlockConfidentialityBlock bcb)
            throws ForbiddenOperationException {
        LinkedList<CanonicalBlock> matches = new LinkedList<>();
        for (int st : bcb.securityTargets) {

            /* 3.10 - condition 3 */
            if (this.securityTargets.contains(st)) {
                throw new ForbiddenOperationException();
            }
        }
    }

    @Override
    public void addTo(Bundle bundle) throws ForbiddenOperationException, NoSuchBlockException {
        for (int i : this.securityTargets) {
            if (bundle.getBlock(i) == null) {
                throw new NoSuchBlockException();
            }

            /* 3.10 - cond 6 */
            if (bundle.getBlock(i).type
                    == BlockConfidentialityBlock.BLOCK_CONFIDENTIALITY_BLOCK_TYPE) {
                throw new ForbiddenOperationException();
            }
        }

        for (CanonicalBlock block : bundle.getBlocks()) {
            if (block.type
                    == BlockConfidentialityBlock.BLOCK_CONFIDENTIALITY_BLOCK_TYPE) {
                checkBcbInteraction((BlockConfidentialityBlock) block);
            }
        }
        bundle.addBlock(this);
    }

    /**
     * Apply digest to the targets from the bundle given as an argument.
     *
     * @param bundle            to apply the integrity block to.
     * @param context           security context.
     * @param serializerFactory to serialize the encoded block.
     * @throws SecurityOperationException if there was an issue during encryption.
     */
    public void applyTo(Bundle bundle,
                        SecurityContext context,
                        BlockDataSerializerFactory serializerFactory) throws SecurityOperationException {
        for (int blockNumber : securityTargets) {
            log.trace(".. computing integrity for block number: " + blockNumber);
            CanonicalBlock block = bundle.getBlock(blockNumber);
            LinkedList<SecurityResult> results = new LinkedList<>();
            securityResults.add(results);
            if (block != null) {
                MessageDigest digest;
                try {
                    digest = context.initDigestForIntegrity(
                            this.cipherSuiteId, this.securitySource);
                } catch (SecurityContext.NoSecurityContextFound
                        | NoSuchAlgorithmException
                        | NoSuchPaddingException e) {
                    e.printStackTrace();
                    throw new SecurityOperationException(e.getMessage());
                }

                CborEncoder encoder;
                try {
                    encoder = serializerFactory.create(block);
                } catch (BlockDataSerializerFactory.UnknownBlockTypeException ubte) {
                    throw new SecurityOperationException("target block serializer not found");
                }

                encoder.observe()
                        .subscribe(/* same thread */
                                byteBuffer -> digest.update(byteBuffer.array()),
                                e -> {
                                },
                                () -> {
                                });
                results.add(new IntegrityResult(digest.digest()));
            } else {
                /* should we thrown a NoSuchBlockException ? probably means that it was removed
                   along the way */
            }
        }
    }

    /**
     * Apply digest from the targets from the bundle given as an argument.
     *
     * @param bundle  to apply the security block to.
     * @param context security context.
     * @param serializerFactory to serialize target block.
     * @throws SecurityOperationException if there was an issue during encryption.
     */
    public void applyFrom(Bundle bundle,
                          SecurityContext context,
                          BlockDataSerializerFactory serializerFactory)
            throws SecurityOperationException {
        if (securityResults.size() != securityTargets.size()) {
            throw new SecurityOperationException("There should same number of results "
                    + "as there is targets");
        }

        for (int i = 0; i < securityTargets.size(); i++) {
            int blockNumber = securityTargets.get(i);
            List<SecurityResult> results = securityResults.get(i);

            log.trace(".. checking integrity for block number: " + blockNumber);
            CanonicalBlock block = bundle.getBlock(blockNumber);
            if (block != null) {
                MessageDigest digest;

                try {
                    digest = context.initDigestForVerification(
                            this.cipherSuiteId, this.securitySource);
                    if (results.size() < CipherSuites.expectedResults(this.cipherSuiteId)) {
                        throw new SecurityOperationException("wrong number of result for this "
                                +  "cipherId, id=" + cipherSuiteId + " results=" + results.size());
                    }
                    if (results.get(0).getResultId() != 1) {
                        throw new SecurityOperationException("ResultId should have been 1");
                    }
                } catch (SecurityContext.NoSecurityContextFound
                        | NoSuchAlgorithmException
                        | NoSuchPaddingException e) {
                    e.printStackTrace();
                    throw new SecurityOperationException(e.getMessage());
                }

                IntegrityResult ir = (IntegrityResult) results.get(0);

                CborEncoder encoder;
                try {
                    encoder = serializerFactory.create(block);
                } catch (BlockDataSerializerFactory.UnknownBlockTypeException ubte) {
                    throw new SecurityOperationException("target block serializer not found");
                }
                encoder.observe()
                        .subscribe(/* same thread */
                                byteBuffer -> {
                                    if (byteBuffer.remaining() == byteBuffer.capacity()) {
                                        digest.update(byteBuffer.array());
                                    } else {
                                        byte[] array = new byte[byteBuffer.remaining()];
                                        byteBuffer.get(array);
                                        digest.update(array);
                                    }
                                },
                                e -> {
                                },
                                () -> {
                                });

                byte[] checkDigest = digest.digest();
                if (!Arrays.equals(ir.getChecksum(), checkDigest)) {
                    log.trace(".. integrity failed for target block=" + blockNumber);
                    throw new SecurityOperationException("checksum doesn't match: "
                            + "\nbib_result=" + new String(ir.getChecksum())
                            + "\ndigest=" + new String(checkDigest));
                } else {
                    log.trace(".. integrity ok for target block=" + blockNumber);
                }
            } else {
                /* should we thrown a NoSuchBlockException ? probably means that it was removed
                   along the way */
            }
        }
    }

}
