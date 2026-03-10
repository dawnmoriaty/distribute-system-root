package org.example.common;

/**
 * Constants for command types used in the distributed system.
 */
public final class Commands {

    private Commands() {} // Prevent instantiation

    // User commands
    public static final String GET_USER = "GET_USER";
    public static final String GET_ALL_USERS = "GET_ALL_USERS";
    public static final String CREATE_USER = "CREATE_USER";
    public static final String UPDATE_USER = "UPDATE_USER";
    public static final String DELETE_USER = "DELETE_USER";
    public static final String SEARCH_USERS = "SEARCH_USERS";

    // System commands
    public static final String PING = "PING";
    public static final String HEALTH_CHECK = "HEALTH_CHECK";
    public static final String GET_STATS = "GET_STATS";

    // Large data simulation
    public static final String GET_LARGE_DATA = "GET_LARGE_DATA";

    // Admin - Access Control commands
    public static final String ADMIN_BLOCK_IP = "ADMIN_BLOCK_IP";
    public static final String ADMIN_UNBLOCK_IP = "ADMIN_UNBLOCK_IP";
    public static final String ADMIN_WHITELIST_IP = "ADMIN_WHITELIST_IP";
    public static final String ADMIN_REMOVE_WHITELIST_IP = "ADMIN_REMOVE_WHITELIST_IP";
    public static final String ADMIN_LIST_CONNECTIONS = "ADMIN_LIST_CONNECTIONS";
    public static final String ADMIN_KICK_CONNECTION = "ADMIN_KICK_CONNECTION";
    public static final String ADMIN_SET_MODE = "ADMIN_SET_MODE";
    public static final String ADMIN_GET_ACL_STATUS = "ADMIN_GET_ACL_STATUS";
}
