package io.disruptedsystems.libdtn.common.data.bundlev7.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.disruptedsystems.libdtn.common.data.bundlev7.parser.BundleV7Item.TAG;


import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborParser;
import io.disruptedsystems.libdtn.common.data.BlockBlob;
import io.disruptedsystems.libdtn.common.data.blob.BlobFactory;
import io.disruptedsystems.libdtn.common.data.blob.NullBlob;
import io.disruptedsystems.libdtn.common.data.blob.WritableBlob;

import java.io.IOException;

/**
 * Parser for a {@link BlockBlob}.
 *
 * @author Lucien Loiseau on 04/11/18.
 */
public class BlockBlobParser {

    private static final Logger log = LoggerFactory.getLogger(BlockBlobParser.class);

    static CborParser getParser(BlockBlob block, BlobFactory factory) {
        return CBOR.parser()
                .cbor_parse_byte_string(
                        (parser, tags, size) -> {
                            log.trace( ".. blob_byte_string_size=" + size);
                            try {
                                block.data = factory.createBlob((int) size);
                            } catch (IOException e) {
                                log.trace( ".. blob_create=NullBlob");
                                block.data = new NullBlob();
                            }
                            parser.setReg(3, block.data.getWritableBlob());
                        },
                        (p, chunk) -> {
                            log.trace( ".. blob_byte_chunk_size=" + chunk.remaining());
                            log.trace( ".. chunk=" + new String(chunk.array()));
                            try {
                                p.<WritableBlob>getReg(3).write(chunk);
                            } catch (IOException io) {
                                log.trace( ".. blob_write_error=" + io.getMessage());
                                p.<WritableBlob>getReg(3).close();
                                p.setReg(3, null);
                            }
                        },
                        (p) -> {
                            log.trace( ".. blob_byte_string_finish");
                            if (p.getReg(3) != null) {
                                p.<WritableBlob>getReg(3).close();
                            }
                        });
    }

}
