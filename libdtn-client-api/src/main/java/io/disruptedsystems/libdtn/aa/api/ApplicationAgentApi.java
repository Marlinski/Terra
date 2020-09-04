package io.disruptedsystems.libdtn.aa.api;

import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.BundleId;
import io.reactivex.rxjava3.core.Single;

import java.net.URI;
import java.util.Set;

/**
 * ApplicationAgentApi exposes the public API that the application layer can use to interact
 * with the LibDTN library.
 *
 * @author Lucien Loiseau on 26/10/18.
 */
public interface ApplicationAgentApi {

    class ApplicationAgentException extends Exception {
        public ApplicationAgentException() {
        }

        public ApplicationAgentException(String msg) {
            super(msg);
        }
    }

    class ApplicationAgentDisabled extends ApplicationAgentException {
    }

    class EidAlreadyRegistered extends ApplicationAgentException {
    }

    class RegistrationAlreadyActive extends ApplicationAgentException {
    }

    class EidNotRegistered extends ApplicationAgentException {
    }

    class InvalidEid extends ApplicationAgentException {
    }

    class BadCookie extends ApplicationAgentException {
    }

    class BundleNotFound extends ApplicationAgentException {
    }

    class NullArgument extends ApplicationAgentException {
    }

    /**
     * Check whether an eid is registered or not.
     *
     * @param eid identifying this AA
     * @return true if the AA is registered, false otherwise
     * @rxthrows RegistrarDisabled if the registration service is disabled
     * @rxthrows NullArgument if eid is null
     * @rxthrows InvalidEid if eid is malformed
     */
    Single<Boolean> isRegistered(URI eid);

    /**
     * Register a passive pull-based registration. A cookie is returned that can be used
     * to pull data passively.
     *
     * @param eid to register
     * @return a RegistrationHandler if registered, nu
     * @rxthrows RegistrarDisabled if the registration service is disabled
     * @rxthrows EidAlreadyRegistered if the eid is already registered by another AA
     * @rxthrows NullArgument if eid is null
     * @rxthrows InvalidEid if eid is malformed
     */
    Single<String> register(URI eid);

    /**
     * Register an active registration. The cookie for this registration is returned.
     *
     * @param eid to register
     * @param cb registration callback
     * @return a RegistrationHandler if registered
     * @rxthrows RegistrarDisabled if the registration service is disabled
     * @rxthrows EidAlreadyRegistered if the eid is already registered by another AA
     * @rxthrows NullArgument if eid or the cb is null
     * @rxthrows InvalidEid if eid is malformed
     */
    Single<String> register(URI eid, ActiveRegistrationCallback cb);

    /**
     * Unregister an application agent.
     *
     * @param eid identifying the AA to be unregistered
     * @param cookie cookie for this registration
     * @return true if the AA was unregister, false otherwise
     * @rxthrows RegistrarDisabled if the registration service is disabled
     * @rxthrows EidNotRegistered if the eid is not actually registered
     * @rxthrows BadCookie if the cookie supplied is invalid
     * @rxthrows NullArgument if eid is null or the cookie is null
     * @rxthrows InvalidEid if eid is malformed
     */
    Single<Boolean> unregister(URI eid, String cookie);

    /**
     * Send data using the services of the Bundle Protocol from a registered application-agent.
     *
     * @param eid registered eid for the AA
     * @param cookie for the registration
     * @param bundle to send
     * @return true if the bundle is queued, false otherwise
     * @rxthrows RegistrarDisabled if the registration service is disabled
     * @rxthrows EidNotRegistered if the eid is not actually registered
     * @rxthrows BadCookie if the cookie supplied is invalid
     * @rxthrows NullArgument if eid is null or the cookie is null
     * @rxthrows InvalidEid if eid is malformed
     */
    Single<Boolean> send(URI eid, String cookie, Bundle bundle);

    /**
     * Send data using the services of the Bundle Protocol from an anonymous application-agent.
     *
     * @param bundle to send
     * @return true if the bundle is queued, false otherwise
     */
    Single<Boolean> send(Bundle bundle);

    /**
     * Check how many bundles are queued for retrieval for a given eid.
     *
     * @param eid to check
     * @param cookie that was returned upon registration.
     * @return a list with all the bundle ids.
     * @rxthrows RegistrarDisabled if the registration service is disabled
     * @rxthrows EidNotRegistered if the eid is not actually registered
     * @rxthrows BadCookie if the cookie supplied is invalid
     * @rxthrows NullArgument if eid or the cookie is null
     * @rxthrows InvalidEid if eid is malformed
     */
    Set<BundleId> checkInbox(URI eid, String cookie);

    /**
     * get a specific bundle but does not mark it as delivered.
     *
     * @param eid to check
     * @param cookie that was returned upon registration.
     * @param bundleId id of the bundle to request
     * @return number of data waiting to be retrieved
     * @rxthrows RegistrarDisabled if the registration service is disabled
     * @rxthrows EidNotRegistered if the eid is not actually registered
     * @rxthrows BadCookie if the cookie supplied is invalid
     * @rxthrows BundleNotFound if the bundle does not exist
     * @rxthrows NullArgument if eid or the cookie is null
     * @rxthrows InvalidEid if eid is malformed
     */
    Single<Bundle> get(URI eid, String cookie, BundleId bundleId);

    /**
     * fetch a specific bundle and mark it as delivered.
     *
     * @param eid to check
     * @param cookie that was returned upon registration.
     * @param bundleId id of the bundle to request
     * @return number of data waiting to be retrieved
     * @rxthrows RegistrarDisabled if the registration service is disabled
     * @rxthrows EidNotRegistered if the eid is not actually registered
     * @rxthrows BadCookie if the cookie supplied is invalid
     * @rxthrows BundleNotFound if the bundle does not exist
     * @rxthrows NullArgument if eid or the cookie is null
     * @rxthrows InvalidEid if eid is malformed
     */
    Single<Bundle> fetch(URI eid, String cookie, BundleId bundleId);

    /**
     * Turn a registration active. If the registration was already active it does nothing,
     * otherwise it sets the active callbacks of the registration to the one provided as an
     * argument. Fail if the registration is passive but the cookie did not match or the cb is null.
     *
     * @param eid to the registration
     * @param cookie of the registration
     * @param cb the callback for the active registration
     * @return true if the registration was successfully activated, false otherwise.
     * @rxthrows RegistrarDisabled if the registration service is disabled
     * @rxthrows EidNotRegistered if the eid is not actually registered
     * @rxthrows BadCookie if the cookie supplied is invalid
     * @rxthrows NullArgument if eid or the cookie is null
     * @rxthrows InvalidEid if eid is malformed
     */
    Single<Boolean> reAttach(URI eid, String cookie, ActiveRegistrationCallback cb);

    /**
     * Turn a registration passive. If the registration was already passive it does nothing,
     * Fails if the registration is active but the cookie did not match.
     *
     * @param eid to the registration
     * @param cookie of the registration
     * @return true if the registration was successfully activated, false otherwise.
     * @rxthrows RegistrarDisabled if the registration service is disabled
     * @rxthrows EidNotRegistered if the eid is not actually registered
     * @rxthrows BadCookie if the cookie supplied is invalid
     * @rxthrows NullArgument if eid or the cookie is null
     * @rxthrows InvalidEid if eid is malformed
     */
    Single<Boolean> setPassive(URI eid, String cookie);
}

