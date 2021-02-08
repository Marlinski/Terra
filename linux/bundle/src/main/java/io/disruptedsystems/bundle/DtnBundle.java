package io.disruptedsystems.bundle;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;

import io.disruptedsystems.libdtn.common.BaseExtensionToolbox;
import io.disruptedsystems.libdtn.common.data.BlockHeader;
import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.PayloadBlock;
import io.disruptedsystems.libdtn.common.data.PrimaryBlock;
import io.disruptedsystems.libdtn.common.data.blob.BaseBlobFactory;
import io.disruptedsystems.libdtn.common.data.blob.Blob;
import io.disruptedsystems.libdtn.common.data.blob.VolatileBlob;
import io.disruptedsystems.libdtn.common.data.blob.WritableBlob;
import io.disruptedsystems.libdtn.common.data.bundlev7.parser.BundleV7Item;
import io.disruptedsystems.libdtn.common.data.bundlev7.serializer.BaseBlockDataSerializerFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.serializer.BundleV7Serializer;
import io.marlinski.libcbor.CBOR;
import io.marlinski.libcbor.CborParser;
import io.marlinski.libcbor.rxparser.RxParserException;
import picocli.CommandLine;

@CommandLine.Command(
        name = "bundle", mixinStandardHelpOptions = true, version = "bundle 1.0",
        description = {"bundle is a simple Unix utility to write or check a dtn bundle"},
        optionListHeading = "@|bold %nOptions|@:%n",
        footer = {""})
public class DtnBundle implements Callable<Void> {

    static final String TAG = "dtncat";

    @CommandLine.Option(names = {"-d", "--destination"}, description = "destination eid ")
    private String destination = "dtn://destination/";
    @CommandLine.Option(names = {"-s", "--source"}, description = "destination eid ")
    private String source = "dtn://source/";
    @CommandLine.Option(names = {"-r", "--report"}, description = "report-to eid")
    private String report = "dtn://report/";
    @CommandLine.Option(names = {"-l", "--lifetime"}, description = "lifetime of the bundle")
    private int lifetime = 0;
    @CommandLine.Option(names = {"--crc-16"}, description = "use crc-16")
    private boolean crc16 = false;
    @CommandLine.Option(names = {"--crc-32"}, description = "use crc-32")
    private boolean crc32 = false;
    @CommandLine.Option(names = {"-c", "--check"}, description = "check that input data is bundle")
    private boolean check = false;

    private String fixEid(String eid) {
        if (eid == null) {
            return null;
        }

        if (!eid.startsWith("dtn://")) {
            if (eid.startsWith("/")) {
                return "dtn://api:me" + eid;
            } else {
                return "dtn://api:me/" + eid;
            }
        }
        return eid;
    }

    private VolatileBlob createBlobFromStdin() throws IOException {
        InputStream is = new BufferedInputStream(System.in);
        VolatileBlob blob = new VolatileBlob(100000);
        WritableBlob wblob = blob.getWritableBlob();
        wblob.write(is);
        wblob.close();
        return blob;
    }


    private void createBundleFromStdin() throws URISyntaxException, IOException {
        // create bundle
        URI dest = new URI(fixEid(destination));
        Bundle bundle = new Bundle(dest, lifetime);
        URI reportTo = new URI(fixEid(report));
        bundle.setReportTo(reportTo);
        URI src = new URI(fixEid(source));
        bundle.setSource(src);
        Blob blob = createBlobFromStdin();
        bundle.addBlock(new PayloadBlock(blob));

        if (crc16) {
            bundle.setCrcType(PrimaryBlock.CrcFieldType.CRC_16);
            bundle.getPayloadBlock().crcType = BlockHeader.CrcFieldType.CRC_16;
        }
        if (crc32) {
            bundle.setCrcType(PrimaryBlock.CrcFieldType.CRC_32);
            bundle.getPayloadBlock().crcType = BlockHeader.CrcFieldType.CRC_32;
        }

        // dump bundle
        OutputStream os = new BufferedOutputStream(System.out);
        BundleV7Serializer.encode(bundle, new BaseBlockDataSerializerFactory())
                .observe()
                .subscribe(
                        bb -> {
                            while (bb.hasRemaining()) {
                                int b = bb.get();
                                os.write(b);
                            }
                        }
                );
        os.flush();
        os.close();
    }

    private void checkBundleFromStdin() throws IOException {
        CborParser parser = CBOR.parser().cbor_parse_custom_item(
                () -> new BundleV7Item(
                        new BaseExtensionToolbox(),
                        new BaseBlobFactory().setVolatileMaxSize(100000)),
                (p, t, item) -> {
                });

        try {
            InputStream is = new BufferedInputStream(System.in);
            byte[] buffer = new byte[1024];
            boolean done = false;
            int c = 1;
            while (!done) {
                c = is.read(buffer);
                System.out.println("c=" + c);
                if (c < 0) {
                    break;
                }

                ByteBuffer bb = ByteBuffer.wrap(buffer);
                bb.limit(c);
                done = parser.read(bb);
            }
            System.out.println("valid!");
        } catch (RxParserException e) {
            System.out.println("invalid!");
        }
    }

    @Override
    public Void call() throws Exception {
        if (check) {
            checkBundleFromStdin();
        } else {
            createBundleFromStdin();
        }
        return null;
    }

    public static void main(String[] args) {
        CommandLine.call(new DtnBundle(), args);
    }

}
