package com.example.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Marker interface for DTOs that can be serialized to JSON for Hazelcast caching.
 */
public interface IJsonDto {

    ObjectMapper JSON_MAPPER = new ObjectMapper();

    /**
     * Serialize the current DTO to a JSON string representation.
     *
     * @return JSON payload
     */
    default String getJson() {
        try {
            return JSON_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to serialize payload to JSON", e);
        }
    }
}
