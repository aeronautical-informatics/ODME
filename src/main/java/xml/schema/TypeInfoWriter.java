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
//jdk8x2r jaxp.TypeInfoWriter -xsd11 -i xsd11_datatype_test.xml
package xml.schema;

import org.w3c.dom.TypeInfo;
import org.xml.sax.Attributes;
import org.xml.sax.DTDHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import odme.jtreetograph.JtreeToGraphGeneral;
import odme.odmeeditor.Console;
import odme.odmeeditor.ODMEEditor;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.TypeInfoProvider;
import javax.xml.validation.ValidatorHandler;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Vector;

/**
 * <h1>TypeInfoWriter</h1>
 * <p>
 * Provides a trace of the schema type information for elements and attributes
 * in an XML document. XML instance of the created SES model is validated
 * against an SES XML Schema using this class. Validation result is displayed in
 * the console window of the editor as human readable format.
 * </p>
 *
 * @author Michael Glavassevich, IBM
 * @version $Id: TypeInfoWriter.java 903087 2010-01-26 05:41:00Z mrglavas $
 * @ModifiedBy ---
 */
public class TypeInfoWriter extends DefaultHandler {
    /**
     * Schema full checking feature id
     * (http://apache.org/xml/features/validation/schema-full-checking).
     */
    protected static final String SCHEMA_FULL_CHECKING_FEATURE_ID =
            "http://apache.org/xml/features/validation/schema-full-checking";

    /**
     * Honour all schema locations feature id
     * (http://apache.org/xml/features/honour-all-schemaLocations).
     */
    protected static final String HONOUR_ALL_SCHEMA_LOCATIONS_ID =
            "http://apache.org/xml/features/honour-all-schemaLocations";

    /**
     * Validate schema annotations feature id
     * (http://apache.org/xml/features/validate-annotations)
     */
    protected static final String VALIDATE_ANNOTATIONS_ID =
            "http://apache.org/xml/features/validate-annotations";

    /**
     * Generate synthetic schema annotations feature id
     * (http://apache.org/xml/features/generate-synthetic-annotations).
     */
    protected static final String GENERATE_SYNTHETIC_ANNOTATIONS_ID =
            "http://apache.org/xml/features/generate-synthetic-annotations";

    /**
     * Default schema language (http://www.w3.org/2001/XMLSchema).
     */
    protected static final String DEFAULT_SCHEMA_LANGUAGE = XMLConstants.W3C_XML_SCHEMA_NS_URI;

    /**
     * XSD 1.1 schema language (http://www.w3.org/XML/XMLSchema/v1.1).
     */
    protected static final String XSD11_SCHEMA_LANGUAGE = "http://www.w3.org/XML/XMLSchema/v1.1";

    /**
     * Default parser name (org.apache.xerces.parsers.SAXParser).
     */
    protected static final String DEFAULT_PARSER_NAME = "org.apache.xerces.parsers.SAXParser";

    /**
     * Default schema full checking support (false).
     */
    protected static final boolean DEFAULT_SCHEMA_FULL_CHECKING = false;

    /**
     * Default honour all schema locations (false).
     */
    protected static final boolean DEFAULT_HONOUR_ALL_SCHEMA_LOCATIONS = false;

    /**
     * Default validate schema annotations (false).
     */
    protected static final boolean DEFAULT_VALIDATE_ANNOTATIONS = false;

    /**
     * Default generate synthetic schema annotations (false).
     */
    protected static final boolean DEFAULT_GENERATE_SYNTHETIC_ANNOTATIONS = false;

    /**
     * TypeInfo provider.
     */
    protected TypeInfoProvider fTypeInfoProvider;

    /**
     * Print writer.
     */
    protected PrintWriter fOut;

    /**
     * Indent level.
     */
    protected int fIndent;

    public static void validateXML() {
        XMLReader parser = null;
        Vector<String> instances = null;
        boolean schemaFullChecking = DEFAULT_SCHEMA_FULL_CHECKING;
        boolean honourAllSchemaLocations = DEFAULT_HONOUR_ALL_SCHEMA_LOCATIONS;
        boolean validateAnnotations = DEFAULT_VALIDATE_ANNOTATIONS;
        boolean generateSyntheticAnnotations = DEFAULT_GENERATE_SYNTHETIC_ANNOTATIONS;

        // process -i: instance documents
        if (instances == null) {
            instances = new Vector<String>();
        }

        if (ODMEEditor.sesValidationControl == 1) {
        	String path = new String();
        	if (ODMEEditor.toolMode == "ses")
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/xmlforxsd.xml";
        	else
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario + "/xmlforxsd.xml";
        	
            instances.add(path);
            ODMEEditor.sesValidationControl = 0;
        } else {
            String rootNodeName = JtreeToGraphGeneral.rootNodeName();
            
            String path = new String();
        	if (ODMEEditor.toolMode == "ses")
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/" + rootNodeName + ".xml";
        	else
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario + "/" + rootNodeName + ".xml";
        	
            instances.add(path);
        }

        // use default parser?
        if (parser == null) {
            // create parser
            try {
                parser = XMLReaderFactory.createXMLReader(DEFAULT_PARSER_NAME);
            } 
            catch (Exception e) {
                Console.addConsoleOutput("error: Unable to instantiate parser (" + DEFAULT_PARSER_NAME + ")");
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }

        try {
            // Create writer
            TypeInfoWriter writer = new TypeInfoWriter();
            writer.setOutput(System.out, "UTF8");

            // Create SchemaFactory and configure
            SchemaFactory factory = SchemaFactory.newInstance(DEFAULT_SCHEMA_LANGUAGE);
            factory.setErrorHandler(writer);

            try {
                factory.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, schemaFullChecking);
            } 
            catch (SAXNotRecognizedException e) {
                Console.addConsoleOutput("warning: SchemaFactory does not recognize feature ("
                                         + SCHEMA_FULL_CHECKING_FEATURE_ID + ")");
            } 
            catch (SAXNotSupportedException e) {
                Console.addConsoleOutput(
                        "warning: SchemaFactory does not support feature (" + SCHEMA_FULL_CHECKING_FEATURE_ID
                        + ")");
            }
            try {
                factory.setFeature(HONOUR_ALL_SCHEMA_LOCATIONS_ID, honourAllSchemaLocations);
            } 
            catch (SAXNotRecognizedException e) {
                Console.addConsoleOutput(
                        "warning: SchemaFactory does not recognize feature (" + HONOUR_ALL_SCHEMA_LOCATIONS_ID
                        + ")");
            } 
            catch (SAXNotSupportedException e) {
                Console.addConsoleOutput(
                        "warning: SchemaFactory does not support feature (" + HONOUR_ALL_SCHEMA_LOCATIONS_ID
                        + ")");
            }
            try {
                factory.setFeature(VALIDATE_ANNOTATIONS_ID, validateAnnotations);
            } 
            catch (SAXNotRecognizedException e) {
                System.err.println(
                        "warning: SchemaFactory does not recognize feature (" + VALIDATE_ANNOTATIONS_ID
                        + ")");
            } 
            catch (SAXNotSupportedException e) {
                Console.addConsoleOutput(
                        "warning: SchemaFactory does not support feature (" + VALIDATE_ANNOTATIONS_ID + ")");
            }
            try {
                factory.setFeature(GENERATE_SYNTHETIC_ANNOTATIONS_ID, generateSyntheticAnnotations);
            } 
            catch (SAXNotRecognizedException e) {
                Console.addConsoleOutput("warning: SchemaFactory does not recognize feature ("
                                         + GENERATE_SYNTHETIC_ANNOTATIONS_ID + ")");
            } 
            catch (SAXNotSupportedException e) {
                Console.addConsoleOutput("warning: SchemaFactory does not support feature ("
                                         + GENERATE_SYNTHETIC_ANNOTATIONS_ID + ")");
            }

            // Build Schema from sources
            Schema schema;
            schema = factory.newSchema();

            // Setup validator and parser
            ValidatorHandler validator = schema.newValidatorHandler();
            parser.setContentHandler(validator);
            if (validator instanceof DTDHandler) {
                parser.setDTDHandler((DTDHandler) validator);
            }
            parser.setErrorHandler(writer);
            validator.setContentHandler(writer);
            validator.setErrorHandler(writer);
            writer.setTypeInfoProvider(validator.getTypeInfoProvider());

            try {
                validator.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, schemaFullChecking);
            } 
            catch (SAXNotRecognizedException e) {
                Console.addConsoleOutput(
                        "warning: Validator does not recognize feature (" + SCHEMA_FULL_CHECKING_FEATURE_ID
                        + ")");
            } 
            catch (SAXNotSupportedException e) {
                Console.addConsoleOutput(
                        "warning: Validator does not support feature (" + SCHEMA_FULL_CHECKING_FEATURE_ID
                        + ")");
            }
            try {
                validator.setFeature(HONOUR_ALL_SCHEMA_LOCATIONS_ID, honourAllSchemaLocations);
            } 
            catch (SAXNotRecognizedException e) {
                Console.addConsoleOutput(
                        "warning: Validator does not recognize feature (" + HONOUR_ALL_SCHEMA_LOCATIONS_ID
                        + ")");
            } 
            catch (SAXNotSupportedException e) {
                Console.addConsoleOutput(
                        "warning: Validator does not support feature (" + HONOUR_ALL_SCHEMA_LOCATIONS_ID
                        + ")");
            }
            try {
                validator.setFeature(VALIDATE_ANNOTATIONS_ID, validateAnnotations);
            } 
            catch (SAXNotRecognizedException e) {
                Console.addConsoleOutput(
                        "warning: Validator does not recognize feature (" + VALIDATE_ANNOTATIONS_ID + ")");
            } 
            catch (SAXNotSupportedException e) {
                Console.addConsoleOutput(
                        "warning: Validator does not support feature (" + VALIDATE_ANNOTATIONS_ID + ")");
            }
            try {
                validator.setFeature(GENERATE_SYNTHETIC_ANNOTATIONS_ID, generateSyntheticAnnotations);
            } 
            catch (SAXNotRecognizedException e) {
                Console.addConsoleOutput(
                        "warning: Validator does not recognize feature (" + GENERATE_SYNTHETIC_ANNOTATIONS_ID
                        + ")");
            } 
            catch (SAXNotSupportedException e) {
                Console.addConsoleOutput(
                        "warning: Validator does not support feature (" + GENERATE_SYNTHETIC_ANNOTATIONS_ID
                        + ")");
            }

            // Validate instance documents and print type information
            if (instances != null && instances.size() > 0) {
                final int length = instances.size();
                for (int j = 0; j < length; ++j) {
                    parser.parse((String) instances.elementAt(j));
                }
            }
        } 
        catch (SAXParseException e) {
        } 
        catch (Exception e) {
            Console.addConsoleOutput("error: Parse error occurred - " + e.getMessage());
            if (e instanceof SAXException) {
                Exception nested = ((SAXException) e).getException();
                if (nested != null) {
                    e = nested;
                }
            }
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            Console.addConsoleOutput(errors.toString());
        }
    }

    public void setDocumentLocator(Locator locator) {
        fIndent = 0;
        printIndent();
        Console.addConsoleOutput("setDocumentLocator(");
        Console.addConsoleOutput("systemId=");
        printQuotedString(locator.getSystemId());
        Console.addConsoleOutput(", publicId=");
        printQuotedString(locator.getPublicId());
        Console.addConsoleOutput(")");
        fOut.flush();
    }

    public void startDocument() throws SAXException {
        fIndent = 0;
        printIndent();
        Console.addConsoleOutput("startDocument()");
        fOut.flush();
        fIndent++;
    }

    public void startElement(String uri, String localName, String qname, Attributes attributes)
            throws SAXException {
        TypeInfo type;
        printIndent();
        Console.addConsoleOutput("startElement(");
        Console.addConsoleOutput("name=");
        printQName(uri, localName);
        Console.addConsoleOutput(",");
        Console.addConsoleOutput("type=");
        if (fTypeInfoProvider != null && (type = fTypeInfoProvider.getElementTypeInfo()) != null) {
            printQName(type.getTypeNamespace(), type.getTypeName());
        } 
        else {
            Console.addConsoleOutput("null");
        }
        Console.addConsoleOutput(",");
        Console.addConsoleOutput("attributes=");
        if (attributes == null) {
            Console.addConsoleOutput("null");
        } 
        else {
            Console.addConsoleOutput("{");
            int length = attributes.getLength();
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    Console.addConsoleOutput(",");
                }
                String attrURI = attributes.getURI(i);
                String attrLocalName = attributes.getLocalName(i);
                Console.addConsoleOutput("{");
                Console.addConsoleOutput("name=");
                printQName(attrURI, attrLocalName);
                Console.addConsoleOutput(",");
                Console.addConsoleOutput("type=");
                if (fTypeInfoProvider != null && (type = fTypeInfoProvider.getAttributeTypeInfo(i)) != null) {
                    printQName(type.getTypeNamespace(), type.getTypeName());
                } 
                else {
                    Console.addConsoleOutput("null");
                }
                Console.addConsoleOutput(",");
                Console.addConsoleOutput("id=");
                Console.addConsoleOutput(
                        fTypeInfoProvider != null && fTypeInfoProvider.isIdAttribute(i) ? "\"true\"" :
                                "\"false\"");
                Console.addConsoleOutput(",");
                Console.addConsoleOutput("specified=");
                Console.addConsoleOutput(
                        fTypeInfoProvider == null || fTypeInfoProvider.isSpecified(i) ? "\"true\"" :
                                "\"false\"");
                Console.addConsoleOutput("}");
            }
            Console.addConsoleOutput("}");
        }
        Console.addConsoleOutput(")");
        fOut.flush();
        fIndent++;
    }

    public void endElement(String uri, String localName, String qname) throws SAXException {
        fIndent--;
        printIndent();
        Console.addConsoleOutput("endElement(");
        Console.addConsoleOutput("name=");
        printQName(uri, localName);
        Console.addConsoleOutput(")");
        fOut.flush();
    }

    public void endDocument() throws SAXException {
        fIndent--;
        printIndent();
        Console.addConsoleOutput("endDocument()");
        fOut.flush();
    }

    public void warning(SAXParseException ex) throws SAXException {
        printError("Warning", ex);
    }

    public void error(SAXParseException ex) throws SAXException {
        printError("Error", ex);
    }

    public void fatalError(SAXParseException ex) throws SAXException {
        printError("Fatal Error", ex);
        throw ex;
    }

    public void setOutput(OutputStream stream, String encoding) throws UnsupportedEncodingException {
        if (encoding == null) {
            encoding = "UTF8";
        }
        java.io.Writer writer = new OutputStreamWriter(stream, encoding);
        fOut = new PrintWriter(writer);
    }

    protected void setTypeInfoProvider(TypeInfoProvider provider) {
        fTypeInfoProvider = provider;
    }

    protected void printError(String type, SAXParseException ex) {
        if (type.equals("Error")) {
            ODMEEditor.errorPresentInSES = 1;
            ODMEEditor.errorMessageInSES = ex.getMessage();
        }

        Console.addConsoleOutput("[");
        Console.addConsoleOutput(type);
        Console.addConsoleOutput("] ");
        String systemId = ex.getSystemId();
        if (systemId != null) {
            int index = systemId.lastIndexOf('/');
            if (index != -1) {
                systemId = systemId.substring(index + 1);
            }
            Console.addConsoleOutput(systemId);
        }
        Console.addConsoleOutput(":");
        Console.addConsoleOutput("" + ex.getLineNumber());
        Console.addConsoleOutput("" + ':');
        Console.addConsoleOutput("" + ex.getColumnNumber());
        Console.addConsoleOutput(": ");
        Console.addConsoleOutput(ex.getMessage());
        Console.addConsoleOutput("");
    }

    protected void printIndent() {
        for (int i = 0; i < fIndent; i++) {
            Console.addConsoleOutput(" ");
        }
    }

    protected void printQName(String uri, String localName) {
        if (uri != null && uri.length() > 0) {
            printQuotedString('{' + uri + "}" + localName);
            return;
        }
        printQuotedString(localName);
    }

    protected void printQuotedString(String s) {
        if (s == null) {
            Console.addConsoleOutput("null");
            return;
        }
        Console.addConsoleOutput("\"");
        Console.addConsoleOutput(s);
        Console.addConsoleOutput("\"");
    }
}
