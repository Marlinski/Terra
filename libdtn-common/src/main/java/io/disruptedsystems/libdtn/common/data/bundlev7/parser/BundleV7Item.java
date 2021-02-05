package io.disruptedsystems.libdtn.common.data.bundlev7.parser;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborParser;
import io.marlinski.libcbor.rxparser.RxParserException;
import io.disruptedsystems.libdtn.common.ExtensionToolbox;
import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.blob.BlobFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.processor.BlockProcessorFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.processor.ProcessingException;

/**
 * BundleV7Item is a CborParser.ParseableItem for {@link Bundle}.
 *
 * @author Lucien Loiseau on 10/09/18.
 */
public class BundleV7Item implements CborParser.ParseableItem {

    private static final Logger log = LoggerFactory.getLogger(BundleV7Item.class);
    static final String TAG = "BundleV7Item";

    /**
     * A Bundle Item requires a toolbox to be able to parse extension block and
     * extension eid. It also need a BlobFactory to create a new Blob to hold the payload.
     *
     * @param toolbox     for the data structure factory
     * @param blobFactory to create blobs.
     */
    public BundleV7Item(ExtensionToolbox toolbox,
                        BlobFactory blobFactory) {
        this.toolbox = toolbox;
        this.blobFactory = blobFactory;
    }

    public Bundle bundle = null;
    private ExtensionToolbox toolbox;
    private BlobFactory blobFactory;


    @Override
    public CborParser getItemParser() {
        return CBOR.parser()
                .cbor_open_array((parser, tags, size) -> {
                    log.trace( "[+] parsing new bundle");
                })
                .cbor_parse_custom_item(
                        () -> new PrimaryBlockItem(),
                        (parser, tags, item) -> {
                            log.trace( "-> primary block parsed");
                            bundle = item.bundle;
                        })
                .cbor_parse_array_items(
                        () -> new CanonicalBlockItem(toolbox, blobFactory),
                        (parser, tags, item) -> {
                            log.trace( "-> canonical block parsed");

                            /* early validation of block */
                            try {
                                toolbox.getBlockProcessorFactory().create(item.block.type)
                                        .onBlockDeserialized(item.block);
                            } catch (BlockProcessorFactory.ProcessorNotFoundException pnfe) {
                                /* ignore */
                            } catch (ProcessingException pe) {
                                throw new RxParserException(pe.getMessage());
                            }

                            bundle.addBlock(item.block);
                        });
    }
}
