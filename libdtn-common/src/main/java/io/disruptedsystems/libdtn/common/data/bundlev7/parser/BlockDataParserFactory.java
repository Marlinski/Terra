package io.disruptedsystems.libdtn.common.data.bundlev7.parser;


import io.disruptedsystems.libdtn.common.data.CanonicalBlock;
import io.disruptedsystems.libdtn.common.data.blob.BlobFactory;
import io.marlinski.libcbor.CborParser;

/**
 * Factory to parse blocks.
 *
 * @author Lucien Loiseau on 21/11/18.
 */
public interface BlockDataParserFactory {

    class UnknownBlockTypeException extends Exception{
    }

    /**
     * returns a parser for newly instantiated ExtensionBlock.
     *
     * @param type block type
     * @param block block to parse
     * @param blobFactory Blob} factory
     * @return CborParser
     * @throws UnknownBlockTypeException if type is unknown
     */
    CborParser create(int type,
                      CanonicalBlock block,
                      BlobFactory blobFactory) throws UnknownBlockTypeException;


}
