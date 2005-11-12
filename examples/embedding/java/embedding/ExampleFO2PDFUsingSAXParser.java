/*
 * Copyright 1999-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
 
package embedding;

// Java
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

//JAXP
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;

//SAX
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;

// FOP
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.MimeConstants;

/**
 * This class demonstrates the conversion of an FO file to PDF using FOP.
 * It uses a SAXParser with FOP as the DefaultHandler
 */
public class ExampleFO2PDFUsingSAXParser {

    /**
     * Converts an FO file to a PDF file using FOP
     * @param fo the FO file
     * @param pdf the target PDF file
     * @throws FactoryConfigurationError
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException In case of an I/O problem
     * @throws FOPException In case of a FOP problem
     */
    public void convertFO2PDF(File fo, File pdf)
        throws FactoryConfigurationError,
               ParserConfigurationException,
               FOPException, SAXException, IOException {

        OutputStream out = null;
        
        try {
            // Construct fop and setup output format
            Fop fop = new Fop(MimeConstants.MIME_PDF);
    
            // Setup output stream.  Note: Using BufferedOutputStream
            // for performance reasons (helpful with FileOutputStreams).
            out = new FileOutputStream(pdf);
            out = new BufferedOutputStream(out);
            fop.setOutputStream(out);

            // Setup SAX parser
            // throws FactoryConfigurationError
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            // throws ParserConfigurationException
            SAXParser parser = factory.newSAXParser();
                
            // Obtain FOP's DefaultHandler
            // throws FOPException
            DefaultHandler dh = fop.getDefaultHandler();

            // Start parsing and FOP processing
            // throws SAXException, IOException
            parser.parse(fo, dh);

        } finally {
            out.close();
        }
    }


    /**
     * Main method.
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        try {
            System.out.println("FOP ExampleFO2PDFUsingSAXParser\n");
            System.out.println("Preparing...");
            
            //Setup directories
            File baseDir = new File(".");
            File outDir = new File(baseDir, "out");
            outDir.mkdirs();

            //Setup input and output files            
            File fofile = new File(baseDir, "xml/fo/helloworld.fo");
            File pdffile = new File(outDir, "ResultFO2PDFUsingSAXParser.pdf");

            System.out.println("Input: XSL-FO (" + fofile + ")");
            System.out.println("Output: PDF (" + pdffile + ")");
            System.out.println();
            System.out.println("Transforming...");
            
            ExampleFO2PDFUsingSAXParser app = new ExampleFO2PDFUsingSAXParser();
            app.convertFO2PDF(fofile, pdffile);
            
            System.out.println("Success!");
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }
}

