package edu.hm.hafner.coverage.parser;

import java.io.Reader;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang3.tuple.Pair;

import edu.hm.hafner.coverage.ClassNode;
import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.PackageNode;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.PathUtil;
import edu.hm.hafner.util.SecureXmlParserFactory;
import edu.hm.hafner.util.SecureXmlParserFactory.ParsingException;

/**
 * A parser which parses reports made by OpenCover into a Java Object Model.
 *
 */
public class OpenCoverParser extends CoverageParser {

    private static final long serialVersionUID = 1L;

    private static final PathUtil PATH_UTIL = new PathUtil();

    /** XML elements. */
    private static final QName MODULE = new QName("Module");
    private static final QName CLASS = new QName("Class");
    private static final QName METHOD = new QName("Method");
    private static final QName CLASS_NAME = new QName("FullName");
    private static final QName METHOD_NAME = new QName("Name");
    private static final QName MODULE_NAME = new QName("ModuleName");
    private static final QName FILE = new QName("File");
    private static final QName FILE_REF = new QName("FileRef");

    private static final QName UID = new QName("uid");
    private static final QName FULL_PATH = new QName("fullPath");
    private static final QName CLASS_COMPLEXITY = new QName("maxCyclomaticComplexity");
    private static final QName METHOD_COMPLEXITY = new QName("cyclomaticComplexity");

    @Override
    protected ModuleNode parseReport(final Reader reader, final FilteredLog log) {
        try {
            var eventReader = new SecureXmlParserFactory().createXmlEventReader(reader);
            var root = new ModuleNode("-");
            boolean isEmpty = true;
            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();
                if (event.isStartElement()) {
                    var startElement = event.asStartElement();
                    var tagName = startElement.getName();
                    if (MODULE.equals(tagName)) {
                        readPackage(eventReader, root, startElement, log);
                        isEmpty = false;
                    }
                }
            }
            if (isEmpty) {
                throw new NoSuchElementException("No coverage information found in the specified file.");
            }
            return root;
        }
        catch (XMLStreamException exception) {
            throw new ParsingException(exception);
        }
    }

    private void readPackage(final XMLEventReader reader, final ModuleNode root,
            final StartElement currentStartElement, final FilteredLog log) throws XMLStreamException {

        Map<String, String> files = new LinkedHashMap<>();
        List<Pair<String, List<MethodMapping>>> classes = new LinkedList<>();
        PackageNode packageNode = null;
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.isStartElement()) {
                var nextElement = event.asStartElement();
                if (CLASS.equals(nextElement.getName())) {
                    classes.add(readClass(reader, nextElement, log));
                }
                else if (FILE.equals(nextElement.getName())) {
                    var fileName = getValueOf(nextElement, FULL_PATH);
                    var uid = getValueOf(nextElement, UID);
                    var relativePath = PATH_UTIL.getRelativePath(fileName);
                    files.put(uid, relativePath);
                }
                else if (MODULE_NAME.equals(nextElement.getName())) {
                    String packageName = reader.nextEvent().asCharacters().getData();
                    packageNode = root.findOrCreatePackageNode(packageName);
                }
            }
        }

        // Creating all nodes
        for (var file : files.entrySet()) {
            FileNode fileNode = packageNode.findOrCreateFileNode(getFileName(file.getValue()), getTreeStringBuilder().intern(file.getValue()));
            for (Pair<String, List<MethodMapping>> clazz : classes) {
                if (clazz.getValue().get(0).getFileId().equals(file.getKey())) {
                    ClassNode classNode = fileNode.createClassNode(clazz.getKey());
                    for (var method : clazz.getValue()) {
                        classNode.createMethodNode(method.getMethodName(), method.getMethodName());
                    }
                }   
            }
        }
    }

    private Pair<String, List<MethodMapping>> readClass(final XMLEventReader reader, final StartElement parentElement, final FilteredLog log) throws XMLStreamException {
        String className = null;
        List<MethodMapping> methods = new LinkedList<>();
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.isStartElement()) {
                var nextElement = event.asStartElement();
                if (CLASS_NAME.equals(nextElement.getName())) {
                    className = reader.nextEvent().asCharacters().getData();
                }
                if (METHOD.equals(nextElement.getName())) {
                    methods.add(readMethod(reader, nextElement, log));
                }
            }
            else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (CLASS.equals(endElement.getName())) {
                    return Pair.of(className, methods);
                }
            }
        }

        throw new NoSuchElementException("Unable to parse class name");
    }

    private static class MethodMapping {
        private final String methodName;
        private final String fileId;
        public MethodMapping(final String methodName, final String fileId) {
            this.methodName = methodName;
            this.fileId = fileId;
        }
        public String getMethodName() {
            return methodName;
        }
        public String getFileId() {
            return fileId;
        }
    }

    private MethodMapping readMethod(final XMLEventReader reader, final StartElement parentElement, final FilteredLog log) throws XMLStreamException {
        String methodName = null;
        String fileId = null;
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.isStartElement()) {
                var nextElement = event.asStartElement();
                if (METHOD_NAME.equals(nextElement.getName())) {
                    methodName = reader.nextEvent().asCharacters().getData();
                }
                if (FILE_REF.equals(nextElement.getName())) {
                    fileId = getValueOf(nextElement, UID);
                }
            }
            else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (METHOD.equals(endElement.getName()) && fileId != null) {
                    return new MethodMapping(methodName, fileId);
                }
            }
        }

        throw new NoSuchElementException("Unable to parse method");
    }

    private int readComplexity(final String c) {
        try {
            return Math.round(Float.parseFloat(c)); // some reports use float values
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

    private String getFileName(final String relativePath) {
        var path = Paths.get(PATH_UTIL.getAbsolutePath(relativePath)).getFileName();
        if (path == null) {
            return relativePath;
        }
        return path.toString();
    }

}
