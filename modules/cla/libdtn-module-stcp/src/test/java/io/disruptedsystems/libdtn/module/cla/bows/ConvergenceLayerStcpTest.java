package io.disruptedsystems.libdtn.module.cla.bows;

import static junit.framework.TestCase.fail;

import io.disruptedsystems.libdtn.common.utils.NullLogger;
import io.disruptedsystems.libdtn.core.api.ConfigurationApi;
import io.disruptedsystems.libdtn.common.BaseExtensionToolbox;
import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.blob.BaseBlobFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.serializer.BaseBlockDataSerializerFactory;
import io.disruptedsystems.libdtn.common.data.eid.BaseClaEid;
import io.disruptedsystems.libdtn.common.data.eid.EidFormatException;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * Test class for the ConvergenceLayerStcp.
 *
 * @author Lucien Loiseau on 26/09/18.
 */
public class ConvergenceLayerStcpTest {

    ConfigurationApi stcpConf = new ConfigurationApi() {
        @Override
        public <T> EntryInterface<T> get(CoreEntry key) {
            return null;
        }

        @Override
        public EntryInterface<Boolean> getModuleEnabled(String name, boolean defaultValue) {
            return null;
        }

        @Override
        public <T> EntryInterface<T> getModuleConf(String module, String entry, T defaultValue) {
            return new EntryInterface<T>() {
                @Override
                public T value() {
                    return defaultValue;
                }

                @Override
                public Observable<T> observe() {
                    return Observable.just(defaultValue);
                }

                @Override
                public void update(T value) {
                }
            };
        }
    };

    @Test
    public void testServerOneClient() {
        System.out.println("[+] stcp: testing one server and one api");

        CountDownLatch lock = new CountDownLatch(1);

        Bundle[] recv = {null, null, null, null, null, null};
        int[] i = {0};

        BaseClaEid eid = null;
        try {
            eid = new ClaStcpEid("127.0.0.1", 4591, "/test");
        } catch (EidFormatException efe) {
            fail();
        }
        new ConvergenceLayerStcp()
                .setPort(4591)
                .start(stcpConf, new NullLogger())
                .subscribe(
                        channel -> {
                            channel.recvBundle(
                                    new BaseExtensionToolbox(),
                                    new BaseBlobFactory().enableVolatile(1000000)).subscribe(
                                    b -> {
                                        recv[i[0]++] = b;
                                    },
                                    e -> {
                                        lock.countDown();
                                    },
                                    () -> {
                                        lock.countDown();
                                    });
                        },
                        e -> {
                            fail();
                            lock.countDown();
                        });

        new ConvergenceLayerStcp().open(eid)
                .subscribe(
                        dtnChannel -> {
                            Bundle[] bundles = {
                                    TestBundle.testBundle1(),
                                    TestBundle.testBundle2(),
                                    TestBundle.testBundle3(),
                                    TestBundle.testBundle4(),
                                    TestBundle.testBundle5(),
                                    TestBundle.testBundle6()
                            };
                            dtnChannel
                                    .sendBundles(
                                            Flowable.fromArray(bundles),
                                            new BaseBlockDataSerializerFactory())
                                    .subscribe(
                                            j -> {
                                                // ignore
                                            },
                                            e -> {
                                                // ignore
                                            },
                                            dtnChannel::close);
                        },
                        e -> {
                            fail();
                            lock.countDown();
                        });

        try {
            lock.await(2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
            // ignore
        }

        // check payload
        for (int j = 0; j < 6; j++) {
            TestBundle.checkBundlePayload(recv[j]);
        }
    }


    @Test
    public void testServerTenClient() {
        System.out.println("[+] stcp: testing one server and ten clients");

        CountDownLatch lock = new CountDownLatch(10);

        BaseClaEid eid = null;
        try {
            eid = new ClaStcpEid("127.0.0.1", 4592, "/test");
        } catch (EidFormatException efe) {
            fail();
        }
        new ConvergenceLayerStcp()
                .setPort(4592)
                .start(stcpConf, new NullLogger())
                .subscribe(
                        channel -> {
                            channel.recvBundle(
                                    new BaseExtensionToolbox(),
                                    new BaseBlobFactory().enableVolatile(1000000)).subscribe(
                                    TestBundle::checkBundlePayload,
                                    e -> lock.countDown(),
                                    lock::countDown);
                        },
                        e -> {
                            fail();
                            lock.countDown();
                        });

        for (int k = 0; k < 10; k++) {
            new ConvergenceLayerStcp().open(eid)
                    .subscribe(
                            dtnChannel -> {
                                Bundle[] bundles = {
                                        TestBundle.testBundle1(),
                                        TestBundle.testBundle2(),
                                        TestBundle.testBundle3(),
                                        TestBundle.testBundle4(),
                                        TestBundle.testBundle5(),
                                        TestBundle.testBundle6()
                                };
                                dtnChannel
                                        .sendBundles(
                                                Flowable.fromArray(bundles),
                                                new BaseBlockDataSerializerFactory())
                                        .subscribe(
                                                j -> {
                                                    // ignore
                                                },
                                                e -> {
                                                    // ignore
                                                },
                                                dtnChannel::close);
                            },
                            e -> {
                                fail();
                                lock.countDown();
                            });
        }

        try {
            lock.await(2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
            // ignore
        }
    }

}
