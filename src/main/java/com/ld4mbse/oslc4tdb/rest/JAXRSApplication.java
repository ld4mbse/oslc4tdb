package com.ld4mbse.oslc4tdb.rest;

import com.ld4mbse.oslc4tdb.model.OSLCModel;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * This REST application definition.
 * @author rherrera
 */
@ApplicationPath(OSLCModel.PATHS.PREFIX)
public class JAXRSApplication extends Application {

}
