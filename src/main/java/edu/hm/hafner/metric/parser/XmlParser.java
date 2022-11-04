package edu.hm.hafner.metric.parser;

import java.io.Reader;
import java.io.Serializable;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import edu.hm.hafner.metric.ModuleNode;
import edu.hm.hafner.util.SecureXmlParserFactory;
import edu.hm.hafner.util.SecureXmlParserFactory.ParsingException;

/**
 * Base class for xml parser.
 *
 * @author Melissa Bauer
 */
public abstract class XmlParser implements Serializable {
    private static final long serialVersionUID = -181158607646148018L;

    private ModuleNode rootNode;

    public ModuleNode getRootNode() {
        return rootNode;
    }

    protected void setRootNode(final ModuleNode newRootNode) {
        rootNode = newRootNode;
    }

    /**
     * Parses xml report at given path.
     *
     * @param reader
     *         the reader to wrap
     *
     * @return the created data model
     */
    public ModuleNode parse(final Reader reader) {
        SecureXmlParserFactory factory = new SecureXmlParserFactory();

        XMLEventReader r;
        try {
            r = factory.createXmlEventReader(reader);
            while (r.hasNext()) {
                XMLEvent e = r.nextEvent();

                if (e.isStartElement()) {
                    startElement(e.asStartElement());
                }

                if (e.isEndElement()) {
                    endElement(e.asEndElement());
                }
            }
        }
        catch (XMLStreamException ex) {
            throw new ParsingException(ex);
        }

        return rootNode;
    }

    /**
     * Method to handle the 'startElement' event.
     *
     * @param element
     *         the current report element
     */
    protected abstract void startElement(StartElement element);

    /**
     * Method to handle the 'endElement' event.
     *
     * @param element
     *         the current report element
     */
    protected abstract void endElement(EndElement element);

    protected String getValueOf(final StartElement element, final QName attribute) {
        Attribute value = element.getAttributeByName(attribute);

        if (value == null) {
            if (!isOptional(attribute.toString())) {
                throw new IllegalArgumentException(
                        String.format("Could not obtain attribute '%s' from element '%s'", attribute, element));
            }
            return null;
        }

        return value.getValue();
    }

    /**
     * Checks if the given attribute is an optional one in the report or not.
     * @param attribute the attribute of the report
     * @return if the attribute is optional
     */
    abstract boolean isOptional(String attribute);
}
