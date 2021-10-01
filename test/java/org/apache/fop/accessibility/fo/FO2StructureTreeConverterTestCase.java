/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.accessibility.fo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.custommonkey.xmlunit.Diff;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import static org.junit.Assert.assertTrue;

import org.apache.fop.accessibility.StructureTree2SAXEventAdapter;
import org.apache.fop.accessibility.StructureTreeEventHandler;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.fo.FODocumentParser;
import org.apache.fop.fo.FODocumentParser.FOEventHandlerFactory;
import org.apache.fop.fo.FOEventHandler;
import org.apache.fop.fo.LoadingException;
import org.apache.fop.fotreetest.DummyFOEventHandler;

public class FO2StructureTreeConverterTestCase {

    private interface FOLoader {

        InputStream getFoInputStream();
    }

    private static final String STRUCTURE_TREE_SEQUENCE_NAME = "structure-tree-sequence";

    private FOLoader foLoader;

    @Test
    public void testCompleteDocument() throws Exception {
        foLoader = new FOLoader() {
            public InputStream getFoInputStream() {
                return getResource("/org/apache/fop/fo/complete_document.fo");
            }
        };
        testConverter();
    }

    @Test
    public void testTableFooters() throws Exception {
        foLoader = new FOLoader() {
            public InputStream getFoInputStream() {
                return getResource("table-footers.fo");
            }
        };
        testConverter();
    }

    @Test
    public void testCompleteContentWrappedInTableFooter() throws Exception {
        Source xslt = new StreamSource(getResource("wrapCompleteDocumentInTableFooter.xsl"));
        Transformer transformer = createTransformer(xslt);
        InputStream originalFO = getResource("/org/apache/fop/fo/complete_document.fo");
        ByteArrayOutputStream transformedFoOutput = new ByteArrayOutputStream();
        transformer.transform(new StreamSource(originalFO), new StreamResult(transformedFoOutput));
        final byte[] transformedFoOutputBytes = transformedFoOutput.toByteArray();
        foLoader = new FOLoader() {
            public InputStream getFoInputStream() {
                return new ByteArrayInputStream(transformedFoOutputBytes);
            }
        };
        testConverter();
    }

    @Test
    public void testArtifact() throws Exception {
        foLoader = new FOLoader() {

            public InputStream getFoInputStream() {
                return getResource("artifact.fo");
            }
        };
        testConverter();
    }

    private Transformer createTransformer(Source xslt) throws TransformerFactoryConfigurationError,
            TransformerConfigurationException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        return transformerFactory.newTransformer(xslt);
    }

    private static InputStream getResource(String name) {
        return FO2StructureTreeConverterTestCase.class.getResourceAsStream(name);
    }

    private void testConverter() throws Exception {
        DOMResult expectedStructureTree = loadExpectedStructureTree();
        DOMResult actualStructureTree = buildActualStructureTree();
        final Diff diff = createDiff(expectedStructureTree, actualStructureTree);
        assertTrue(diff.toString(), diff.identical());
    }

    private DOMResult loadExpectedStructureTree() {
        DOMResult expectedStructureTree = new DOMResult();
        InputStream xslt = getResource("fo2StructureTree.xsl");
        runXSLT(xslt, foLoader.getFoInputStream(), expectedStructureTree);
        return expectedStructureTree;
    }

    private static void runXSLT(InputStream xslt, InputStream doc, Result result) {
        Source fo = new StreamSource(doc);
        try {
            Transformer transformer = TransformerFactory.newInstance()
                    .newTransformer(new StreamSource(xslt));
            transformer.transform(fo, result);
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        } finally {
            closeStream(xslt);
            closeStream(doc);
        }
    }

    private static void closeStream(InputStream stream) {
        try {
            stream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private DOMResult buildActualStructureTree() throws Exception {
        DOMResult actualStructureTree = new DOMResult();
        createStructureTreeFromDocument(foLoader.getFoInputStream(), actualStructureTree);
        return actualStructureTree;
    }

    private static void createStructureTreeFromDocument(InputStream foInputStream,
            Result result) throws Exception {
        TransformerHandler tHandler = createTransformerHandler(result);
        startStructureTreeSequence(tHandler);
        StructureTreeEventHandler structureTreeEventHandler
                = StructureTree2SAXEventAdapter.newInstance(tHandler);
        FODocumentParser documentParser = createDocumentParser(structureTreeEventHandler);
        FOUserAgent userAgent = createFOUserAgent(documentParser);
        parseDocument(foInputStream, documentParser, userAgent);
        endStructureTreeSequence(tHandler);
    }

    private static TransformerHandler createTransformerHandler(Result domResult)
            throws TransformerConfigurationException, TransformerFactoryConfigurationError {
        SAXTransformerFactory factory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        TransformerHandler transformerHandler = factory.newTransformerHandler();
        transformerHandler.setResult(domResult);
        return transformerHandler;
    }

    private static void startStructureTreeSequence(TransformerHandler tHandler) throws SAXException {
        tHandler.startDocument();
        tHandler.startElement("", STRUCTURE_TREE_SEQUENCE_NAME, STRUCTURE_TREE_SEQUENCE_NAME,
                new AttributesImpl());
    }

    private static FODocumentParser createDocumentParser(
            final StructureTreeEventHandler structureTreeEventHandler) {
        return FODocumentParser.newInstance(new FOEventHandlerFactory() {
            public FOEventHandler newFOEventHandler(FOUserAgent foUserAgent) {
                return new FO2StructureTreeConverter(structureTreeEventHandler,
                        new DummyFOEventHandler(foUserAgent));
            }
        });
    }

    private static FOUserAgent createFOUserAgent(FODocumentParser documentParser) {
        FOUserAgent userAgent = documentParser.createFOUserAgent();
        userAgent.setAccessibility(true);
        return userAgent;
    }

    private static void parseDocument(InputStream foInputStream, FODocumentParser documentParser,
            FOUserAgent userAgent) throws FOPException, LoadingException {
        try {
            documentParser.parse(foInputStream, userAgent);
        } finally {
            closeStream(foInputStream);
        }
    }

    private static void endStructureTreeSequence(TransformerHandler tHandler) throws SAXException {
        tHandler.endElement("", STRUCTURE_TREE_SEQUENCE_NAME, STRUCTURE_TREE_SEQUENCE_NAME);
        tHandler.endDocument();
    }

    private static Diff createDiff(DOMResult expected, DOMResult actual) {
        Diff diff = new Diff(getDocument(expected), getDocument(actual));
        return diff;
    }

    private static Document getDocument(DOMResult result) {
        return (Document) result.getNode();
    }
}
