package io.disruptedsystems.libdtn.core.api;

import io.reactivex.rxjava3.core.Observable;

/**
 * API for the configuration.
 *
 * @author Lucien Loiseau on 26/10/18.
 */
public interface ConfigurationApi {

    /**
     * An observable configuration entry.
     *
     * @param <T> PAYLOAD_BLOCK_TYPE of entry
     */
    interface EntryInterface<T> {
        /**
         * Return the last configured value for this entry.
         *
         * @return value
         */
        T value();

        /**
         * Observe the change of value for this entry.
         *
         * @return Observable of this entry
         */
        Observable<T> observe();

        /**
         * Update the value of this entry.
         *
         * @param value to update
         */
        void update(T value);
    }

    interface EntrySetInterface<T> extends EntryInterface {
        /**
         * Add a new entry to this set.
         *
         * @param value to add
         */
        void add(T value);

        /**
         * delete an entry from this set.
         *
         * @param value to delete
         */
        void del(T value);
    }

    /**
     * An observable configuration entry holding a Map.
     *
     * @param <T> PAYLOAD_BLOCK_TYPE of Map key0
     * @param <U> PAYLOAD_BLOCK_TYPE of Map value
     */
    interface EntryMapInterface<T,U> extends EntryInterface {
        /**
         * Add a new entry to this map.
         *
         * @param key   of the entry
         * @param value of the entry
         */
        void add(T key, U value);

        /**
         * Delete an entry from this map.
         *
         * @param key of the entry to delete
         */
        void del(T key);
    }

    // ---- CONFIGURATION ENTRIES ----
    enum CoreEntry {
        // local
        LOCAL_EID("localEid"),
        ALIASES("aliases"),

        // bundle protocol - validation
        MAX_LIFETIME("max_lifetime"),
        MAX_TIMESTAMP_FUTURE("max_timestamp_future"),
        EID_SINGLETON_ONLY("eid_singleton_only"),
        ALLOW_RECEIVE_ANONYMOUS_BUNDLE("allow_receive_anonymous_bundle"),
        ENABLE_STATUS_REPORTING("enable_status_reporting"),
        ENABLE_FORWARDING("dtn_enable_forwarding"),

        // components
        COMPONENT_ENABLE_CONNECTION_AGENT("component_enable_connection_agent"),
        COMPONENT_ENABLE_EVENT_PROCESSING("component_enable_event_processing"),
        COMPONENT_ENABLE_AA_REGISTRATION("component_enable_aa_registration"),
        COMPONENT_ENABLE_LINKLOCAL_ROUTING("component_enable_linklocal_routing"),
        COMPONENT_ENABLE_ROUTING("component_enable_routing"),
        COMPONENT_ENABLE_STATIC_ROUTING("component_enable_static_routing"),
        COMPONENT_ENABLE_SMART_ROUTING("component_enable_smart_routing"),

        // routing
        STATIC_ROUTE_CONFIGURATION("static_routes_configuration"),
        ENABLE_AUTO_CONNECT_FOR_BUNDLE("dtn_enable_auto_connect_bundle"),
        ENABLE_AUTO_CONNECT_FOR_DETECT_EVENT("dtn_enable_auto_connect_detect"),
        AUTO_CONNECT_USE_WHITELIST("dtn_auto_connect_use_whitelist"),
        AUTO_CONNECT_WHITELIST("dtn_auto_connect_whitelist"),

        // modules extensions
        COMPONENT_ENABLE_MODULE_LOADER("component_enable_module_loader"),
        ENABLE_CLA_MODULES("enable_cla_modules"),
        MODULES_CLA_PATH("modules_cla_path"),
        ENABLE_AA_MODULES("enable_aa_modules"),
        MODULES_AA_PATH("modules_aa_path"),
        ENABLE_CORE_MODULES("enable_core_modules"),
        MODULES_CORE_PATH("modules_core_path"),

        // blob and storage
        COMPONENT_ENABLE_STORAGE("component_enable_storage"),
        PERSISTENCE_STORAGE_PATH("persistence_storage_path"),
        BLOB_VOLATILE_MAX_SIZE("volatile_blob_max_size"),
        LIMIT_BLOCKSIZE("limit_blocksize"),

        // logging
        COMPONENT_ENABLE_LOGGING("component_enable_logging"),
        LOG_LEVEL("log_level"),
        ENABLE_LOG_FILE("enable_log_file"),
        LOG_FILE_PATH("log_file_path");

        private final String key;

        CoreEntry(final String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return key;
        }
    }

    /**
     * get a core configuration entry.
     *
     * @param key of the entry to query
     * @param <T> PAYLOAD_BLOCK_TYPE of the entry
     * @return the queried configuration entry.
     */
    <T> EntryInterface<T> get(CoreEntry key);

    /**
     * get wether a module is enabled or not.
     *
     * @param name of the module
     * @param defaultValue set the default value if not set already
     * @return a Boolean that is true if module is enabled, false otherwise.
     */
    EntryInterface<Boolean> getModuleEnabled(String name, boolean defaultValue);

    /**
     * get a module specific entry.
     *
     * @param moduleName name of the module
     * @param entry name of the entry
     * @param defaultValue set the default value if not set already
     * @param <T> PAYLOAD_BLOCK_TYPE of the entry
     * @return queried entry.
     */
    <T> EntryInterface<T> getModuleConf(String moduleName, String entry, T defaultValue);

}
