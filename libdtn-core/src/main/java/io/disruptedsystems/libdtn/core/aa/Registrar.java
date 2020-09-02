package io.disruptedsystems.libdtn.core.aa;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.BundleId;
import io.disruptedsystems.libdtn.common.data.eid.ApiEid;
import io.disruptedsystems.libdtn.common.data.eid.DtnEid;
import io.disruptedsystems.libdtn.common.data.eid.EidFormatException;
import io.disruptedsystems.libdtn.core.CoreComponent;
import io.disruptedsystems.libdtn.core.api.CoreApi;
import io.disruptedsystems.libdtn.core.api.DeliveryApi;
import io.disruptedsystems.libdtn.core.api.LocalEidApi;
import io.disruptedsystems.libdtn.core.api.RegistrarApi;
import io.disruptedsystems.libdtn.core.events.RegistrationActive;
import io.disruptedsystems.libdtn.core.spi.ActiveRegistrationCallback;
import io.disruptedsystems.libdtn.core.storage.EventListener;
import io.marlinski.librxbus.RxBus;
import io.marlinski.librxbus.Subscribe;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

import static io.disruptedsystems.libdtn.core.api.DeliveryApi.DeliveryFailure.Reason.DeliveryDisabled;
import static io.disruptedsystems.libdtn.core.api.DeliveryApi.DeliveryFailure.Reason.DeliveryRefused;
import static io.disruptedsystems.libdtn.core.api.DeliveryApi.DeliveryFailure.Reason.PassiveRegistration;
import static io.disruptedsystems.libdtn.core.api.DeliveryApi.DeliveryFailure.Reason.UnregisteredEid;

/**
 * Registrar Routing keeps track of all the registered application agent.
 *
 * @author Lucien Loiseau on 24/08/18.
 */
public class Registrar extends CoreComponent implements RegistrarApi, DeliveryApi {

    private static final String TAG = "Registrar";

    public static class Registration {
        String registration;
        String cookie;
        ActiveRegistrationCallback cb;

        boolean isActive() {
            return cb != passiveRegistration;
        }

        Registration(String eid, ActiveRegistrationCallback cb) {
            this.registration = eid;
            this.cb = cb;
            this.cookie = UUID.randomUUID().toString();
        }
    }

    private CoreApi core;
    private Map<String, Registration> registrations;
    private DeliveryListener listener;

    /**
     * Constructor.
     *
     * @param core reference to the core
     */
    public Registrar(CoreApi core) {
        this.core = core;
        registrations = new ConcurrentHashMap<>();
        listener = new DeliveryListener(core);
    }

    @Override
    public String getComponentName() {
        return TAG;
    }

    @Override
    protected void componentUp() {
    }

    @Override
    protected void componentDown() {
    }

    /* ---- helper methods ---- */

    private void checkEnable() throws RegistrarDisabled {
        if (!isEnabled()) {
            throw new RegistrarDisabled();
        }
    }

    private void checkArgumentNotNull(Object obj) throws NullArgument {
        if (obj == null) {
            throw new NullArgument();
        }
    }

    private void checkValidEid(String eid) throws InvalidEid {
        try {
            core.getExtensionManager().getEidFactory().create(eid);
        } catch (EidFormatException e) {
            throw new InvalidEid();
        }
    }

    private Registration checkRegisteredSink(String eid)
            throws RegistrarDisabled, InvalidEid, EidNotRegistered, NullArgument {
        checkEnable();
        checkArgumentNotNull(eid);
        checkValidEid(eid);
        Registration registration = registrations.get(eid);
        if (registration == null) {
            throw new EidNotRegistered();
        }
        return registration;
    }

    private Registration checkRegisteredSink(String eid, String cookie)
            throws RegistrarDisabled, InvalidEid, EidNotRegistered, BadCookie, NullArgument {
        checkEnable();
        checkArgumentNotNull(eid);
        checkArgumentNotNull(cookie);
        checkValidEid(eid);
        Registration registration = registrations.get(eid);
        if (registration == null) {
            throw new EidNotRegistered();
        }
        if (!registration.cookie.equals(cookie)) {
            throw new BadCookie();
        }
        return registration;
    }

    private void replaceApiMe(Bundle bundle) {
        if (bundle.getSource() instanceof ApiEid) {
            bundle.setSource(core.getLocalEid().nodeId().copy().copyDemux(
                    ((ApiEid) bundle.getSource())));
        }
        if (bundle.getReportto() instanceof ApiEid) {
            bundle.setReportto(core.getLocalEid().nodeId().copy().copyDemux(
                    ((ApiEid) bundle.getSource())));
        }
        if (bundle.getDestination() instanceof ApiEid) {
            bundle.setDestination(core.getLocalEid().nodeId().copy().copyDemux(
                    ((ApiEid) bundle.getSource())));
        }
    }

    /* ------  RegistrarApi  is the contract facing ApplicationAgentAdapter ------- */

    @Override
    public boolean isRegistered(String eid) throws RegistrarDisabled, InvalidEid, NullArgument {
        checkEnable();
        checkArgumentNotNull(eid);
        checkValidEid(eid);
        return registrations.containsKey(eid);
    }

    @Override
    public String register(String eid)
            throws RegistrarDisabled, InvalidEid, EidAlreadyRegistered, NullArgument {
        return register(eid, passiveRegistration);
    }

    @Override
    public String register(String eid, ActiveRegistrationCallback cb)
            throws RegistrarDisabled, InvalidEid, EidAlreadyRegistered, NullArgument {
        checkEnable();
        checkArgumentNotNull(eid);
        checkArgumentNotNull(cb);
        checkValidEid(eid);

        Registration registration = new Registration(eid, cb);
        if (registrations.putIfAbsent(eid, registration) == null) {
            core.getLogger().i(TAG, "sink registered: " + eid
                    + " (cookie=" + registration.cookie + ") - "
                    + (cb == passiveRegistration ? "passive" : "active"));
            RxBus.post(new RegistrationActive(eid, registration.cb));
            return registration.cookie;
        }

        throw new EidAlreadyRegistered();
    }

    @Override
    public boolean unregister(String eid, String cookie)
            throws RegistrarDisabled, InvalidEid, EidNotRegistered, BadCookie, NullArgument {
        checkRegisteredSink(eid, cookie);

        if (registrations.remove(eid) == null) {
            throw new EidNotRegistered();
        }
        core.getLogger().i(TAG, "sink unregistered: " + eid);
        return true;
    }


    @Override
    public boolean send(Bundle bundle) throws RegistrarDisabled, NullArgument, BundleMalformed {
        checkEnable();
        checkArgumentNotNull(bundle);
        replaceApiMe(bundle);
        core.getBundleProtocol().bundleDispatching(bundle);
        return true;
    }

    @Override
    public boolean send(String eid, String cookie, Bundle bundle)
            throws RegistrarDisabled, InvalidEid, BadCookie, EidNotRegistered, NullArgument, BundleMalformed {
        checkRegisteredSink(eid, cookie);
        checkArgumentNotNull(bundle);
        replaceApiMe(bundle);
        core.getBundleProtocol().bundleDispatching(bundle);
        return true;
    }

    @Override
    public Set<BundleId> checkInbox(String eid, String cookie)
            throws RegistrarDisabled, InvalidEid, BadCookie, EidNotRegistered, NullArgument {
        checkRegisteredSink(eid, cookie);
        // todo: call storage service
        return null;
    }

    @Override
    public Bundle get(String eid, String cookie, String bundleId)
            throws RegistrarDisabled, InvalidEid, BadCookie, EidNotRegistered, NullArgument {
        checkRegisteredSink(eid, cookie);
        checkArgumentNotNull(bundleId);
        // todo: call storage service
        return null;
    }

    @Override
    public Bundle fetch(String eid, String cookie, String bundleId)
            throws RegistrarDisabled, InvalidEid, BadCookie, EidNotRegistered, NullArgument {
        checkRegisteredSink(eid, cookie);
        checkArgumentNotNull(bundleId);
        // todo: call storage service
        return null;
    }

    @Override
    public Flowable<Bundle> fetch(String eid, String cookie)
            throws RegistrarDisabled, InvalidEid, BadCookie, EidNotRegistered, NullArgument {
        checkRegisteredSink(eid, cookie);
        // todo: call storage service
        return null;
    }

    @Override
    public boolean setActive(String eid, String cookie, ActiveRegistrationCallback cb)
            throws RegistrarDisabled, InvalidEid, BadCookie, EidNotRegistered, NullArgument {
        checkArgumentNotNull(cb);
        Registration registration = checkRegisteredSink(eid, cookie);
        registration.cb = cb;
        core.getLogger().i(TAG, "registration active: " + eid);
        RxBus.post(new RegistrationActive(eid, registration.cb));
        return true;
    }

    @Override
    public boolean setPassive(String eid)
            throws RegistrarDisabled, InvalidEid, EidNotRegistered, NullArgument {
        Registration registration = checkRegisteredSink(eid);
        registration.cb = passiveRegistration;
        core.getLogger().i(TAG, "registration passive: " + eid);
        return true;
    }

    @Override
    public boolean setPassive(String eid, String cookie)
            throws RegistrarDisabled, InvalidEid, BadCookie, EidNotRegistered, NullArgument {
        Registration registration = checkRegisteredSink(eid, cookie);
        registration.cb = passiveRegistration;
        core.getLogger().i(TAG, "registration passive: " + eid);
        return true;
    }

    /**
     * print the state of the registration table.
     *
     * @return String
     */
    public String printTable() {
        StringBuilder sb = new StringBuilder("\n\ncurrent registration table:\n");
        sb.append("---------------------------\n\n");
        if (isEnabled()) {
            registrations.forEach(
                    (sink, reg) -> {
                        sb.append(sink).append(" ");
                        if (reg.isActive()) {
                            sb.append("ACTIVE\n");
                        } else {
                            sb.append("PASSIVE\n");
                        }
                    }
            );
        } else {
            sb.append("disabled");
        }
        sb.append("\n");
        return sb.toString();
    }

    /* ------  DeliveryApi is the contract facing CoreApi ------- */

    /**
     * DeliveryListener listen for active registration and forward matching undelivered bundle.
     */
    public class DeliveryListener extends EventListener<String> {
        DeliveryListener(CoreApi core) {
            super(core);
        }

        @Override
        public String getComponentName() {
            return "DeliveryListener";
        }

        /**
         * React to RegistrationActive event and forward the relevent bundles.
         *
         * @param event active registration event
         */
        @Subscribe
        public void onEvent(RegistrationActive event) {
            /* deliver every bundle of interest */
            getBundlesOfInterest(event.eid).subscribe(
                    bundleID -> {
                        /* retrieve the bundle */
                        core.getStorage().get(bundleID).subscribe(
                                /* deliver it */
                                bundle -> event.cb.recv(bundle).subscribe(
                                        () -> {
                                            listener.unwatch(event.eid, bundle.bid);
                                            core.getBundleProtocol()
                                                    .bundleLocalDeliverySuccessful(bundle);
                                        },
                                        e -> core.getBundleProtocol()
                                                .bundleLocalDeliveryFailure(bundle, LocalEidApi.LocalEid.registered(event.eid), e)),
                                e -> {
                                });
                    });
        }
    }

    @Override
    public Completable deliver(LocalEidApi.LocalEid<?> localMatch, Bundle bundle) {
        if (!isEnabled()) {
            return Completable.error(new DeliveryFailure(DeliveryDisabled));
        }

        // first we check if there's an AA that registered to the bundle destination EID.
        Registration reg = registrations.get(bundle.getDestination().getEidString());
        if (reg != null) {
            return deliverToRegistration(reg, bundle);
        }

        // if the device was detected to be local because of a registration, then it must have
        // been unregistered by then (should be very rare).
        if (localMatch instanceof LocalEidApi.Registered) {
            return Completable.error(new DeliveryFailure(UnregisteredEid));
        }

        // this bundle have been detected to be local because it matched
        // against either a node id or a CLA. So last chance is to try swaping with api:me
        // which is only applicable for dtn-eid
        if (!(bundle.getDestination() instanceof DtnEid)) {
            return Completable.error(new DeliveryFailure(UnregisteredEid));
        }

        try {
            ApiEid newEid = new ApiEid(((DtnEid) bundle.getDestination()).getDemux());
            reg = registrations.get(newEid.getEidString());
            if (reg != null) {
                return deliverToRegistration(reg, bundle);
            }
            return Completable.error(new DeliveryFailure(UnregisteredEid));
        } catch (EidFormatException e) {
            /* cannot happen */
            return Completable.error(e);
        }
    }

    private Completable deliverToRegistration(Registration registration, Bundle bundle) {
        if (registration == null) {
            return Completable.error(new DeliveryFailure(UnregisteredEid));
        }
        if (!registration.isActive()) {
            return Completable.error(new DeliveryFailure(PassiveRegistration));
        }
        return registration.cb.recv(bundle)
                .onErrorResumeWith(Completable.error(new DeliveryFailure(DeliveryRefused)));
    }

    @Override
    public void deliverLater(LocalEidApi.LocalEid<?> localMatch, Bundle bundle) {
        listener.watch("todo", bundle.bid);
    }

    /* passive registration */
    private static ActiveRegistrationCallback passiveRegistration
            = (payload) -> Completable.error(new DeliveryFailure(PassiveRegistration));
}
