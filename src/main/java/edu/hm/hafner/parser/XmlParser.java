package edu.hm.hafner.parser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import edu.hm.hafner.coverage.CoverageNode;

/**
 * Base class for xml parser.
 *
 * @author Melissa Bauer
 */
public abstract class XmlParser {

    private static CoverageNode rootNode;

    public CoverageNode getRootNode() {
        return rootNode;
    }

    protected void setRootNode(final CoverageNode newRootNode) {
        rootNode = newRootNode;
    }

    /**
     * Parses xml report at given path.
     *
     * @param path path to report file
     */
    void parseFile(final String path) {
        XMLInputFactory factory = XMLInputFactory.newInstance();

        XMLEventReader r;
        try {
            r = factory.createXMLEventReader(path, new FileInputStream(path));
            while (r.hasNext()) {
                XMLEvent e = r.nextEvent();

                if (e.isStartDocument()) {
                    startDocument(e);
                }

                if (e.isStartElement()) {
                    startElement(e.asStartElement());
                }

                if (e.isEndElement()) {
                    endElement(e.asEndElement());
                }
            }
        }
        catch (XMLStreamException | FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Method to handle the 'startDocument' element.
     * It's possible to get the file name at this point.
     * @param event the xml element
     */
    protected abstract void startDocument(XMLEvent event);

    /**
     * Method to handle the 'startElement' event.
     * Get the current xml
     * @param element the current element
     */
    protected abstract void startElement(StartElement element);

    protected abstract void endElement(EndElement element);

}
