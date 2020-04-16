package com.ld4mbse.oslc4tdb.rest.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class IllegalStoreExceptionHandler implements ExceptionMapper<IllegalStoreException> {

    @Override
    public Response toResponse(IllegalStoreException e) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(e.getMessage())
                .build();
    }

}
