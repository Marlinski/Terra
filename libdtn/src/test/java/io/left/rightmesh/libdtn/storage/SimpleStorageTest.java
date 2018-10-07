package io.left.rightmesh.libdtn.storage;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.left.rightmesh.libdtn.DTNConfiguration;
import io.left.rightmesh.libdtn.data.Bundle;
import io.left.rightmesh.libdtn.data.bundleV7.BundleV7Test;

import static io.left.rightmesh.libdtn.DTNConfiguration.Entry.COMPONENT_ENABLE_SIMPLE_STORAGE;
import static io.left.rightmesh.libdtn.DTNConfiguration.Entry.SIMPLE_STORAGE_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Lucien Loiseau on 04/10/18.
 */
public class SimpleStorageTest {

    @Test
    public void testSimpleStoreBundle() {
        System.out.println("[+] storage: test store bundles in simple storage");

        Set<String> paths = new HashSet<>();
        paths.add(System.getProperty("path"));
        DTNConfiguration.<Boolean>get(COMPONENT_ENABLE_SIMPLE_STORAGE).update(true);
        DTNConfiguration.<Set<String>>get(SIMPLE_STORAGE_PATH).update(paths);
        SimpleStorage.init();

        Bundle[] bundles = {
                BundleV7Test.testBundle1(),
                BundleV7Test.testBundle2(),
                BundleV7Test.testBundle3(),
                BundleV7Test.testBundle4(),
                BundleV7Test.testBundle5(),
                BundleV7Test.testBundle6()
        };

        final AtomicReference<CountDownLatch> lock = new AtomicReference<>(new CountDownLatch(1));

        /* store the bundles in storage */
        clearStorage();
        assertStorageSize(0);


        /* store the bundles in storage */
        lock.set(new CountDownLatch(6));
        for (int i = 0; i < bundles.length; i++) {
            final int j = i;
            SimpleStorage.store(bundles[j]).subscribe(
                    () -> {
                        SimpleStorage.contains(bundles[j].bid).subscribe(
                                b -> {
                                    lock.get().countDown();
                                },
                                e -> {
                                    System.out.println("cannot contain: " + e.getMessage());
                                    lock.get().countDown();
                                });
                    },
                    e -> {
                        System.out.println("error> "+e.getMessage());
                        lock.get().countDown();
                    });
        }
        try {
            lock.get().await(2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
            // ignore
        }
        assertStorageSize(6);

        /* clear the storage */
        clearStorage();
        assertStorageSize(0);
    }

    private void clearStorage() {
        final AtomicReference<CountDownLatch> lock = new AtomicReference<>(new CountDownLatch(1));
        lock.set(new CountDownLatch(1));
        SimpleStorage.clear().subscribe(
                () -> lock.get().countDown(),
                e -> {
                    System.out.println("cannot clear storage: "+e.getMessage());
                    e.printStackTrace();
                    lock.get().countDown();
                });
        try {
            lock.get().await(2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
            // ignore
        }
    }

    private void assertStorageSize(int expectedSize) {
        final AtomicReference<CountDownLatch> lock = new AtomicReference<>(new CountDownLatch(1));
        final AtomicInteger storageSize = new AtomicInteger();
        SimpleStorage.count().subscribe(
                i -> {
                    storageSize.set(i);
                    lock.get().countDown();
                },
                e -> {
                    System.out.println("cannot count: " + e.getMessage());
                });
        try {
            lock.get().await(2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
            // ignore
        }
        assertEquals(expectedSize, storageSize.get());
    }

}