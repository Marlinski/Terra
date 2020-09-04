package io.disruptedsystems.dtnping;

import io.disruptedsystems.libdtn.aa.api.ActiveRegistrationCallback;
import io.disruptedsystems.libdtn.aa.api.ApplicationAgentApi;
import io.disruptedsystems.libdtn.aa.ldcp.ApplicationAgent;
import io.disruptedsystems.libdtn.common.BaseExtensionToolbox;
import io.disruptedsystems.libdtn.common.ExtensionToolbox;
import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.PayloadBlock;
import io.disruptedsystems.libdtn.common.data.PrimaryBlock;
import io.disruptedsystems.libdtn.common.data.blob.BaseBlobFactory;
import io.disruptedsystems.libdtn.common.data.blob.BlobFactory;
import io.disruptedsystems.libdtn.common.data.blob.NullBlob;
import io.disruptedsystems.libdtn.common.data.eid.Api;
import io.disruptedsystems.libdtn.common.data.eid.Dtn;
import io.disruptedsystems.libdtn.common.data.eid.Eid;
import io.disruptedsystems.libdtn.common.utils.Log;
import io.disruptedsystems.libdtn.common.utils.SimpleLogger;
import io.reactivex.rxjava3.core.Completable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import picocli.CommandLine;

@CommandLine.Command(
        name = "dtnping", mixinStandardHelpOptions = true, version = "dtnping 1.0",
        //descriptionHeading = "@|bold %nDescription|@:%n",
        description = {
                "dtnping - send ping bundle to dtn node",},
        optionListHeading = "@|bold %nOptions|@:%n",
        footer = {
                ""})
public class DtnPing implements Callable<Void> {

    @CommandLine.Parameters(index = "0", description = "ping the following dtn host.")
    private String dtneid;

    @CommandLine.Option(names = {"-t", "--connect-to"}, description = "connect to dtn "
            + "daemon host IP address (defaut: localhost)")
    private String dtnhost = "127.0.0.1";

    @CommandLine.Option(names = {"-p", "--port"}, description = "connect to dtn daemon TCP "
            + "port, (default: 4557)")
    private int dtnport = 4557;

    @CommandLine.Option(names = {"-n", "--number"}, description = "number of echo request to "
            + "send (default: indefinite)")
    private int number = 10;

    @CommandLine.Option(names = {"-c", "--cookie"}, description = "cookie to reattach to a "
            + "previous ping session")
    private String cookie;

    @CommandLine.Option(names = {"-s", "--sessionId"}, description = "manually set the session ID")
    private String sessionId = null;

    @CommandLine.Option(names = {"-v", "--verbose"}, description = "set the log level to "
            + "debug (-v -vv -vvv).")
    private boolean[] verbose = new boolean[0];

    private ApplicationAgentApi agent;
    private String sink;
    private Log logger;

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

    private static float round(float number, int scale) {
        int pow = 10;
        for (int i = 1; i < scale; i++) {
            pow *= 10;
        }
        float tmp = number * pow;
        return ((float) ((int) ((tmp - (int) tmp) >= 0.5f ? tmp + 1 : tmp))) / pow;
    }

    private void receiveEchoResponse() throws Api.InvalidApiEid, Dtn.InvalidDtnEid, URISyntaxException {
        ActiveRegistrationCallback cb = (recvbundle) ->
                Completable.create(s -> {
                    URI dest = recvbundle.getDestination();

                    final String regex = "/dtnping/([0-9a-fA-F]+)?seq=([0-9]+)&ts=([0-9]+)";
                    Pattern r = Pattern.compile(regex);
                    Matcher m = r.matcher(dest.getPath());
                    if (!m.find()) {
                        System.err.println("received malformed echo response:" + dest);
                        s.onComplete();
                        return;
                    }

                    String eid = m.group(1);
                    String recvSessionId = m.group(2);
                    int seq = Integer.parseInt(m.group(3));
                    long timestamp = Long.parseLong(m.group(4));

                    if (!recvSessionId.equals(sessionId)) {
                        System.err.println("received echo response from another session:"
                                + dest + " session=" + recvSessionId);
                        s.onComplete();
                        return;
                    }

                    long timeElapsed = System.nanoTime() - timestamp;
                    System.err.println("echo response from "
                            + recvbundle.getSource()
                            + " seq=" + seq + " time=" + round((timeElapsed / 1000000.0f), 2)
                            + " ms");

                    s.onComplete();
                });

        ExtensionToolbox toolbox = new BaseExtensionToolbox();
        BlobFactory factory = new BaseBlobFactory().enableVolatile(10000);
        if (cookie == null) {
            agent = ApplicationAgent.create(dtnhost, dtnport, toolbox, factory, logger);
            agent.register(Api.me(sink), cb).subscribe(
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
            agent.reAttach(Api.me(sink), cookie, cb).subscribe(
                    b -> System.err.println("re-attach to registered sink"),
                    e -> {
                        System.err.println("could not re-attach to sink: " + sink + " - "
                                + e.getMessage());
                        System.exit(1);
                    });
        }
    }

    @Override
    public Void call() throws Exception {
        if (sessionId == null) {
            sessionId = Long.toHexString(Double.doubleToLongBits(Math.random()));
        }
        sink = "/dtnping/" + sessionId;

        logger = new SimpleLogger();
        switch (verbose.length) {
            case 0:
                ((SimpleLogger) logger).set(Log.LogLevel.WARN);
                break;
            case 1:
                ((SimpleLogger) logger).set(Log.LogLevel.INFO);
                break;
            case 2:
                ((SimpleLogger) logger).set(Log.LogLevel.DEBUG);
                break;
            default:
                ((SimpleLogger) logger).set(Log.LogLevel.VERBOSE);
        }

        /* register echo response */
        receiveEchoResponse();

        /* set ping destination eid */
        dtneid = fixEid(dtneid);

        /* create ping bundle */
        URI destination = new URI(dtneid);
        Bundle bundle = new Bundle(destination);
        bundle.setSource(Api.me());
        bundle.setV7Flag(PrimaryBlock.BundleV7Flags.DELIVERY_REPORT, true);
        bundle.addBlock(new PayloadBlock(new NullBlob()));

        /* send periodic echo request */
        AtomicInteger seq = new AtomicInteger(0);
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        Runnable sendPing = () -> {
            try {
                /* update ping seq number */
                long timestamp = System.nanoTime();
                URI reportto = Api.me(sink, "seq="+seq.get() + "&ts=" + timestamp,null);
                bundle.setReportTo(reportto);
                bundle.setCreationTimestamp(timestamp);
                agent.send(bundle).subscribe(
                        b -> {
                            if (b) {
                                seq.incrementAndGet();
                            } else {
                                bundle.clearBundle();
                                System.err.println("echo request was refused by " + dtnhost
                                        + ":" + dtnport);
                                System.exit(1);
                            }
                        },
                        e -> {
                            bundle.clearBundle();
                            System.err.println("error: " + e.getMessage());
                            System.exit(1);
                        });
            } catch (URISyntaxException | Dtn.InvalidDtnEid | Api.InvalidApiEid efe) {
                /* ignore */
                System.err.println("eid error: " + efe.getMessage());
                System.exit(1);
            }
            number--;
            if (number <= 0) {
                executor.shutdown();
            }
        };

        System.err.println("pinging " + dtneid);
        executor.scheduleAtFixedRate(sendPing, 0, 1, TimeUnit.SECONDS);

        return null;
    }

    public static void main(String[] args) {
        CommandLine.call(new DtnPing(), args);
    }

}
