package org.example.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for client requests.
 * Used for communication between Client -> LoadBalancer -> Worker.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestPayload {

    private String command;
    private String data;
    private String requestId;

    @Builder.Default
    private long timestamp = System.currentTimeMillis();

    public RequestPayload(String command, String data) {
        this.command = command;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public RequestPayload(String command, String data, String requestId) {
        this(command, data);
        this.requestId = requestId;
    }
}
