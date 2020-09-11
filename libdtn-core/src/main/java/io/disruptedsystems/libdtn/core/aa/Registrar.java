package io.disruptedsystems.libdtn.core.aa;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.eid.Api;
import io.disruptedsystems.libdtn.common.data.eid.Dtn;
import io.disruptedsystems.libdtn.common.data.eid.Eid;
import io.disruptedsystems.libdtn.core.CoreComponent;
import io.disruptedsystems.libdtn.core.api.CoreApi;
import io.disruptedsystems.libdtn.core.api.DeliveryApi;
import io.disruptedsystems.libdtn.core.api.LocalEidApi;
import io.disruptedsystems.libdtn.core.api.RegistrarApi;
import io.disruptedsystems.libdtn.core.events.RegistrationActive;
import io.disruptedsystems.libdtn.core.spi.ActiveRegistrationCallback;
import io.marlinski.librxbus.RxBus;
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
        URI registration;
        String cookie;
        ActiveRegistrationCallback cb;

        boolean isActive() {
            return cb != passiveRegistration;
        }

        Registration(URI eid, ActiveRegistrationCallback cb) {
            this.registration = eid;
            this.cb = cb;
            this.cookie = UUID.randomUUID().toString();
        }
    }

    private CoreApi core;
    private Map<URI, Registration> registrations;

    /**
     * Constructor.
     *
     * @param core reference to the core
     */
    public Registrar(CoreApi core) {
        this.core = core;
        registrations = new ConcurrentHashMap<>();
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

    private Registration checkRegisteredSink(URI eid)
            throws RegistrarDisabled, EidNotRegistered, NullArgument {
        checkEnable();
        checkArgumentNotNull(eid);
        Registration registration = registrations.get(eid);
        if (registration == null) {
            throw new EidNotRegistered();
        }
        return registration;
    }

    private Registration checkRegisteredSink(URI eid, String cookie)
            throws RegistrarDisabled, EidNotRegistered, BadCookie, NullArgument {
        checkEnable();
        checkArgumentNotNull(eid);
        checkArgumentNotNull(cookie);
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
        if (Api.isApiEid(bundle.getSource())) {
            bundle.setSource(Api
                    .swapApiMeUnsafe(bundle.getSource(), core.getLocalEidTable().nodeId()));
        }
        if (Api.isApiEid(bundle.getReportTo())) {
            bundle.setReportTo(Api
                    .swapApiMeUnsafe(bundle.getReportTo(), core.getLocalEidTable().nodeId()));
        }
        if (Api.isApiEid(bundle.getDestination())) {
            bundle.setDestination(Api
                    .swapApiMeUnsafe(bundle.getDestination(), core.getLocalEidTable().nodeId()));
        }
    }

    /* ------  RegistrarApi  is the contract facing ApplicationAgentAdapter ------- */

    @Override
    public boolean isRegistered(URI eid) throws RegistrarDisabled, InvalidEid, NullArgument {
        checkEnable();
        checkArgumentNotNull(eid);
        return registrations.containsKey(eid);
    }

    @Override
    public String register(URI eid)
            throws RegistrarDisabled, InvalidEid, EidAlreadyRegistered, NullArgument {
        return register(eid, passiveRegistration);
    }

    @Override
    public String register(URI eid, ActiveRegistrationCallback cb)
            throws RegistrarDisabled, InvalidEid, EidAlreadyRegistered, NullArgument {
        checkEnable();
        checkArgumentNotNull(eid);
        checkArgumentNotNull(cb);

        Registration registration = new Registration(eid, cb);
        if (registrations.putIfAbsent(Eid.getEndpoint(eid), registration) == null) {
            core.getLogger().i(TAG, "sink registered: " + eid
                    + " (cookie=" + registration.cookie + ") - "
                    + (cb == passiveRegistration ? "passive" : "active"));
            RxBus.post(new RegistrationActive(eid, registration.cb));
            return registration.cookie;
        }

        throw new EidAlreadyRegistered();
    }

    @Override
    public boolean unregister(URI eid, String cookie)
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
    public boolean send(URI eid, String cookie, Bundle bundle)
            throws RegistrarDisabled, InvalidEid, BadCookie, EidNotRegistered, NullArgument, BundleMalformed {
        checkRegisteredSink(eid, cookie);
        checkArgumentNotNull(bundle);
        replaceApiMe(bundle);
        core.getBundleProtocol().bundleDispatching(bundle);
        return true;
    }

    @Override
    public Set<String> checkInbox(URI eid, String cookie)
            throws RegistrarDisabled, InvalidEid, BadCookie, EidNotRegistered, NullArgument {
        checkRegisteredSink(eid, cookie);
        // todo: call storage service
        return null;
    }

    @Override
    public Bundle get(URI eid, String cookie, String bundleId)
            throws RegistrarDisabled, InvalidEid, BadCookie, EidNotRegistered, NullArgument {
        checkRegisteredSink(eid, cookie);
        checkArgumentNotNull(bundleId);
        // todo: call storage service
        return null;
    }

    @Override
    public Bundle fetch(URI eid, String cookie, String bundleId)
            throws RegistrarDisabled, InvalidEid, BadCookie, EidNotRegistered, NullArgument {
        checkRegisteredSink(eid, cookie);
        checkArgumentNotNull(bundleId);
        // todo: call storage service
        return null;
    }

    @Override
    public Flowable<Bundle> fetch(URI eid, String cookie)
            throws RegistrarDisabled, InvalidEid, BadCookie, EidNotRegistered, NullArgument {
        checkRegisteredSink(eid, cookie);
        // todo: call storage service
        return null;
    }

    @Override
    public boolean setActive(URI eid, String cookie, ActiveRegistrationCallback cb)
            throws RegistrarDisabled, InvalidEid, BadCookie, EidNotRegistered, NullArgument {
        checkArgumentNotNull(cb);
        Registration registration = checkRegisteredSink(eid, cookie);
        registration.cb = cb;
        core.getLogger().i(TAG, "registration active: " + eid);
        RxBus.post(new RegistrationActive(eid, registration.cb));
        return true;
    }

    @Override
    public boolean setPassive(URI eid)
            throws RegistrarDisabled, InvalidEid, EidNotRegistered, NullArgument {
        Registration registration = checkRegisteredSink(eid);
        registration.cb = passiveRegistration;
        core.getLogger().i(TAG, "registration passive: " + eid);
        return true;
    }

    @Override
    public boolean setPassive(URI eid, String cookie)
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

    @Override
    public Completable deliver(LocalEidApi.LookUpResult localMatch, Bundle bundle) {
        if (!isEnabled()) {
            return Completable.error(new DeliveryFailure(DeliveryDisabled));
        }

        // first we check if there's an AA that registered to the bundle destination EID.
        Registration reg = registrations.get(Eid.getEndpoint(bundle.getDestination()));
        if (reg != null) {
            return deliverToRegistration(reg, bundle);
        }

        // if the device was detected to be registration local, then the AA must
        // have been unregistered by then (should be very rare).
        if (localMatch == LocalEidApi.LookUpResult.eidMatchAARegistration) {
            return Completable.error(new DeliveryFailure(UnregisteredEid));
        }

        // if the eid is not a dtn we cannot do anything at this point
        if (!Dtn.isDtnEid(bundle.getDestination())) {
            return Completable.error(new DeliveryFailure(UnregisteredEid));
        }

        // some registration may have used the api:me authority so we try to
        // use it and see if it matches
        URI newEid = Api.swapApiMeUnsafe(bundle.getDestination(), Api.me());
        reg = registrations.get(Eid.getEndpoint(newEid));
        if (reg != null) {
            return deliverToRegistration(reg, bundle);
        }

        // todo should we do prefix matching ?

        // we couldn't find a registration
        return Completable.error(new DeliveryFailure(UnregisteredEid));
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
    public void deliverLater(Bundle bundle) {
        // todo
    }

    /* passive registration */
    private static ActiveRegistrationCallback passiveRegistration
            = (payload) -> Completable.error(new DeliveryFailure(PassiveRegistration));
}
