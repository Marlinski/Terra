package io.disruptedsystems.libdtn.core.events;

import io.disruptedsystems.libdtn.core.spi.ActiveRegistrationCallback;

/**
 * RegistrationActive event is thrown whenever an application-agent is active.
 *
 * @author Lucien Loiseau on 10/10/18.
 */
public class RegistrationActive implements DtnEvent {

    public String sink;
    public ActiveRegistrationCallback cb;

    public RegistrationActive(String sink, ActiveRegistrationCallback cb) {
        this.sink = sink;
        this.cb = cb;
    }

    @Override
    public String toString() {
        return "Registration active: sink=" + sink;
    }
}
