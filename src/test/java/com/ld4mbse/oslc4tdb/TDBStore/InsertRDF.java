package com.ld4mbse.oslc4tdb.TDBStore;

import com.ld4mbse.oslc4tdb.services.RDFManager;
import com.ld4mbse.oslc4tdb.services.TDBManager;
import com.ld4mbse.oslc4tdb.util.Models;
import com.ld4mbse.oslc4tdb.util.Requests;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;

import java.io.*;
import java.util.Date;

public class InsertRDF {

    private static RDFManager manager = new TDBManager();

    public static void main(String[] args) throws UnsupportedEncodingException {

        String slug = String.valueOf(new Date().getTime());
        String store = "persons";
        String baseURL = "http://localhost:8080/oslc4tdb/rest";
        String storeURL = Requests.buildURI(baseURL, "stores", store);
        String resourceURL = Requests.buildURI(storeURL, slug);

        Lang language = RDFLanguages.contentTypeToLang("application/rdf+xml");
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream model_post = loader.getResourceAsStream("person_extended.rdf");

        Model model = ModelFactory.createDefaultModel();

        String url = Requests.buildURI(baseURL , "resource", "persons", slug) + "/";
        RDFDataMgr.read(model, model_post, url, language);

        HttpPost post = new HttpPost(storeURL);
        StringWriter out = new StringWriter();
        model.write(out, language.getContentType().getContentType());
        StringEntity entity = new StringEntity(out.toString());
        entity.setContentType(language.getContentType().getContentType());
        entity.setContentEncoding("UTF-8");
        post.setEntity(entity);
        post.setHeader("Slug", slug);
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            try (CloseableHttpResponse response = client.execute(post)) {
                int code = response.getStatusLine().getStatusCode();
                String text = EnglishReasonPhraseCatalog.INSTANCE.getReason(code, null);
                PrintStream output = (code > 199 && code < 300 ? System.out : System.err);
                output.print(text + ": ");
                switch(code) {
                    case HttpStatus.SC_CREATED:
                        output.println(response.getFirstHeader("Location").getValue());
                        break;
                    case HttpStatus.SC_NOT_FOUND:
                        output.println(storeURL);
                        break;
                    default:
                        output.println(EntityUtils.toString(response.getEntity()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        OutputStream output = System.out;
        model = manager.getModel(store, Models.getStoreURN(slug));
        RDFDataMgr.write(output, model, language);

    }


}
