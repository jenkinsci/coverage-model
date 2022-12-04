package edu.hm.hafner.metric.parser;

import java.io.Reader;
import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Optional;
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
import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * Base class for xml parser.
 *
 * @author Melissa Bauer
 */
public abstract class XmlParser implements Serializable {
    private static final long serialVersionUID = -181158607646148018L;

    @CheckForNull
    private ModuleNode rootNode;

    protected final ModuleNode getRootNode() {
        if (rootNode == null) {
            throw new NoSuchElementException(
                    "No root node found, is this file a valid coverage report?"); // TODO: Parsing exception
        }

        return rootNode;
    }

    protected void setRootNode(final ModuleNode newRootNode) {
        rootNode = newRootNode;
    }

    /**
     * Template method that parses the XML report provided by the given reader. Delegates processing of start and end
     * element events to abstract methods that need to be overwritten by concrete subclasses.
     *
     * @param reader
     *         the reader with the XML content
     *
     * @return the root of the created tree
     * @throws ParsingException
     *         if the XML content cannot be read
     */
    public ModuleNode parse(final Reader reader) {
        try {
            SecureXmlParserFactory factory = new SecureXmlParserFactory();
            XMLEventReader xmlEventReader = factory.createXmlEventReader(reader);
            while (xmlEventReader.hasNext()) {
                XMLEvent event = xmlEventReader.nextEvent();

                if (event.isStartElement()) {
                    startElement(event.asStartElement());
                }
                else if (event.isEndElement()) {
                    endElement(event.asEndElement());
                }
            }
        }
        catch (XMLStreamException ex) {
            throw new ParsingException(ex);
        }

        return getRootNode();
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
        var value = getOptionalValueOf(element, attribute);

        return value.orElseThrow(() ->
            new NoSuchElementException(
                    String.format("Could not obtain attribute '%s' from element '%s'", attribute, element)));
    }

    protected Optional<String> getOptionalValueOf(final StartElement element, final QName attribute) {
        Attribute value = element.getAttributeByName(attribute);

        if (value == null) {
            return Optional.empty();
        }

        return Optional.of(value.getValue());
    }

    /**
     * Checks if the given attribute is an optional one in the report or not.
     *
     * @param attribute
     *         the attribute of the report
     *
     * @return if the attribute is optional
     */
    abstract boolean isOptional(String attribute);
}
