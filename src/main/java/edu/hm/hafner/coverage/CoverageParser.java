package edu.hm.hafner.coverage;

import java.io.Reader;
import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Optional;
import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.google.errorprone.annotations.FormatMethod;

import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.TreeStringBuilder;

/**
 * Parses a file and returns the code coverage information in a tree of {@link Node} instances.
 *
 * @author Ullrich Hafner
 */
public abstract class CoverageParser implements Serializable {
    private static final long serialVersionUID = 3941742254762282096L;

    /** Toplevel module name. */
    protected static final String EMPTY = "-";

    /**
     * Defines how to handle fatal errors during parsing.
     */
    public enum ProcessingMode {
        /** All fatal errors will be ignored and logged. */
        IGNORE_ERRORS,
        /** An exception will be thrown if a fatal error is detected. */
        FAIL_FAST
    }

    private transient TreeStringBuilder treeStringBuilder = new TreeStringBuilder();
    private final ProcessingMode processingMode; // since 0.26.0

    /**
     * Creates a new instance of {@link CoverageParser}.
     *
     * @param processingMode
     *         determines whether to ignore errors
     */
    protected CoverageParser(final ProcessingMode processingMode) {
        this.processingMode = processingMode;
    }

    /**
     * Creates a new instance of {@link CoverageParser} that will fail on all errors.
     */
    protected CoverageParser() {
        this(ProcessingMode.FAIL_FAST);
    }

    /**
     * Returns whether to ignore errors or to fail fast.
     *
     * @return true if errors should be ignored, false if an exception should be thrown on errors
     */
    protected boolean ignoreErrors() {
        return processingMode == ProcessingMode.IGNORE_ERRORS;
    }

    /**
     * Parses a report provided by the given reader.
     *
     * @param reader
     *         the reader with the coverage information
     * @param fileName
     *         the file name of the report
     * @param log
     *         the logger to write messages to
     *
     * @return the root of the created tree
     * @throws ParsingException
     *         if the content cannot be read by the parser
     */
    public ModuleNode parse(final Reader reader, final String fileName, final FilteredLog log) {
        var moduleNode = parseReport(reader, fileName, log);
        getTreeStringBuilder().dedup();
        return moduleNode;
    }

    /**
     * Returns the name of the specified element.
     *
     * @param event
     *         the event
     *
     * @return the name
     */
    protected QName getElementName(final XMLEvent event) {
        if (event.isStartElement()) {
            return event.asStartElement().getName();
        }
        else {
            return event.asEndElement().getName();
        }
    }

    /**
     * Handles processing of empty results.
     *
     * @param fileName
     *         the file name of the report
     * @param log
     *         the log
     * @param isEmpty
     *         set this flag to {@code true} to indicate that the results are empty
     *
     * @throws NoSuchElementException
     *         if the results are empty and errors should not be ignored
     */
    protected void handleEmptyResults(final String fileName, final FilteredLog log, final boolean isEmpty) {
        if (isEmpty) {
            var emptyMessage = String.format("[%s] The processed file '%s' does not contain data.", getClass().getSimpleName(), fileName);
            if (ignoreErrors()) {
                log.logError(emptyMessage);
            }
            else {
                throw new NoSuchElementException(emptyMessage);
            }
        }
    }

    /**
     * Handles processing of empty results.
     *
     * @param fileName
     *         the file name of the report
     * @param log
     *         the log
     */
    protected void handleEmptyResults(final String fileName, final FilteredLog log) {
        handleEmptyResults(fileName, log, true);
    }

    /**
     * Called after de-serialization to restore transient fields.
     *
     * @return this
     */
    @SuppressWarnings("PMD.NullAssignment")
    protected Object readResolve() {
        treeStringBuilder = new TreeStringBuilder();

        return this;
    }

    public final TreeStringBuilder getTreeStringBuilder() {
        return treeStringBuilder;
    }

    /**
     * Parses a report provided by the given reader.
     *
     * @param reader
     *         the reader with the coverage information
     * @param fileName
     *         the file name of the report
     * @param log
     *         the logger to write messages to
     *
     * @return the root of the created tree
     * @throws ParsingException
     *         if the content cannot be read by the parser
     */
    protected abstract ModuleNode parseReport(Reader reader, String fileName, FilteredLog log);

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

    protected static ParsingException createEofException(final String fileName) {
        return new ParsingException("Unexpected end of file '%s'", fileName);
    }

    /**
     * Indicates that during parsing a non-recoverable error has been occurred.
     *
     * @author Ullrich Hafner
     */
    public static class ParsingException extends RuntimeException {
        private static final long serialVersionUID = -9016364685084958944L;

        /**
         * Constructs a new {@link ParsingException} with the specified cause.
         *
         * @param cause
         *         the cause (which is saved for later retrieval by the {@link #getCause()} method).
         */
        public ParsingException(final Throwable cause) {
            super(createMessage(cause, "Exception occurred during parsing"), cause);
        }

        /**
         * Constructs a new {@link ParsingException} with the specified cause and message.
         *
         * @param cause
         *         the cause (which is saved for later retrieval by the {@link #getCause()} method).
         * @param messageFormat
         *         the message as a format string as described in <a href="../util/Formatter.html#syntax">Format string
         *         syntax</a>
         * @param args
         *         Arguments referenced by the format specifiers in the format string.  If there are more arguments than
         *         format specifiers, the extra arguments are ignored.  The number of arguments is variable and may be zero.
         *         The maximum number of arguments is limited by the maximum dimension of a Java array as defined by
         *         <cite>The Java&trade; Virtual Machine Specification</cite>. The behaviour on a {@code null} argument
         *         depends on the <a href="../util/Formatter.html#syntax">conversion</a>.
         */
        @FormatMethod
        public ParsingException(final Throwable cause, final String messageFormat, final Object... args) {
            super(createMessage(cause, String.format(messageFormat, args)), cause);
        }

        /**
         * Constructs a new {@link ParsingException} with the specified message.
         *
         * @param messageFormat
         *         the message as a format string as described in <a href="../util/Formatter.html#syntax">Format string
         *         syntax</a>
         * @param args
         *         Arguments referenced by the format specifiers in the format string.  If there are more arguments than
         *         format specifiers, the extra arguments are ignored.  The number of arguments is variable and may be zero.
         *         The maximum number of arguments is limited by the maximum dimension of a Java array as defined by
         *         <cite>The Java&trade; Virtual Machine Specification</cite>. The behavior on a {@code null} argument
         *         depends on the <a href="../util/Formatter.html#syntax">conversion</a>.
         */
        @FormatMethod
        public ParsingException(final String messageFormat, final Object... args) {
            super(String.format(messageFormat, args));
        }

        private static String createMessage(final Throwable cause, final String message) {
            return String.format("%s%n%s%n%s", message,
                    ExceptionUtils.getMessage(cause), ExceptionUtils.getStackTrace(cause));
        }
    }
}
