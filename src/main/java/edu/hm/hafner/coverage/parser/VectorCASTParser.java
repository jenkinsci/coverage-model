package edu.hm.hafner.coverage.parser;

import java.io.Reader;
import java.util.regex.Pattern;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.Attribute;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.CyclomaticComplexity;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.util.FilteredLog;
import java.lang.StringBuilder;

import java.util.List;
import edu.hm.hafner.coverage.Value;



/**
 * Parses VectorCAST reports into a hierarchical Java Object Model.
 *
 * @author Melissa Bauer
 * @author Ullrich Hafner
 */
@SuppressWarnings({"checkstyle:ClassDataAbstractionCoupling", "PMD.GodClass"})
public class VectorCASTParser extends CoberturaParser {
    private static final long serialVersionUID = 598117573006409816L;
 
    private static final Pattern BRANCH_PATTERN = Pattern.compile(".*\\((?<covered>\\d+)/(?<total>\\d+)\\)");

    private static final Coverage DEFAULT_BRANCH_COVERAGE       = new CoverageBuilder(Metric.BRANCH).withCovered(2).withMissed(0).build();
    private static final Coverage DEFAULT_MCDCPAIR_COVERAGE     = new CoverageBuilder(Metric.MCDC_PAIR).withCovered(0).withMissed(0).build();
    private static final Coverage DEFAULT_FUNCTION_COVERAGE     = new CoverageBuilder(Metric.FUNCTION).withCovered(0).withMissed(0).build();
    private static final Coverage DEFAULT_FUNCTIONCALL_COVERAGE = new CoverageBuilder(Metric.FUNCTION_CALL).withCovered(0).withMissed(0).build();

    /** XML elements. */
    private static final QName CLASS = new QName("class");
    private static final QName METHOD = new QName("method");
    private static final QName LINE = new QName("line");

    /** Required attributes of the XML elements. */
    private static final QName HITS = new QName("hits");
    private static final QName COMPLEXITY = new QName("complexity");
    private static final QName NUMBER = new QName("number");

    /** Optional attributes of the XML elements. */
    private static final QName MCDCPAIR_COVERAGE = new QName("mcdcpair-coverage");
    private static final QName FUNCTIONCALL_COVERAGE = new QName("functioncall-coverage");
    private static final QName FUNCTION_COVERAGE = new QName("function-coverage");
    
    /**
     * Creates a new instance of {@link VectorCASTParser}.
     *
     * @param processingMode
     *         determines whether to ignore errors
     */
    public VectorCASTParser(final ProcessingMode processingMode) {
        super(processingMode);
    }
    
    @Override 
    protected ModuleNode parseReport(final Reader reader, final String fileName, final FilteredLog log) {
        return super.parseReport(reader, fileName, log);
    }
    
    protected Coverage setMcdcPairCoverage(final StartElement nextElement, Coverage mcdcPairLineCoverage, Coverage mcdcPairCoverage){
        if (isMcdcPairCoverage(nextElement)) {
            mcdcPairLineCoverage = readMcdcPairCoverage(nextElement);
            mcdcPairCoverage = mcdcPairCoverage.add(mcdcPairLineCoverage);
        }
        return mcdcPairCoverage;
    }

    protected Coverage setFunctionCallCoverage(final StartElement nextElement, Coverage functionCallLineCoverage, Coverage functionCallCoverage) {
        if (isFunctionCallCoverage(nextElement)) {
            functionCallLineCoverage = readFunctionCallCoverage(nextElement);
            functionCallCoverage = functionCallCoverage.add(functionCallLineCoverage);
        }
        return functionCallCoverage;
    }

    protected Coverage seFunctionCoverage(final StartElement nextElement, Coverage functionMethodCoverage, Coverage functionCoverage) {
        if (METHOD.equals(nextElement.getName())) {
            functionMethodCoverage = Coverage.nullObject(Metric.FUNCTION);
            functionMethodCoverage = readFunctionCoverage(nextElement);
            functionCoverage = functionCoverage.add(functionMethodCoverage);    
        }
        return functionCoverage;
    }
    
    protected void processClassMethodEnd(Node node, Coverage lineCoverage, Coverage mcdcPairCoverage, Coverage functionCallCoverage, 
        Coverage functionCoverage, Coverage branchCoverage) {
            
        node.addValue(lineCoverage);
        
        if (mcdcPairCoverage.isSet()) {
            node.addValue(mcdcPairCoverage);
        }
        
        if (functionCallCoverage.isSet()) {
            node.addValue(functionCallCoverage);
        }
        if (functionCoverage.isSet()) {
            node.addValue(functionCoverage);
            
        }
        if (branchCoverage.isSet()) {
            node.addValue(branchCoverage);
        }
    }

    @Override 
    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.CognitiveComplexity"})
    protected void readClassOrMethod(final XMLEventReader reader, 
            final FileNode fileNode, final Node parentNode, 
            final StartElement element, final String fileName, final FilteredLog log)
                throws XMLStreamException {
        
        var lineCoverage = Coverage.nullObject(Metric.LINE);
        var branchCoverage = Coverage.nullObject(Metric.BRANCH);
        var mcdcPairCoverage = Coverage.nullObject(Metric.MCDC_PAIR);
        var functionCallCoverage = Coverage.nullObject(Metric.FUNCTION_CALL);
        var functionCoverage = Coverage.nullObject(Metric.FUNCTION);

        Node node = createNode(parentNode, element, log);
        getOptionalValueOf(element, COMPLEXITY)
                .ifPresent(c -> node.addValue(new CyclomaticComplexity(readComplexity(c))));

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                var nextElement = event.asStartElement();
                if (LINE.equals(nextElement.getName())) {
                    Coverage lineBranchCoverage;
                    Coverage currentLineCoverage;
                    Coverage mcdcPairLineCoverage = Coverage.nullObject(Metric.MCDC_PAIR);
                    Coverage functionCallLineCoverage = Coverage.nullObject(Metric.FUNCTION_CALL);
                    if (isBranchCoverage(nextElement)) {
                        lineBranchCoverage = readBranchCoverage(nextElement);
                        currentLineCoverage = computeLineCoverage(lineBranchCoverage.getCovered());
                        branchCoverage = branchCoverage.add(lineBranchCoverage);
                        mcdcPairCoverage = setMcdcPairCoverage(nextElement, mcdcPairLineCoverage, mcdcPairCoverage);
                        functionCallCoverage = setFunctionCallCoverage(nextElement, functionCallLineCoverage, functionCallCoverage);
                    } 
                    else if (isFunctionCallCoverage(nextElement)) {
                        functionCallCoverage = setFunctionCallCoverage(nextElement, functionCallLineCoverage, functionCallCoverage);
                        int lineHits = getIntegerValueOf(nextElement, HITS);
                        currentLineCoverage = computeLineCoverage(lineHits);
                        lineBranchCoverage = currentLineCoverage;
                    }
                    else {
                        int lineHits = getIntegerValueOf(nextElement, HITS);
                        currentLineCoverage = computeLineCoverage(lineHits);
                        lineBranchCoverage = currentLineCoverage;
                    }
                    
                    lineCoverage = lineCoverage.add(currentLineCoverage);

                    if (CLASS.equals(element.getName())) { // Use the line counters at the class level for a file
                        int lineNumber = getIntegerValueOf(nextElement, NUMBER);
                        
                        fileNode.addCounters(lineNumber, lineBranchCoverage.getCovered(), lineBranchCoverage.getMissed());
                        fileNode.addMCDCPairCounters(lineNumber, mcdcPairLineCoverage.getCovered(), mcdcPairLineCoverage.getMissed());
                        fileNode.addFunctionCallCounters(lineNumber, functionCallLineCoverage.getCovered(), functionCallLineCoverage.getMissed());
                    }
                }
                else if (METHOD.equals(nextElement.getName())) {
                    Coverage functionMethodCoverage = Coverage.nullObject(Metric.FUNCTION);
                    seFunctionCoverage(nextElement, functionMethodCoverage, functionCoverage);                    
                    readClassOrMethod(reader, fileNode, node, nextElement, fileName, log); // recursive call
                }
                else if (CLASS.equals(nextElement.getName())) {
                    readClassOrMethod(reader, fileNode, node, nextElement, fileName, log); // recursive call
                }
            }
            else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (CLASS.equals(endElement.getName()) || METHOD.equals(endElement.getName())) {
                    processClassMethodEnd(node, lineCoverage, mcdcPairCoverage, functionCallCoverage, functionCoverage, branchCoverage);
                    return;
                }
            }
        }
        throw createEofException(fileName);
    }
    
    private boolean isVectorCASTCoverage(final StartElement line, final QName attribute) {
        boolean retVal = false;
        
        Attribute cov = line.getAttributeByName(attribute);
        
        if (cov != null) {
            retVal = true;
        } 
        return retVal;
    }

    private boolean isMcdcPairCoverage(final StartElement line) {
        return isVectorCASTCoverage(line, MCDCPAIR_COVERAGE);
    }

    private boolean isFunctionCoverage(final StartElement line) {
        return isVectorCASTCoverage(line, FUNCTION_COVERAGE);
    }

    private boolean isFunctionCallCoverage(final StartElement line) {
        return isVectorCASTCoverage(line, FUNCTIONCALL_COVERAGE);
    }

    private Coverage readMcdcPairCoverage(final StartElement line) {
        return getOptionalValueOf(line, MCDCPAIR_COVERAGE).map(this::fromMcdcPairCoverage).orElse(DEFAULT_MCDCPAIR_COVERAGE);
    }

    private Coverage fromMcdcPairCoverage(final String mcdcPairCoverageAttribute) {
        var matcher = BRANCH_PATTERN.matcher(mcdcPairCoverageAttribute);
        if (matcher.matches()) {
            return new CoverageBuilder().withMetric(Metric.MCDC_PAIR)
                    .withCovered(matcher.group("covered"))
                    .withTotal(matcher.group("total"))
                    .build();
        }
        return Coverage.nullObject(Metric.MCDC_PAIR);
    }
    
    private Coverage readFunctionCoverage(final StartElement line) {
        return getOptionalValueOf(line, FUNCTION_COVERAGE).map(this::fromFunctionCoverage).orElse(DEFAULT_FUNCTION_COVERAGE);
    }

    private Coverage fromFunctionCoverage(final String functionCoverageAttribute) {
        var matcher = BRANCH_PATTERN.matcher(functionCoverageAttribute);
        if (matcher.matches()) {
            return new CoverageBuilder().withMetric(Metric.FUNCTION)
                    .withCovered(matcher.group("covered"))
                    .withTotal(matcher.group("total"))
                    .build();
        }
        return Coverage.nullObject(Metric.FUNCTION);
    }
    
    
    private Coverage readFunctionCallCoverage(final StartElement line) {
        return getOptionalValueOf(line, FUNCTIONCALL_COVERAGE).map(this::fromFunctionCallCoverage).orElse(DEFAULT_FUNCTIONCALL_COVERAGE);
    }

    private Coverage fromFunctionCallCoverage(final String functionCallCoverageAttribute) {
        var matcher = BRANCH_PATTERN.matcher(functionCallCoverageAttribute);
        if (matcher.matches()) {
            return new CoverageBuilder().withMetric(Metric.FUNCTION_CALL)
                    .withCovered(matcher.group("covered"))
                    .withTotal(matcher.group("total"))
                    .build();
        }
        return Coverage.nullObject(Metric.FUNCTION_CALL);
    }
}
