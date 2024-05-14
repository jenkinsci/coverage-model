package edu.hm.hafner.coverage.parser;

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
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.util.FilteredLog;
import java.util.Map;
import java.util.HashMap;

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
        
    private Coverage processClassMethodStart(final StartElement nextElement, final Coverage functionCoverage) {
        Coverage localFunctionCoverage = functionCoverage;
        
        if (METHOD.equals(nextElement.getName())) {
            Coverage functionMethodCoverage;
            functionMethodCoverage = readFunctionCoverage(nextElement);
            localFunctionCoverage = localFunctionCoverage.add(functionMethodCoverage);
        }
        
        return localFunctionCoverage;
    }

    protected boolean processStartElement(final StartElement nextElement, final StartElement element, final FileNode fileNode, final Map<Metric, Coverage> coverageMap) throws XMLStreamException {
        boolean runReadClassOrMethod = false;

        if (LINE.equals(nextElement.getName())) {
            Coverage lineBranchCoverage;
            Coverage currentLineCoverage;
            Coverage mcdcPairLineCoverage = Coverage.nullObject(Metric.MCDC_PAIR);
            Coverage functionCallLineCoverage = Coverage.nullObject(Metric.FUNCTION_CALL);
            if (isBranchCoverage(nextElement)) {
                lineBranchCoverage = readBranchCoverage(nextElement);
                currentLineCoverage = computeLineCoverage(lineBranchCoverage.getCovered());
                
                //repeating
                coverageMap.put(Metric.BRANCH,coverageMap.get(Metric.BRANCH).add(lineBranchCoverage));

                if (isMcdcPairCoverage(nextElement)) {
                    mcdcPairLineCoverage = readMcdcPairCoverage(nextElement);
                    
                    //repeating
                    coverageMap.put(Metric.MCDC_PAIR,coverageMap.get(Metric.MCDC_PAIR).add(mcdcPairLineCoverage));
                }
                if (isFunctionCallCoverage(nextElement)) {
                    functionCallLineCoverage = readFunctionCallCoverage(nextElement);
                    
                    //repeating
                    coverageMap.put(Metric.FUNCTION_CALL,coverageMap.get(Metric.FUNCTION_CALL).add(functionCallLineCoverage));
                }
            } 
            else if (isFunctionCallCoverage(nextElement)) {
                functionCallLineCoverage = readFunctionCallCoverage(nextElement);
                
                coverageMap.put(Metric.FUNCTION_CALL,coverageMap.get(Metric.FUNCTION_CALL).add(functionCallLineCoverage));

                int lineHits = getIntegerValueOf(nextElement, HITS);
                currentLineCoverage = computeLineCoverage(lineHits);
                lineBranchCoverage = currentLineCoverage;
            }
            else {
                int lineHits = getIntegerValueOf(nextElement, HITS);
                currentLineCoverage = computeLineCoverage(lineHits);
                lineBranchCoverage = currentLineCoverage;
            }
            
            coverageMap.put(Metric.LINE,coverageMap.get(Metric.LINE).add(currentLineCoverage));
                
            if (CLASS.equals(element.getName())) { // Use the line counters at the class level for a file
                int lineNumber = getIntegerValueOf(nextElement, NUMBER);
                
                fileNode.addCounters(lineNumber, lineBranchCoverage.getCovered(), lineBranchCoverage.getMissed());
                fileNode.addMCDCPairCounters(lineNumber, mcdcPairLineCoverage.getCovered(), mcdcPairLineCoverage.getMissed());
                fileNode.addFunctionCallCounters(lineNumber, functionCallLineCoverage.getCovered(), functionCallLineCoverage.getMissed());
            }
        }
        else if (classOrMethodElement(nextElement)) {
            coverageMap.put(Metric.FUNCTION, processClassMethodStart(nextElement, coverageMap.get(Metric.FUNCTION)));
            runReadClassOrMethod = true;
        }
        
        return runReadClassOrMethod;
    }

    private boolean classOrMethodElement(final StartElement nextElement) {
        return METHOD.equals(nextElement.getName()) || CLASS.equals(nextElement.getName());
    }
    
    protected void processClassMethodEnd(final Node node, final Map<Metric, Coverage> coverageMap) {
    
        node.addValue(coverageMap.get(Metric.LINE));
        
        if (coverageMap.get(Metric.MCDC_PAIR).isSet()) {
            node.addValue(coverageMap.get(Metric.MCDC_PAIR));
        }
        if (coverageMap.get(Metric.FUNCTION_CALL).isSet()) {
            node.addValue(coverageMap.get(Metric.FUNCTION_CALL));
        }
        if (coverageMap.get(Metric.FUNCTION).isSet()) {
            node.addValue(coverageMap.get(Metric.FUNCTION));
        }
        if (coverageMap.get(Metric.BRANCH).isSet()) {
            node.addValue(coverageMap.get(Metric.BRANCH));
        }
    }

    @Override 
    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.CognitiveComplexity"})
    protected void readClassOrMethod(final XMLEventReader reader, 
            final FileNode fileNode, final Node parentNode, 
            final StartElement element, final String fileName, final FilteredLog log)
                throws XMLStreamException {    
        Map<Metric, Coverage> coverageMap = new HashMap<>();
        
        coverageMap.put(Metric.LINE, Coverage.nullObject(Metric.LINE));
        coverageMap.put(Metric.BRANCH, Coverage.nullObject(Metric.BRANCH));
        coverageMap.put(Metric.MCDC_PAIR, Coverage.nullObject(Metric.MCDC_PAIR));
        coverageMap.put(Metric.FUNCTION_CALL, Coverage.nullObject(Metric.FUNCTION_CALL));
        coverageMap.put(Metric.FUNCTION, Coverage.nullObject(Metric.FUNCTION));

        Node node = createNode(parentNode, element, log);
        getOptionalValueOf(element, COMPLEXITY)
                .ifPresent(c -> node.addValue(new CyclomaticComplexity(readComplexity(c))));

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                var nextElement = event.asStartElement();

                if (processStartElement(nextElement, element, fileNode, coverageMap)) {
                    readClassOrMethod(reader, fileNode, node, nextElement, fileName, log);
                }
            }

            else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (CLASS.equals(endElement.getName()) || METHOD.equals(endElement.getName())) {
                    processClassMethodEnd(node, coverageMap);
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

