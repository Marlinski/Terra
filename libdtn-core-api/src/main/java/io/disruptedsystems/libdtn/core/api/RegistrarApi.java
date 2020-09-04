package io.disruptedsystems.libdtn.core.api;

import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.BundleId;
import io.disruptedsystems.libdtn.core.spi.ActiveRegistrationCallback;
import io.reactivex.rxjava3.core.Flowable;

import java.net.URI;
import java.util.Set;

/**
 * Api to access the services of the Bundle Protocol from an Application Agent.
 *
 * @author Lucien Loiseau on 23/10/18.
 */
public interface RegistrarApi {

    class RegistrarException extends Exception {
        public RegistrarException(String msg) {
            super(msg);
        }
    }

    class BundleMalformed extends RegistrarException {
        public BundleMalformed(String msg) {
            super("bundle is malformed: " + msg);
        }
    }

    class RegistrarDisabled extends RegistrarException {
        public RegistrarDisabled() {
            super("registrar is disabled");
        }
    }

    class EidAlreadyRegistered extends RegistrarException {
        public EidAlreadyRegistered() {
            super("eid is already registered");
        }
    }

    class EidNotRegistered extends RegistrarException {
        public EidNotRegistered() {
            super("eid is not registered");
        }
    }

    class InvalidEid extends RegistrarException {
        public InvalidEid() {
            super("eid argument is invalid");
        }
    }

    class BadCookie extends RegistrarException {
        public BadCookie() {
            super("bad cookie");
        }
    }

    class BundleNotFound extends RegistrarException {
        public BundleNotFound() {
            super("bundle not found");
        }
    }

    class NullArgument extends RegistrarException {
        public NullArgument() {
            super("null argument is forbidden");
        }
    }

    /**
     * Check wether a sink is registered or not.
     *
     * @param eid identifying this AA
     * @return true if the AA is registered, false otherwise
     * @throws RegistrarDisabled if the registrar is disabled
     * @throws NullArgument      if one of the argument is null
     * @throws InvalidEid        if the eid is invalid
     */
    boolean isRegistered(URI eid) throws RegistrarDisabled, InvalidEid, NullArgument;

    /**
     * Register a passive pull-based registration. A cookie is returned that can be used
     * to pull data passively.
     *
     * @param eid to register
     * @return a cookir for this registration upon success, null otherwise
     * @throws RegistrarDisabled     if the registrar is disabled
     * @throws EidAlreadyRegistered if the sink is already registered
     * @throws NullArgument          if one of the argument is null
     * @throws InvalidEid        if the eid is invalid
     */
    String register(URI eid) throws RegistrarDisabled, InvalidEid, EidAlreadyRegistered, NullArgument;

    /**
     * Register an active registration. It fails If the sink is already registered.
     *
     * @param eid  to register
     * @param cb   callback to receive data for this registration
     * @return a cookie for this registration upon success, null otherwise.
     * @throws RegistrarDisabled     if the registrar is disabled
     * @throws EidAlreadyRegistered if the sink is already registered
     * @throws NullArgument          if one of the argument is null
     * @throws InvalidEid        if the eid is invalid
     */
    String register(URI eid, ActiveRegistrationCallback cb)
            throws RegistrarDisabled, InvalidEid, EidAlreadyRegistered, NullArgument;

    /**
     * Unregister an application agent.
     *
     * @param eid    identifying the AA to be unregistered
     * @param cookie cookie for this registration
     * @return true if the AA was unregister, false otherwise
     * @throws RegistrarDisabled if the registrar is disabled
     * @throws EidNotRegistered if the sink to unregister is not register
     * @throws BadCookie         if the cookie provided does not match registration cookie
     * @throws NullArgument      if one of the argument is null
     * @throws InvalidEid        if the eid is invalid
     */
    boolean unregister(URI eid, String cookie)
            throws RegistrarDisabled, InvalidEid, EidNotRegistered, BadCookie, NullArgument;

    /**
     * Allow a registered application-agent to send data using the services of the Bundle Protocol.
     *
     * @param eid    identifying the registered AA.
     * @param cookie cookie for this registration
     * @param bundle to send
     * @return true if the bundle is queued, false otherwise
     * @throws RegistrarDisabled if the registrar is disabled
     * @throws EidNotRegistered if the sink is not register
     * @throws BadCookie         if the cookie provided does not match registration cookie
     * @throws NullArgument      if one of the argument is null
     * @throws BundleMalformed   if the bundle can't be serialized
     * @throws InvalidEid        if the eid is invalid
     */
    boolean send(URI eid, String cookie, Bundle bundle)
            throws RegistrarDisabled, InvalidEid, BadCookie, EidNotRegistered, NullArgument, BundleMalformed;

    /**
     * Allow an anonymous application-agent to send data using the services of the Bundle Protocol.
     *
     * @param bundle to send
     * @return true if the bundle is queued, false otherwise
     * @throws RegistrarDisabled if the registrar is disabled
     * @throws NullArgument      if one of the argument is null
     * @throws BundleMalformed   if the bundle can't be serialized
     */
    boolean send(Bundle bundle)
            throws RegistrarDisabled, NullArgument, BundleMalformed;


    /**
     * Check how many bundles are queued for retrieval for a given sink.
     *
     * @param eid    to check
     * @param cookie that was returned upon registration.
     * @return a list with all the bundle ids.
     * @throws RegistrarDisabled if the registrar is disabled
     * @throws EidNotRegistered if the sink is not register
     * @throws BadCookie         if the cookie provided does not match registration cookie
     * @throws NullArgument      if one of the argument is null
     * @throws InvalidEid        if the eid is invalid
     */
    Set<BundleId> checkInbox(URI eid, String cookie)
            throws RegistrarDisabled, InvalidEid, EidNotRegistered, BadCookie, NullArgument;

    /**
     * get a specific bundle but does not mark it as delivered.
     *
     * @param eid      to check
     * @param cookie   that was returned upon registration.
     * @param bundleId id of the bundle to retrieve
     * @return number of data waiting to be retrieved
     * @throws RegistrarDisabled if the registrar is disabled
     * @throws EidNotRegistered if the sink is not register
     * @throws BadCookie         if the cookie provided does not match registration cookie
     * @throws BundleNotFound    if the bundle was not found
     * @throws NullArgument      if one of the argument is null
     * @throws InvalidEid        if the eid is invalid
     */
    Bundle get(URI eid, String cookie, String bundleId)
            throws RegistrarDisabled, InvalidEid, EidNotRegistered, BadCookie, BundleNotFound, NullArgument;

    /**
     * get a specific bundle and mark it as delivered.
     *
     * @param eid      to check
     * @param cookie   that was returned upon registration.
     * @param bundleId id of the bundle to retrieve
     * @return number of data waiting to be retrieved
     * @throws RegistrarDisabled if the registrar is disabled
     * @throws EidNotRegistered if the sink is not register
     * @throws BadCookie         if the cookie provided does not match registration cookie
     * @throws BundleNotFound    if the bundle was not found
     * @throws NullArgument      if one of the argument is null
     * @throws InvalidEid        if the eid is invalid
     */
    Bundle fetch(URI eid, String cookie, String bundleId)
            throws RegistrarDisabled, InvalidEid, EidNotRegistered, BadCookie, BundleNotFound, NullArgument;

    /**
     * fetch all the bundle from the inbox.
     *
     * @param eid    to check
     * @param cookie that was returned upon registration.
     * @return Flowable of Blob
     * @throws RegistrarDisabled if the registrar is disabled
     * @throws EidNotRegistered if the sink is not register
     * @throws BadCookie         if the cookie provided does not match registration cookie
     * @throws NullArgument      if one of the argument is null
     * @throws InvalidEid        if the eid is invalid
     */
    Flowable<Bundle> fetch(URI eid, String cookie)
            throws RegistrarDisabled, InvalidEid, EidNotRegistered, BadCookie, NullArgument;

    /**
     * Turn a registration active. If the registration was already active it does nothing,
     * otherwise it set the active callbacks of the registration to the one provided as an
     * argument. Fail if the registration is passive but the cookie did not match or the cb is null.
     *
     * @param eid    to the registration
     * @param cookie of the registration
     * @param cb     the callback for the active registration
     * @return true if the registration was successfully activated, false otherwise.
     * @throws RegistrarDisabled if the registrar is disabled
     * @throws EidNotRegistered if the sink is not register
     * @throws BadCookie         if the cookie provided does not match registration cookie
     * @throws NullArgument      if one of the argument is null
     * @throws InvalidEid        if the eid is invalid
     */
    boolean setActive(URI eid, String cookie, ActiveRegistrationCallback cb)
            throws RegistrarDisabled, InvalidEid, EidNotRegistered, BadCookie, NullArgument;

    /**
     * Turn a registration passive. If the registration was already passive it does nothing,
     * Fails if the registration is active but the cookie did not match.
     *
     * @param eid    to the registration
     * @param cookie of the registration
     * @return true if the registration was successfully activated, false otherwise.
     * @throws RegistrarDisabled if the registrar is disabled
     * @throws EidNotRegistered if the sink is not register
     * @throws BadCookie         if the cookie provided does not match registration cookie
     * @throws NullArgument      if one of the argument is null
     * @throws InvalidEid        if the eid is invalid
     */
    boolean setPassive(URI eid, String cookie)
            throws RegistrarDisabled, InvalidEid, EidNotRegistered, BadCookie, NullArgument;

    /**
     * Privileged method! Turn a registration passive. If the registration was already passive it
     * does nothing.
     *
     * @param eid  to the registration
     * @return true if the registration was successfully activated, false otherwise.
     * @throws RegistrarDisabled if the registrar is disabled
     * @throws EidNotRegistered if the sink is not register
     * @throws NullArgument      if one of the argument is null
     * @throws InvalidEid        if the eid is invalid
     */
    boolean setPassive(URI eid)
            throws RegistrarDisabled, InvalidEid, EidNotRegistered, NullArgument;


    //todo: remove this
    String printTable();

}
