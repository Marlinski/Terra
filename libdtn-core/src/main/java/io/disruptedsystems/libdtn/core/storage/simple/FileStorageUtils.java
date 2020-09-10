package io.disruptedsystems.libdtn.core.storage.simple;

import java.io.IOException;

import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.MetaBundle;
import io.disruptedsystems.libdtn.common.data.blob.BlobFactory;
import io.disruptedsystems.libdtn.common.data.blob.FileBlob;
import io.disruptedsystems.libdtn.common.data.bundlev7.parser.BundleV7Item;
import io.disruptedsystems.libdtn.common.data.bundlev7.parser.PrimaryBlockItem;
import io.disruptedsystems.libdtn.common.data.bundlev7.serializer.BlockDataSerializerFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.serializer.BundleV7Serializer;
import io.disruptedsystems.libdtn.common.utils.Log;
import io.disruptedsystems.libdtn.core.api.ExtensionManagerApi;
import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborEncoder;
import io.marlinski.libcbor.CborParser;
import io.marlinski.libcbor.rxparser.RxParserException;

import static io.disruptedsystems.libdtn.core.storage.simple.SimpleStorage.TAG;

/**
 * @author Lucien Loiseau on 10/09/20.
 */
class FileStorageUtils {

    /*
     * ========== information about the bundle blob ========
     *       ( this is going as attachement in the index)
     */

    public static class BundleInfo {
        public String bundlePath;
        public String blobPath;

        public BundleInfo() {
        }

        public BundleInfo(String bundlePath, String blobPath) {
            this.bundlePath = bundlePath;
            this.blobPath = blobPath;
        }
    }

    /*
     * ========= PARSERS =========
     */

    public static class MetaBundleFileItem implements CborParser.ParseableItem {

        Log logger;
        BundleInfo info = new BundleInfo();
        MetaBundle meta;

        MetaBundleFileItem(Log logger) {
            this.logger = logger;
        }

        @Override
        public CborParser getItemParser() {
            return CBOR.parser()
                    .cbor_open_array(2)
                    .cbor_parse_text_string_full((p, str) -> {
                        logger.v(TAG, ".. blobPath=" + str);
                        info.blobPath = str;
                    })
                    .cbor_open_array((p, t, s) -> {  //// META BUNDLE
                    })
                    .cbor_parse_custom_item( /* we are just parsing the primary block */
                            () -> new PrimaryBlockItem(logger),
                            (p, t, item) -> {
                                meta = new MetaBundle(item.bundle);
                            });
        }
    }

    public static class BundleFileItem implements CborParser.ParseableItem {

        Log logger;
        ExtensionManagerApi extensions;
        BlobFactory factory;


        BundleFileItem(ExtensionManagerApi extensions, BlobFactory factory, Log logger) {
            this.extensions = extensions;
            this.factory = factory;
            this.logger = logger;
        }

        BundleInfo info = new BundleInfo();
        Bundle bundle;

        @Override
        public CborParser getItemParser() {
            return CBOR.parser()
                    .cbor_open_array(2)
                    .cbor_parse_text_string_full((p, str) -> {
                        logger.v(TAG, ".. blobPath=" + str);
                        info.blobPath = str;
                    })
                    .cbor_parse_custom_item( //// BUNDLE
                            () -> new BundleV7Item(logger, extensions, factory),
                            (p, t, item) -> {
                                String path = info.blobPath;
                                try {
                                    item.bundle.getPayloadBlock().data.getWritableBlob().dispose();
                                    item.bundle.getPayloadBlock().data = new FileBlob(path);
                                } catch (IOException io) {
                                    throw new RxParserException("can't retrieve payload blob");
                                }
                                item.bundle.tag("in_storage");
                                bundle = item.bundle;
                            });
        }
    }

    public static CborParser createBundleParser(ExtensionManagerApi extensions, BlobFactory factory, Log logger) {
        return CBOR.parser().cbor_parse_custom_item(
                () -> new FileStorageUtils.BundleFileItem(extensions, factory, logger),
                (p, t, item) -> {
                    p.setReg(1, item.bundle); // ret value
                });
    }

    /*
     * ========= ENCODER =========
     */

    public static CborEncoder bundleFileEncoder(Bundle bundle,
                                                String blobPath,
                                                BlockDataSerializerFactory factory) {
        return CBOR.encoder()
                .cbor_start_array(2)  /* File = {blobpath , bundle} */
                .cbor_encode_text_string(blobPath) /*  blob path */
                .merge(BundleV7Serializer.encode(bundle, factory)); /* bundle */
    }
}
