package io.disruptedsystems.libdtn.core;

import io.disruptedsystems.libdtn.core.api.ConfigurationApi;
import io.disruptedsystems.libdtn.core.api.CoreComponentApi;
import io.disruptedsystems.libdtn.common.utils.Log;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * CoreComponent is an abstract class that registers to a configuration value and triggers callbacks
 * whenever the configuration changes.
 *
 * @author Lucien Loiseau on 27/09/18.
 */
public abstract class CoreComponent implements CoreComponentApi {

    private boolean enabled = false;
    private static final Set<CoreComponent> REGISTERED_COMPONENTS = new HashSet<>();

    @Override
    public void initComponent(ConfigurationApi.EntryInterface<Boolean> entry, Log logger) {
        REGISTERED_COMPONENTS.add(this);
        entry.observe()
                .subscribe(
                        enabled -> {
                            this.enabled = enabled;
                            if (enabled) {
                                if (logger != null) {
                                    logger.i(getComponentName(), "UP");
                                }
                                componentUp();
                            } else {
                                if (logger != null) {
                                    logger.i(getComponentName(), "DOWN");
                                }
                                componentDown();
                            }
                        },
                        e -> {
                            /* ignore */
                        });
    }

    public static Collection<CoreComponent> getAllComponents() {
        return Collections.unmodifiableCollection(REGISTERED_COMPONENTS);
    }

    public boolean isEnabled() {
        return enabled;
    }

    protected abstract void componentUp();

    protected abstract void componentDown();

    @Override
    public int hashCode() {
        return getComponentName().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof CoreComponent) {
            return this.getComponentName().equals(((CoreComponent) o).getComponentName());
        } else {
            return false;
        }
    }
}
