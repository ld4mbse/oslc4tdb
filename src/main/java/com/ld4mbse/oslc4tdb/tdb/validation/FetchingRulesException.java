package com.ld4mbse.oslc4tdb.tdb.validation;

/**
 * Definition of an exception rule that will be throw
 * when a request come in and the application is requesting for the
 * rules of the model structure avoiding to insert
 * information with differnt structures.
 */
public class FetchingRulesException extends IllegalStateException {

    public FetchingRulesException() {
        super("Models restrictions are being updated, please try later");
    }

}