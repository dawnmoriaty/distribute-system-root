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
}
