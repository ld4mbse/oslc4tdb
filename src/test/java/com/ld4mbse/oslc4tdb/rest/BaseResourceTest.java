package com.ld4mbse.oslc4tdb.rest;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;
import org.mockito.ArgumentMatchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BaseResourceTest {

    /**
     * Convenient method to convert a String into a ServletInputStream.
     * @param content the content string to convert.
     * @return the corresponding input stream of {@code content}.
     * @throws IOException if some I/O exception occurs.
     */
    public ServletInputStream getServletInputStream(String content)
            throws IOException {
        final InputStream bytes = new ByteArrayInputStream(content.getBytes());
        ServletInputStream mock = mock(ServletInputStream.class);
        when(mock.read(ArgumentMatchers.any(), anyInt(), anyInt()))
                .thenAnswer(new Answer<Integer>() {
                    @Override
                    public Integer answer(InvocationOnMock invocationOnMock)
                            throws Throwable {
                        Object[] args = invocationOnMock.getArguments();
                        return bytes.read((byte[])args[0], (int)args[1], (int)args[2]);
                    }
                });
        when(mock.read()).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocationOnMock)
                    throws Throwable {
                return bytes.read();
            }
        });
        return mock;
    }

    /**
     * Convenient method to convert a String into a ServletInputStream.
     * @param os the content string to convert.
     * @return the corresponding input stream of {@code content}.
     * @throws IOException if some I/O exception occurs.
     */
    public ServletOutputStream getServletOutputStream(
            final ByteArrayOutputStream os) throws IOException {
        ServletOutputStream mock = mock(ServletOutputStream.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock)
                    throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                os.write((byte[])args[0], (int)args[1], (int)args[2]);
                return null;
            }
        }).when(mock).write(ArgumentMatchers.<byte[]>any(), anyInt(), anyInt());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock)
                    throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                os.write((byte[])args[0]);
                return null;
            }
        }).when(mock).write(ArgumentMatchers.<byte[]>any());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock)
                    throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                os.write((int)args[0]);
                return null;
            }
        }).when(mock).write(anyInt());
        return mock;
    }

    /**
     * Convenient method to create a sample Apache Jena model.
     * @return the model.
     */
    public Model getModel() {
        Model model = ModelFactory.createDefaultModel();
        String me = "http://example.org/me";
        model.add(ResourceFactory.createResource(me), RDF.type, FOAF.Person);
        return model;
    }

    /**
     * Convenient method to create a sample Apache Jena model serialization.
     * @param language the desired output {@link Lang language}.
     * @return the model serialization.
     */
    public String getModelSerialization(Lang language) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        RDFDataMgr.write(baos, getModel(), language);
        return baos.toString();
    }

    public class StubServletOutputStream extends ServletOutputStream {
        public ByteArrayOutputStream baos = new ByteArrayOutputStream();
        public void write(int i) throws IOException {
            baos.write(i);
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {

        }
    }

}
