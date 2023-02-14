package edu.hm.hafner.metric;

import java.io.Reader;
import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Optional;
import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

import edu.hm.hafner.util.FilteredLog;
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
     * @param log
     *         the logger to write messages to
     *
     * @return the root of the created tree
     * @throws ParsingException
     *         if the XML content cannot be read
     */
    public abstract ModuleNode parse(Reader reader, FilteredLog log);

    protected static Optional<String> getOptionalValueOf(final StartElement element, final QName attribute) {
        Attribute value = element.getAttributeByName(attribute);
        if (value == null) {
            return Optional.empty();
        }

        return Optional.of(value.getValue());
    }

    protected static int getIntegerValueOf(final StartElement element, final QName attributeName) {
        try {
            return parseInteger(getValueOf(element, attributeName));
        }
        catch (NumberFormatException ignore) {
            return 0;
        }
    }

    protected static String getValueOf(final StartElement element, final QName attribute) {
        return getOptionalValueOf(element, attribute).orElseThrow(
                () -> new NoSuchElementException(String.format(
                        "Could not obtain attribute '%s' from element '%s'", attribute, element)));
    }

    protected static int parseInteger(final String value) {
        try {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException ignore) {
            return 0;
        }
    }

    protected static ParsingException createEofException() {
        return new ParsingException("Unexpected end of file");
    }
}
