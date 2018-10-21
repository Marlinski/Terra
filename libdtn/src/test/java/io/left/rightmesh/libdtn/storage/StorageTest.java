package io.left.rightmesh.libdtn.storage;

import org.junit.Test;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.left.rightmesh.libdtn.DTNConfiguration;
import io.left.rightmesh.libdtncommon.data.Bundle;
import io.left.rightmesh.libdtn.storage.bundle.SimpleStorage;
import io.left.rightmesh.libdtn.storage.bundle.Storage;
import io.left.rightmesh.libdtn.storage.bundle.VolatileStorage;
import io.reactivex.Completable;
import io.reactivex.Observable;

import static io.left.rightmesh.libdtn.DTNConfiguration.Entry.COMPONENT_ENABLE_SIMPLE_STORAGE;
import static io.left.rightmesh.libdtn.DTNConfiguration.Entry.COMPONENT_ENABLE_VOLATILE_STORAGE;
import static io.left.rightmesh.libdtn.DTNConfiguration.Entry.SIMPLE_STORAGE_PATH;
import static org.junit.Assert.assertEquals;

/**
 * @author Lucien Loiseau on 14/10/18.
 */
public class StorageTest {

    public static final Object lock = new Object();
    public static final AtomicReference<CountDownLatch> waitLock = new AtomicReference<>(new CountDownLatch(1));


    public void testStoreBundleBothStorage() {
        synchronized (lock) {
            System.out.println("[+] Meta Storage ");
            Storage.getInstance();
            DTNConfiguration.<Boolean>get(COMPONENT_ENABLE_VOLATILE_STORAGE).update(true);
            DTNConfiguration.<Boolean>get(COMPONENT_ENABLE_SIMPLE_STORAGE).update(true);
            Set<String> paths = new HashSet<>();
            paths.add(System.getProperty("path"));
            File dir = new File(System.getProperty("path") + "/bundle/");
            DTNConfiguration.<Set<String>>get(SIMPLE_STORAGE_PATH).update(paths);

            Bundle[] bundles = {
                    TestBundle.testBundle1(),
                    TestBundle.testBundle2(),
                    TestBundle.testBundle3(),
                    TestBundle.testBundle4(),
                    TestBundle.testBundle5(),
                    TestBundle.testBundle6()
            };

            System.out.println("[.] clear Storage");
            cockLock();
            Storage.clear().subscribe(
                    () -> waitLock.get().countDown(),
                    e -> waitLock.get().countDown()
            );
            waitFinish();

            assertEquals(0, Storage.count());
            assertEquals(0, VolatileStorage.count());
            assertEquals(0, SimpleStorage.count());
            SimpleStorageTest.assertFileStorageSize(0, dir);

            System.out.println("[.] store bundle in Storage");

            cockLock();
            Observable.fromArray(bundles).flatMapCompletable(
                    b -> Completable.fromSingle(Storage.store(b)))
                    .subscribe(
                            () -> waitLock.get().countDown(),
                            e -> waitLock.get().countDown());
            waitFinish();

            assertEquals(bundles.length, Storage.count());
            assertEquals(bundles.length, VolatileStorage.count());
            assertEquals(bundles.length, SimpleStorage.count());
            SimpleStorageTest.assertFileStorageSize(bundles.length, dir);

            System.out.println("[.] clear Storage");
            cockLock();
            Storage.clear().subscribe(
                    () -> waitLock.get().countDown(),
                    e -> waitLock.get().countDown()
            );
            waitFinish();

            assertEquals(0, Storage.count());
            assertEquals(0, VolatileStorage.count());
            assertEquals(0, SimpleStorage.count());
            SimpleStorageTest.assertFileStorageSize(0, dir);
        }
    }

    public static void cockLock() {
        waitLock.set(new CountDownLatch(1));
    }

    public static void waitFinish() {
        try {
            waitLock.get().await(2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
            // ignore
        }
    }

}
