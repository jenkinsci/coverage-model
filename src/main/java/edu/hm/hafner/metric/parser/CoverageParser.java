package edu.hm.hafner.metric.parser;

import java.io.Reader;
import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Optional;
import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

import edu.hm.hafner.metric.ModuleNode;
import edu.hm.hafner.metric.Node;
import edu.hm.hafner.util.SecureXmlParserFactory.ParsingException;

/**
 * Parses a file and returns the code coverage information in a tree of {@link Node} instances.
 *
 * @author Ullrich Hafner
 */
public abstract class CoverageParser implements Serializable {
    private static final long serialVersionUID = 3941742254762282096L;

    /**
     * Parses a report provided by the given reader.
     *
     * @param reader
     *         the reader with the coverage information
     *
     * @return the root of the created tree
     * @throws ParsingException
     *         if the XML content cannot be read
     */
    public abstract ModuleNode parse(Reader reader);

    protected final Optional<String> getOptionalValueOf(final StartElement element, final QName attribute) {
        Attribute value = element.getAttributeByName(attribute);
        if (value == null) {
            return Optional.empty();
        }

        return Optional.of(value.getValue());
    }

    protected String getValueOf(final StartElement element, final QName attribute) {
        return getOptionalValueOf(element, attribute).orElseThrow(
                () -> new NoSuchElementException(String.format(
                        "Could not obtain attribute '%s' from element '%s'", attribute, element)));
    }
}
