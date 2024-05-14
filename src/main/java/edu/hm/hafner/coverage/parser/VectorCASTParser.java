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
        
    private void processClassMethodStart(final StartElement nextElement, final CoverageGroup covGroup) {
        if (METHOD.equals(nextElement.getName())) {
            Coverage functionMethodCoverage;
            functionMethodCoverage = readFunctionCoverage(nextElement);
            covGroup.add(functionMethodCoverage, Metric.FUNCTION);
        }        
    }

    protected void processStartElement(final Node node, final XMLEventReader reader, final XMLEvent event, final StartElement element, final String fileName, final FileNode fileNode, 
            final CoverageGroup covGroup, final FilteredLog log) throws XMLStreamException {
        var nextElement = event.asStartElement();

        if (LINE.equals(nextElement.getName())) {
            Coverage lineBranchCoverage;
            Coverage currentLineCoverage;
            Coverage mcdcPairLineCoverage = Coverage.nullObject(Metric.MCDC_PAIR);
            Coverage functionCallLineCoverage = Coverage.nullObject(Metric.FUNCTION_CALL);
            if (isBranchCoverage(nextElement)) {
                lineBranchCoverage = readBranchCoverage(nextElement);
                currentLineCoverage = computeLineCoverage(lineBranchCoverage.getCovered());
                covGroup.add(lineBranchCoverage, Metric.BRANCH);

                if (isMcdcPairCoverage(nextElement)) {
                    mcdcPairLineCoverage = readMcdcPairCoverage(nextElement);
                    covGroup.add(mcdcPairLineCoverage, Metric.MCDC_PAIR);
                }
                if (isFunctionCallCoverage(nextElement)) {
                    functionCallLineCoverage = readFunctionCallCoverage(nextElement);
                    covGroup.add(functionCallLineCoverage, Metric.FUNCTION_CALL);
                }
            } 
            else if (isFunctionCallCoverage(nextElement)) {
                functionCallLineCoverage = readFunctionCallCoverage(nextElement);
                covGroup.add(functionCallLineCoverage, Metric.FUNCTION_CALL);
                int lineHits = getIntegerValueOf(nextElement, HITS);
                currentLineCoverage = computeLineCoverage(lineHits);
                lineBranchCoverage = currentLineCoverage;
            }
            else {
                int lineHits = getIntegerValueOf(nextElement, HITS);
                currentLineCoverage = computeLineCoverage(lineHits);
                lineBranchCoverage = currentLineCoverage;
            }
            
            covGroup.setCoverage(currentLineCoverage, Metric.LINE);

            if (CLASS.equals(element.getName())) { // Use the line counters at the class level for a file
                int lineNumber = getIntegerValueOf(nextElement, NUMBER);
                
                fileNode.addCounters(lineNumber, lineBranchCoverage.getCovered(), lineBranchCoverage.getMissed());
                fileNode.addMCDCPairCounters(lineNumber, mcdcPairLineCoverage.getCovered(), mcdcPairLineCoverage.getMissed());
                fileNode.addFunctionCallCounters(lineNumber, functionCallLineCoverage.getCovered(), functionCallLineCoverage.getMissed());
            }
        }
        else if (classOrMethodElement(nextElement)) {
            processClassMethodStart(nextElement, covGroup);
            readClassOrMethod(reader, fileNode, node, nextElement, fileName, log);
        }
    }

    private boolean classOrMethodElement(final StartElement nextElement) {
        return METHOD.equals(nextElement.getName()) || CLASS.equals(nextElement.getName());
    }
    
    protected void processClassMethodEnd(final Node node, final CoverageGroup covGroup) {
    
        node.addValue(covGroup.lineCoverage);
        
        if (covGroup.mcdcPairCoverage.isSet()) {
            node.addValue(covGroup.mcdcPairCoverage);
        }
        if (covGroup.functionCallCoverage.isSet()) {
            node.addValue(covGroup.functionCallCoverage);
        }
        if (covGroup.functionCoverage.isSet()) {
            node.addValue(covGroup.functionCoverage);
        }
        if (covGroup.branchCoverage.isSet()) {
            node.addValue(covGroup.branchCoverage);
        }
    }

    @Override 
    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.CognitiveComplexity"})
    protected void readClassOrMethod(final XMLEventReader reader, 
            final FileNode fileNode, final Node parentNode, 
            final StartElement element, final String fileName, final FilteredLog log)
                throws XMLStreamException {
        CoverageGroup covGroup = new CoverageGroup();
        
        Node node = createNode(parentNode, element, log);
        getOptionalValueOf(element, COMPLEXITY)
                .ifPresent(c -> node.addValue(new CyclomaticComplexity(readComplexity(c))));

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                processStartElement(node, reader, event, element, fileName, fileNode, covGroup, log);
            }

            else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (CLASS.equals(endElement.getName()) || METHOD.equals(endElement.getName())) {
                    processClassMethodEnd(node, covGroup);
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

    public static class CoverageGroup {
        
        public Coverage lineCoverage = Coverage.nullObject(Metric.LINE);
        public Coverage branchCoverage = Coverage.nullObject(Metric.BRANCH);
        public Coverage mcdcPairCoverage = Coverage.nullObject(Metric.MCDC_PAIR);
        public Coverage functionCallCoverage = Coverage.nullObject(Metric.FUNCTION_CALL);
        public Coverage functionCoverage = Coverage.nullObject(Metric.FUNCTION);
        
        /**
         * Sets the coverage specified by the metrics.
         *
         * @param coverage
         *          the coverage to be set
         *
         * @param metric
         *          the indicator as to which metric to set
         */

        public void setCoverage(final Coverage coverage, final Metric metric) {
            switch (metric) {
                case LINE:
                    this.lineCoverage = coverage;
                    break;
                case BRANCH:
                    this.branchCoverage = coverage;
                    break;
                case MCDC_PAIR:
                    this.mcdcPairCoverage = coverage;
                    break;
                case FUNCTION_CALL:
                    this.functionCallCoverage = coverage;
                    break;
                case FUNCTION:
                    this.functionCoverage = coverage;
                    break;
                default:
                    break;
            }
        }

        /**
         * Adds the input coverage, specified by the metrics, to the coverage
         *
         * @param coverage
         *          the coverage to be set
         *
         * @param metric
         *          the indicator as to which metric to set
         */
        public void add(final Coverage coverage, final Metric metric) {
            switch (metric) {
                case LINE:
                    this.lineCoverage = this.lineCoverage.add(coverage);
                    break;
                case BRANCH:
                    this.branchCoverage = this.branchCoverage.add(coverage);
                    break;
                case MCDC_PAIR:
                    this.mcdcPairCoverage = this.mcdcPairCoverage.add(coverage);
                    break;
                case FUNCTION_CALL:
                    this.functionCallCoverage = this.functionCallCoverage.add(coverage);
                    break;
                case FUNCTION:
                    this.functionCoverage = this.functionCoverage.add(coverage);
                    break;
                default:
                    break;
            }
        }
    }
}

