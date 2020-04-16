package com.ld4mbse.oslc4tdb.TDBStore;

import com.ld4mbse.oslc4tdb.services.RDFManager;
import com.ld4mbse.oslc4tdb.services.TDBManager;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;

import java.io.OutputStream;

public class RetrieveStoreInfo {

    private static RDFManager manager = new TDBManager();

    public static void main(String[] args) {

        OutputStream output = System.out;
        String slug = "persons";
        Lang language = RDFLanguages.contentTypeToLang("text/turtle");
        Model model;
        model = manager.getModel(slug, null);
        RDFDataMgr.write(output, model, language) ;

    }
}
