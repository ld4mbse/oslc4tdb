package com.ld4mbse.oslc4tdb.tdb.validation;

import java.util.Objects;
import javax.ws.rs.core.MediaType;

/**
 * Definition of an exception rule that will be throw
 * when a validation exception occurs over a resource or a graph.
 */
public class ValidationException extends IllegalArgumentException {

    /**
     * Determines the message content type.
     */
    private final String messageContentType;

    /**
     * Constructs a instance specifying the error validation message and the
     * message content type.
     * @param message the validation error message.
     * @param messageContentType the validation error message content type.
     */
    public ValidationException(String message, String messageContentType) {
        super(message);
        this.messageContentType = Objects.requireNonNull(messageContentType);
    }

    /**
     * Constructs a instance specifying the error validation message as plain
     * text.
     * @param message the validation error message.
     */
    public ValidationException(String message) {
        this(message, MediaType.TEXT_PLAIN);
    }

    /**
     * Gets the message content type.
     * @return the message content type.
     */
    public String getMessageContentType() {
        return messageContentType;
    }
}