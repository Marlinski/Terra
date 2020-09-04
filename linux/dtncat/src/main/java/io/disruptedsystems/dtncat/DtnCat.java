package io.disruptedsystems.dtncat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;

import io.disruptedsystems.libdtn.aa.api.ActiveRegistrationCallback;
import io.disruptedsystems.libdtn.aa.api.ApplicationAgentApi;
import io.disruptedsystems.libdtn.aa.ldcp.ApplicationAgent;
import io.disruptedsystems.libdtn.common.BaseExtensionToolbox;
import io.disruptedsystems.libdtn.common.ExtensionToolbox;
import io.disruptedsystems.libdtn.common.data.BlockHeader;
import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.PayloadBlock;
import io.disruptedsystems.libdtn.common.data.PrimaryBlock;
import io.disruptedsystems.libdtn.common.data.blob.BaseBlobFactory;
import io.disruptedsystems.libdtn.common.data.blob.Blob;
import io.disruptedsystems.libdtn.common.data.blob.BlobFactory;
import io.disruptedsystems.libdtn.common.data.blob.WritableBlob;
import io.disruptedsystems.libdtn.common.data.eid.Eid;
import io.disruptedsystems.libdtn.common.utils.Log;
import io.disruptedsystems.libdtn.common.utils.SimpleLogger;
import io.reactivex.rxjava3.core.Completable;
import picocli.CommandLine;

@CommandLine.Command(
        name = "dtncat", mixinStandardHelpOptions = true, version = "dtncat 1.0",
        //descriptionHeading = "@|bold %nDescription|@:%n",
        description = {
                "",
                "dtncat is a simple Unix utility which reads and writes data across network "
                        + "connections over DtnEid protocol. dtncat is an application agent and so "
                        + "requires a full DtnEid node to connect to.",},
        optionListHeading = "@|bold %nOptions|@:%n",
        footer = {
                ""})
public class DtnCat implements Callable<Void> {

    static final String TAG = "DtnCat";

    @CommandLine.Parameters(index = "0", description = "connect to the following DtnEid host.")
    private String dtnhost;

    @CommandLine.Parameters(index = "1", description = "connect to the following DtnEid host.")
    private int dtnport;

    @CommandLine.Option(names = {"-l", "--listen"}, description = "register to an eid and wait "
            + "for bundles.")
    private String eid;

    @CommandLine.Option(names = {"-c", "--cookie"}, description = "supply a cookie for the registration")
    private String cookie;

    @CommandLine.Option(names = {"-R", "--report-to"}, description = "report-to Endpoint-ID (Eid)")
    private String report;

    @CommandLine.Option(names = {"-L", "--lifetime"}, description = "Lifetime of the bundle")
    private int lifetime;

    @CommandLine.Option(names = {"-D", "--destination"}, description = "Destination Endpoint-ID "
            + "(Eid)")
    private String deid;

    @CommandLine.Option(names = {"--crc-16"}, description = "use Crc-16")
    private boolean crc16 = false;

    @CommandLine.Option(names = {"--crc-32"}, description = "use Crc-32")
    private boolean crc32 = false;

    @CommandLine.Option(names = {"-v", "--verbose"},
            description = "set the log level to debug (-v -vv -vvv).")
    private boolean[] verbose = new boolean[0];

    private ApplicationAgentApi agent;
    private ExtensionToolbox toolbox;
    private BlobFactory factory;
    private SimpleLogger logger;

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

    private Bundle createBundleFromStdin(Bundle bundle) throws
            IOException,
            WritableBlob.BlobOverflowException {
        Blob blob;
        try {
            blob = factory.createBlob(-1);
        } catch (BlobFactory.BlobFactoryException boe) {
            throw new WritableBlob.BlobOverflowException();
        }
        WritableBlob wb = blob.getWritableBlob();
        InputStream isr = new BufferedInputStream(System.in);
        wb.write(isr);
        wb.close();
        bundle.addBlock(new PayloadBlock(blob));

        if (crc16) {
            bundle.setCrcType(PrimaryBlock.CrcFieldType.CRC_16);
            bundle.getPayloadBlock().crcType = BlockHeader.CrcFieldType.CRC_16;
        }
        if (crc32) {
            bundle.setCrcType(PrimaryBlock.CrcFieldType.CRC_32);
            bundle.getPayloadBlock().crcType = BlockHeader.CrcFieldType.CRC_32;
        }

        return bundle;
    }

    private void listenBundle() throws URISyntaxException {
        ActiveRegistrationCallback cb = (recvbundle) ->
                Completable.create(s -> {
                    logger.i(TAG, "bundle received from: " + recvbundle.getSource());
                    try {
                        BufferedOutputStream bos = new BufferedOutputStream(System.out);
                        recvbundle.getPayloadBlock().data.observe().subscribe(
                                byteBuffer -> {
                                    while (byteBuffer.hasRemaining()) {
                                        bos.write(byteBuffer.get());
                                    }
                                },
                                e -> {
                                    logger.i(TAG, "error while reading payload: " + e.getMessage());
                                    /* ignore */
                                }
                        );
                        bos.flush();
                        recvbundle.clearBundle();
                        s.onComplete();
                    } catch (IOException io) {
                        s.onError(io);
                    }
                });

        agent = ApplicationAgent.create(dtnhost, dtnport, toolbox, factory, logger);
        if (cookie == null) {
            agent.register(new URI(eid), cb).subscribe(
                    cookie -> {
                        logger.i(TAG, "eid registered. cookie: " + cookie);
                    },
                    e -> {
                        logger.e(TAG, "could not register to eid: " + eid + " - "
                                + e.getMessage());
                        System.exit(1);
                    });
        } else {
            agent = ApplicationAgent.create(dtnhost, dtnport, toolbox, factory);
            agent.reAttach(new URI(eid), cookie, cb).subscribe(
                    b -> logger.i(TAG, "re-attach to registered eid"),
                    e -> {
                        logger.e(TAG, "could not re-attach to eid: " + eid + " - "
                                + e.getMessage());
                        System.exit(1);
                    });
        }
    }

    private void sendBundle() {
        try {
            if (deid == null) {
                throw new IOException("destination must be set");
            }
            URI destination = new URI(deid);
            Bundle bundle = new Bundle(destination, lifetime);
            if (report != null) {
                URI reportTo = new URI(report);
                bundle.setReportTo(reportTo);
            }

            agent = ApplicationAgent.create(dtnhost, dtnport, toolbox, null, logger);
            agent.send(createBundleFromStdin(bundle)).subscribe(
                    isSent -> {
                        if (isSent) {
                            bundle.clearBundle();
                            logger.i(TAG, "bundle successfully sent to " + dtnhost + ":"
                                    + dtnport);
                            System.exit(0);
                        } else {
                            bundle.clearBundle();
                            logger.e(TAG, "bundle was refused by " + dtnhost + ":" + dtnport);
                            System.exit(1);
                        }
                    },
                    err -> {
                        bundle.clearBundle();
                        logger.e(TAG, "error: " + err.getMessage());
                        System.exit(1);
                    });
        } catch (IOException | WritableBlob.BlobOverflowException | URISyntaxException e) {
            /* ignore */
            logger.e(TAG, "sending error: " + e.getMessage());
        }
    }

    @Override
    public Void call() throws Exception {
        eid = fixEid(eid);
        deid = fixEid(deid);
        toolbox = new BaseExtensionToolbox();
        factory = new BaseBlobFactory().enableVolatile(1000000).enablePersistent("./");
        logger = new SimpleLogger();

        switch (verbose.length) {
            case 0:
                logger.set(Log.LogLevel.WARN);
                break;
            case 1:
                logger.set(Log.LogLevel.INFO);
                break;
            case 2:
                logger.set(Log.LogLevel.DEBUG);
                break;
            default:
                logger.set(Log.LogLevel.VERBOSE);
        }

        if (eid != null) {
            listenBundle();
        } else {
            sendBundle();
        }
        return null;
    }

    public static void main(String[] args) {
        CommandLine.call(new DtnCat(), args);
    }

}
