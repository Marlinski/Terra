package io.disruptedsystems.libdtn.core.utils;

import io.disruptedsystems.libdtn.core.CoreConfiguration;
import io.disruptedsystems.libdtn.core.api.ConfigurationApi;
import io.disruptedsystems.libdtn.common.utils.Log;
import io.disruptedsystems.libdtn.core.CoreComponent;
import io.marlinski.librxbus.RxBus;
import io.marlinski.librxbus.Subscribe;

/**
 * Simple Logger.
 *
 * @author Lucien Loiseau on 15/09/18.
 */
public class Logger extends CoreComponent implements Log {

    private static final String TAG = "Logger";

    /**
     * Constructor.
     *
     * @param conf reference to the core
     */
    public Logger(CoreConfiguration conf) {
        level = LogLevel.INFO;
        conf.<LogLevel>get(ConfigurationApi.CoreEntry.LOG_LEVEL).observe().subscribe(l -> level = l);
        initComponent(conf, ConfigurationApi.CoreEntry.COMPONENT_ENABLE_LOGGING, null);
    }

    @Override
    public String getComponentName() {
        return TAG;
    }

    @Override
    protected void componentUp() {
        RxBus.register(this);
    }

    @Override
    protected void componentDown() {
        RxBus.unregister(this);
    }

    private static LogLevel level;

    private void log(LogLevel l, String tag, String msg) {
        if (isEnabled()) {
            if (l.ordinal() >= level.ordinal()) {
                System.out.println(System.currentTimeMillis() + " "
                        + Thread.currentThread().getName() + " " + l + " - " + tag + ": " + msg);
            }
        }
    }

    public void set(LogLevel level) {
        Logger.level = level;
    }

    @Override
    public void v(String tag, String msg) {
        log(LogLevel.VERBOSE, tag, msg);
    }

    @Override
    public void d(String tag, String msg) {
        log(LogLevel.DEBUG, tag, msg);
    }

    @Override
    public void i(String tag, String msg) {
        log(LogLevel.INFO, tag, msg);
    }

    @Override
    public void w(String tag, String msg) {
        log(LogLevel.WARN, tag, msg);
    }

    @Override
    public void e(String tag, String msg) {
        log(LogLevel.ERROR, tag, msg);
    }

    @Subscribe
    public void onEvent(Object o) {
        d(TAG, "EventReceived - " + o.toString());
    }

}
