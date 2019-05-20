package com.powsybl.cgmes.conversion.validation.report;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.conversion.validation.TerminalFlow;
import com.powsybl.cgmes.conversion.validation.ValidationResults;
import com.powsybl.cgmes.conversion.validation.ValidationResults.ValidationAlternativeResults;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative;
import com.powsybl.commons.io.table.Column;
import com.powsybl.commons.io.table.CsvTableFormatterFactory;
import com.powsybl.commons.io.table.TableFormatter;
import com.powsybl.commons.io.table.TableFormatterConfig;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */

public class CatalogValidationReport extends AbstractReport {

    private static final int SHOW_BRANCH_ENDS = 5;

    public CatalogValidationReport(Path output) {
        super(output);
    }

    public void report(Collection<ValidationResults> results) throws IOException {
        Comparator<ValidationResults> byFailed = (
            ValidationResults o1,
            ValidationResults o2) -> Integer.compare(o1.failedCount(), o2.failedCount());
        List<ValidationResults> sortedResults = new ArrayList<>(results);
        sortedResults.sort(byFailed.reversed());
        write(sortedResults);
    }

    private void write(Collection<ValidationResults> results) throws IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime dateTime = LocalDateTime.now();
        String formattedDateTime = dateTime.format(formatter);
        try (BufferedWriter w = Files.newBufferedWriter(
            outputFile("CatalogValidation"), StandardCharsets.UTF_8)) {
            results.forEach(r -> {
                try {
                    w.write(build(r));
                    w.newLine();
                    w.flush();
                } catch (IOException x) {
                    LOG.warn("Error writing report for model {} {}", r.cgmesName(), x.getMessage());
                }
            });
        }
    }

    private String build(ValidationResults validation) {

        if (validation.exception() != null) {
            return validation.exception().getMessage();
        }

        Map<InterpretationAlternative, ValidationAlternativeResults> validationAlternativeResults = validation.validationAlternativeResults();
        Comparator<Map.Entry<InterpretationAlternative, ValidationAlternativeResults>> byFailedCount = (
            Entry<InterpretationAlternative, ValidationAlternativeResults> o1,
            Entry<InterpretationAlternative, ValidationAlternativeResults> o2) -> Integer.compare(o1.getValue().failedCount(), o2.getValue().failedCount());

        Map<InterpretationAlternative, ValidationAlternativeResults> sortedValidationAlternativeResults = validationAlternativeResults
            .entrySet().stream().sorted(byFailedCount.reversed())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> {
                throw new AssertionError();
            }, LinkedHashMap::new));

        StringBuilder modelReportBuilder = new StringBuilder();
        modelReportBuilder.append(String.format("----> MODEL %s ", validation.cgmesName()));
        modelReportBuilder.append(System.getProperty("line.separator"));
        sortedValidationAlternativeResults.keySet()
            .forEach(alternative -> {
                try {
                    conversionValidationReport(alternative,
                        validationAlternativeResults.get(alternative), modelReportBuilder);
                } catch (IOException e) {
                    // Ignored
                }
            });
        return modelReportBuilder.toString();
    }

    private void conversionValidationReport(InterpretationAlternative alternative,
        ValidationAlternativeResults validationAlternativeResults, StringBuilder modelReportBuilder) throws IOException {
        header(alternative, modelReportBuilder);
        flow(validationAlternativeResults, modelReportBuilder);
        branchEnd(validationAlternativeResults, modelReportBuilder);
    }

    private void header(InterpretationAlternative alternative,
        StringBuilder modelReportBuilder) {
        LOG.debug("----> INTERPRETATION ALTERNATIVE {}", alternative);
        modelReportBuilder.append(String.format("----> config %s", alternative.toString()));
        modelReportBuilder.append(System.getProperty("line.separator"));
    }

    private void flow(ValidationAlternativeResults validationAlternativeResults,
        StringBuilder modelReportBuilder)
        throws IOException {

        long totalBranchEnds = validationAlternativeResults.terminalFlows().values().size();
        long nonCalculated = validationAlternativeResults.nonCalculated();
        long ko = validationAlternativeResults.failedCount();
        long ok = totalBranchEnds - (nonCalculated + ko);

        LOG.debug("FLOW -- total branch ends;non-calculated;ko;ok");
        LOG.debug("{};{};{};{}", totalBranchEnds, nonCalculated, ko, ok);

        TableFormatterConfig config = new TableFormatterConfig(Locale.US, ',', "-", true, true);
        CsvTableFormatterFactory factory = new CsvTableFormatterFactory();
        Column[] columns = new Column[] {
            new Column("total branch ends"),
            new Column("non-calculated"),
            new Column("ko"),
            new Column("ok")
        };
        try (Writer writer = new StringWriter();
            TableFormatter formatter = factory.create(writer, "FLOW", config, columns)) {
            try {
                formatter
                    .writeCell((int) totalBranchEnds)
                    .writeCell((int) nonCalculated)
                    .writeCell((int) ko)
                    .writeCell((int) ok);
            } catch (IOException x) {
                // Ignored
            }
            modelReportBuilder.append(writer.toString());
        }
    }

    private void branchEnd(ValidationAlternativeResults validationAlternativeResults,
        StringBuilder modelReportBuilder) {

        if (validationAlternativeResults.failedCount() == 0) {
            return;
        }

        Comparator<Map.Entry<String, TerminalFlow>> byFlowError = (
            Entry<String, TerminalFlow> o1,
            Entry<String, TerminalFlow> o2) -> Double.compare(o1.getValue().flowError(), o2.getValue().flowError());

        Map<String, TerminalFlow> sortedByModelReport = validationAlternativeResults.terminalFlows().entrySet().stream()
            .sorted(byFlowError.reversed())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> {
                throw new AssertionError();
            }, LinkedHashMap::new));

        LOG.debug("BRANCH END -- id;type;pCgmes;qCgmes;pIidm;qIidm");
        TableFormatterConfig config = new TableFormatterConfig(Locale.US, ',', "-", true, true);
        CsvTableFormatterFactory factory = new CsvTableFormatterFactory();
        Column[] columns = new Column[] {
            new Column("id"),
            new Column("type"),
            new Column("pCgmes"),
            new Column("qCgmes"),
            new Column("pIidm"),
            new Column("qIidm")
        };

        try (Writer writer = new StringWriter();
            TableFormatter formatter = factory.create(writer, "BRANCH END", config, columns)) {
            sortedByModelReport.entrySet().stream()
                .filter(e -> e.getValue().flowError() > ValidationAlternativeResults.FLOW_THRESHOLD).limit(SHOW_BRANCH_ENDS)
                .forEach(e -> {
                    LOG.debug("{},{},{},{},{}",
                        e.getValue().id(),
                        e.getValue().code(),
                        e.getValue().pCgmes(),
                        e.getValue().qCgmes(),
                        e.getValue().pIidm(),
                        e.getValue().qIidm());
                    try {
                        formatter
                            .writeCell(e.getValue().id())
                            .writeCell(e.getValue().code())
                            .writeCell(e.getValue().pCgmes())
                            .writeCell(e.getValue().qCgmes())
                            .writeCell(e.getValue().pIidm())
                            .writeCell(e.getValue().qIidm());
                    } catch (IOException x) {
                        // Ignored
                    }
                });
            modelReportBuilder.append(writer.toString());
        } catch (IOException e) {
            // Ignored
        }
    }

    static final Logger LOG = LoggerFactory.getLogger(CatalogValidationReport.class);
}
