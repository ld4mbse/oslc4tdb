package com.ld4mbse.oslc4tdb.TDBStore;

import com.ld4mbse.oslc4tdb.services.RDFManager;
import com.ld4mbse.oslc4tdb.services.TDBManager;
import com.ld4mbse.oslc4tdb.util.Requests;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;


import java.io.*;

public class CreateStore {

    private static RDFManager manager = new TDBManager();

    public static void main(String[] args) throws UnsupportedEncodingException {

        String baseURL = "http://localhost:8080/oslc4tdb/oslc/rdfstores";
        String slug = "persons";
        Lang language = RDFLanguages.contentTypeToLang("text/turtle");

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream model_shacl_post = loader.getResourceAsStream("family.ttl");

        Model model = ModelFactory.createDefaultModel();

        String url = Requests.buildURI(baseURL , slug) + "/";
        RDFDataMgr.read(model, model_shacl_post, url, language);

        HttpPost post = new HttpPost(baseURL);
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
                PrintStream output = (code > 199 && code < 300 ? System.out : System.err);
                switch(code) {
                    case HttpStatus.SC_CREATED:
                        output.println(response.getFirstHeader("Location").getValue());
                        break;
                    case HttpStatus.SC_NOT_FOUND:
                        output.println(baseURL);
                        break;
                    default:
                        output.println(EntityUtils.toString(response.getEntity()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        OutputStream output = System.out;
        model = manager.getModel(slug, null);
        RDFDataMgr.write(output, model, language) ;

    }


}
