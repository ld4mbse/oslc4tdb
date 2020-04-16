package com.ld4mbse.oslc4tdb.rest.exception;

import java.io.Serializable;

public class IllegalStoreException extends IllegalArgumentException implements Serializable {

    public IllegalStoreException() {
        super();
    }

    public IllegalStoreException(String msg)   {
        super(msg);
    }

    public IllegalStoreException(String msg, Exception e)  {
        super(msg, e);
    }

}
