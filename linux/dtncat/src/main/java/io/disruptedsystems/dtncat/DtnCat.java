package io.disruptedsystems.dtncat;

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
import io.disruptedsystems.libdtn.common.data.eid.BaseEidFactory;
import io.disruptedsystems.libdtn.common.data.eid.Eid;
import io.disruptedsystems.libdtn.common.data.eid.EidFormatException;
import io.reactivex.rxjava3.core.Completable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;

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

    @CommandLine.Parameters(index = "0", description = "connect to the following DtnEid host.")
    private String dtnhost;

    @CommandLine.Parameters(index = "1", description = "connect to the following DtnEid host.")
    private int dtnport;

    @CommandLine.Option(names = {"-l", "--listen"}, description = "register to a sink and wait "
            + "for bundles.")
    private String sink;

    @CommandLine.Option(names = {"-c", "--cookie"}, description = "register to a sink and wait "
            + "for bundles.")
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

    private ApplicationAgentApi agent;
    private ExtensionToolbox toolbox;
    private BlobFactory factory;

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

    private void listenBundle() {
        ActiveRegistrationCallback cb = (recvbundle) ->
                Completable.create(s -> {
                    try {
                        BufferedOutputStream bos = new BufferedOutputStream(System.out);
                        recvbundle.getPayloadBlock().data.observe().subscribe(
                                byteBuffer -> {
                                    while (byteBuffer.hasRemaining()) {
                                        bos.write(byteBuffer.get());
                                    }
                                },
                                e -> {
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

        agent = ApplicationAgent.create(dtnhost, dtnport, toolbox, factory);
        if (cookie == null) {
            agent.register(sink, cb).subscribe(
                    cookie -> {
                        System.err.println("sink registered. cookie: " + cookie);
                    },
                    e -> {
                        System.err.println("could not register to sink: " + sink + " - "
                                + e.getMessage());
                        System.exit(1);
                    });
        } else {
            agent = ApplicationAgent.create(dtnhost, dtnport, toolbox, factory);
            agent.reAttach(sink, cookie, cb).subscribe(
                    b -> System.err.println("re-attach to registered sink"),
                    e -> {
                        System.err.println("could not re-attach to sink: " + sink + " - "
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
            Eid destination = new BaseEidFactory().create(deid);
            Bundle bundle = new Bundle(destination, lifetime);
            if (report != null) {
                Eid reportTo = new BaseEidFactory().create(report);
                bundle.setReportto(reportTo);
            }

            agent = ApplicationAgent.create(dtnhost, dtnport, toolbox, null);
            agent.send(createBundleFromStdin(bundle)).subscribe(
                    isSent -> {
                        if (isSent) {
                            bundle.clearBundle();
                            System.err.println("bundle successfully sent to " + dtnhost + ":"
                                    + dtnport);
                            System.exit(0);
                        } else {
                            bundle.clearBundle();
                            System.err.println("bundle was refused by " + dtnhost + ":" + dtnport);
                            System.exit(1);
                        }
                    },
                    err -> {
                        bundle.clearBundle();
                        System.err.println("error: " + err.getMessage());
                        System.exit(1);
                    });
        } catch (IOException | WritableBlob.BlobOverflowException | EidFormatException e) {
            /* ignore */
            System.err.println("error: " + e.getMessage());
        }
    }

    @Override
    public Void call() throws Exception {
        toolbox = new BaseExtensionToolbox();
        factory = new BaseBlobFactory().enableVolatile(1000000).enablePersistent("./");
        if (sink != null) {
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
