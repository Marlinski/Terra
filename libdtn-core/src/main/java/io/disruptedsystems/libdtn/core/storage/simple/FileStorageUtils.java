package io.disruptedsystems.libdtn.core.storage.simple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.MetaBundle;
import io.disruptedsystems.libdtn.common.data.blob.BlobFactory;
import io.disruptedsystems.libdtn.common.data.blob.FileBlob;
import io.disruptedsystems.libdtn.common.data.bundlev7.parser.BundleV7Item;
import io.disruptedsystems.libdtn.common.data.bundlev7.parser.PrimaryBlockItem;
import io.disruptedsystems.libdtn.common.data.bundlev7.serializer.BlockDataSerializerFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.serializer.BundleV7Serializer;
import io.disruptedsystems.libdtn.core.api.ExtensionManagerApi;
import io.disruptedsystems.libdtn.core.api.StorageApi;
import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborEncoder;
import io.marlinski.libcbor.CborParser;
import io.marlinski.libcbor.rxparser.RxParserException;

import static io.disruptedsystems.libdtn.common.utils.FileUtil.createFile;
import static io.disruptedsystems.libdtn.common.utils.FileUtil.spaceLeft;
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
     * ========= FILE ===========
     */

    protected static File createBundleFile(File path, String bid) throws StorageApi.StorageFullException {
        if (spaceLeft(path.getAbsolutePath()) > 1000) {
            try {
                String safeBid = bid.replaceAll("/", "_");
                return createFile(
                        "bundle-" + safeBid + ".bundle",
                        path.getAbsolutePath());
            } catch (IOException io) {
                System.out.println("IOException createNewFile: " + io.getMessage() + " : dir="
                        + path + "  bid=" + bid + ".bundle");
            }
        }
        throw new StorageApi.StorageFullException();
    }

    /*
     * ========= PARSERS =========
     */

    public static class MetaBundleFileItem implements CborParser.ParseableItem {

        private static final Logger log = LoggerFactory.getLogger(MetaBundleFileItem.class);

        BundleInfo info = new BundleInfo();
        Collection<CBOR.TextStringItem> tags;
        MetaBundle meta;

        @Override
        public CborParser getItemParser() {
            return CBOR.parser()
                    .cbor_open_array(2)
                    .cbor_open_array(2)
                    .cbor_parse_linear_array(
                            (pos) -> new CBOR.TextStringItem(),
                            (__, ___, col) -> {
                                log.trace(".. tags=" + col.stream().map(CBOR.TextStringItem::value).reduce("", (s, i) -> s + i + " "));
                                tags = col;
                            })
                    .cbor_parse_text_string_full((p, str) -> {
                        log.trace(".. blobPath=" + str);
                        info.blobPath = str;
                    })
                    .cbor_open_array((p, t, s) -> {  //// META BUNDLE
                    })
                    .cbor_parse_custom_item( /* we are just parsing the primary block */
                            PrimaryBlockItem::new,
                            (p, t, item) -> {
                                meta = new MetaBundle(item.bundle);
                                for (CBOR.TextStringItem tag : tags) {
                                    meta.tag(tag.value());
                                }
                            });
        }
    }

    public static class BundleFileItem implements CborParser.ParseableItem {

        private static final Logger log = LoggerFactory.getLogger(BundleFileItem.class);

        ExtensionManagerApi extensions;
        BlobFactory factory;


        BundleFileItem(ExtensionManagerApi extensions, BlobFactory factory) {
            this.extensions = extensions;
            this.factory = factory;
        }

        BundleInfo info = new BundleInfo();
        Collection<CBOR.TextStringItem> tags;
        Bundle bundle;

        @Override
        public CborParser getItemParser() {
            return CBOR.parser()
                    .cbor_open_array(2)
                    .cbor_open_array(2)
                    .cbor_parse_linear_array(
                            (pos) -> new CBOR.TextStringItem(),
                            (__, ___, col) -> {
                                log.trace(".. tags=" + col.stream().map(CBOR.TextStringItem::value).reduce("", (s, i) -> s + i + " "));
                                tags = col;
                            })
                    .cbor_parse_text_string_full((p, str) -> {
                        log.trace(".. blobPath=" + str);
                        info.blobPath = str;
                    })
                    .cbor_parse_custom_item( //// BUNDLE
                            () -> new BundleV7Item(extensions, factory),
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
                                for (CBOR.TextStringItem tag : tags) {
                                    bundle.tag(tag.value());
                                }
                            });
        }
    }

    public static CborParser createBundleParser(ExtensionManagerApi extensions, BlobFactory factory) {
        return CBOR.parser().cbor_parse_custom_item(
                () -> new FileStorageUtils.BundleFileItem(extensions, factory),
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
        Set<String> tags = bundle.getAllTags();
        try {
            return CBOR.encoder()
                    .cbor_start_array(2)  /* File = {header , bundle} */
                    .cbor_start_array(2)  /* header = {tags, blob path} */
                    .cbor_encode_collection(tags)      /* tags */
                    .cbor_encode_text_string(blobPath) /*  blob path */
                    .merge(BundleV7Serializer.encode(bundle, factory)); /* bundle */
        } catch (CBOR.CborEncodingUnknown e) {
            throw new IllegalArgumentException();
        }
    }
}
