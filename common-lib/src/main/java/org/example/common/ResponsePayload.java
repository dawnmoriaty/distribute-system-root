package org.example.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object for server responses.
 * Used for communication between Worker -> LoadBalancer -> Client.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponsePayload {

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_ERROR = "ERROR";
    public static final String STATUS_NOT_FOUND = "NOT_FOUND";

    private String status;
    private String data;
    private String message;
    private String requestId;
    private String workerId;
    private long processingTimeMs;
    private List<UserDTO> users;

    public ResponsePayload(String status, String data) {
        this.status = status;
        this.data = data;
    }

    public ResponsePayload(String status, String data, String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    // Static factory methods
    public static ResponsePayload success(String data) {
        return new ResponsePayload(STATUS_SUCCESS, data);
    }

    public static ResponsePayload success(String data, String message) {
        return new ResponsePayload(STATUS_SUCCESS, data, message);
    }

    public static ResponsePayload error(String message) {
        return new ResponsePayload(STATUS_ERROR, null, message);
    }

    public static ResponsePayload notFound(String message) {
        return new ResponsePayload(STATUS_NOT_FOUND, null, message);
    }

    public boolean isSuccess() {
        return STATUS_SUCCESS.equals(status);
    }
}
