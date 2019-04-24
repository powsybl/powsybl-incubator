/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.interpretation.report;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.interpretation.Configuration;
import com.powsybl.cgmes.interpretation.InterpretationResults;
import com.powsybl.cgmes.interpretation.InterpretationResults.InterpretationAlternativeResults;
import com.powsybl.cgmes.interpretation.NodeInterpretationResult;
import com.powsybl.cgmes.interpretation.model.interpreted.DetectedEquipmentModel;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative;
import com.powsybl.commons.io.table.Column;
import com.powsybl.commons.io.table.CsvTableFormatterFactory;
import com.powsybl.commons.io.table.TableFormatter;
import com.powsybl.commons.io.table.TableFormatterConfig;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public class CatalogInterpretationReport extends AbstractReport {

    public CatalogInterpretationReport(Path output) {
        super(output);
    }

    @Override
    public void report(Collection<InterpretationResults> results) throws IOException {
        // TODO comparator should be provided by corresponding class
        // This comparator by error is duplicated in different reports
        Comparator<InterpretationResults> byError = (
            InterpretationResults r1,
            InterpretationResults r2) -> Double.compare(r1.error(), r2.error());
        List<InterpretationResults> sortedResults = new ArrayList<>(results);
        sortedResults.sort(byError.reversed());
        write(sortedResults);
    }

    private void write(List<InterpretationResults> results) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(
            outputFile("CatalogInterpretation"), StandardCharsets.UTF_8)) {
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

    private String build(InterpretationResults interpretation) {
        if (interpretation.exception() != null) {
            return interpretation.exception().getMessage();
        }

        Collection<InterpretationAlternativeResults> alternatives = interpretation
            .interpretationAlternativeResults().values();
        Comparator<InterpretationAlternativeResults> byError = (
            InterpretationAlternativeResults a1,
            InterpretationAlternativeResults a2) -> {
            int c = Double.compare(a1.error(), a2.error());
            if (c == 0) {
                return Integer.compare(a1.alternative().length(), a2.alternative().length());
            }
            return c;
        };

        List<InterpretationAlternativeResults> sorted = new ArrayList<>(alternatives);
        Collections.sort(sorted, byError);

        StringBuilder reportBuilder = new StringBuilder();
        reportBuilder.append(String.format("----> MODEL %s ", interpretation.cgmesName()));
        reportBuilder.append(System.getProperty("line.separator"));
        sorted.forEach(ar -> {
            try {
                build(ar, reportBuilder);
            } catch (IOException e) {
                // Ignored
            }
        });
        return reportBuilder.toString();
    }

    private void build(
        InterpretationAlternativeResults alternativeResults,
        StringBuilder modelReportBuilder) throws IOException {

        header(alternativeResults.alternative(), modelReportBuilder);
        balance(alternativeResults, modelReportBuilder);
        badNodes(alternativeResults, modelReportBuilder);
        badVoltageNodes(alternativeResults, modelReportBuilder);
        detectedModels(alternativeResults, modelReportBuilder);
    }

    private void header(
        InterpretationAlternative alternative,
        StringBuilder modelReportBuilder) {

        LOG.debug("----> INTERPRETATION ALTERNATIVE {}", alternative);
        modelReportBuilder.append(String.format("----> %s %s",
            Configuration.REPORT_INTERPRETATION_ALTERNATIVE_TITLE,
            alternative.toString()));
        modelReportBuilder.append(System.getProperty("line.separator"));
    }

    private void balance(InterpretationAlternativeResults alternativeResults,
        StringBuilder modelReportBuilder)
        throws IOException {

        Collection<NodeInterpretationResult> nodesResults = alternativeResults.nodesResults();
        long totalNodes = nodesResults.size();
        long notCalculatedNodes = nodesResults.stream()
            .filter(b -> !b.isCalculated() && !b.isIsolated())
            .count();
        long badNodes = nodesResults.stream()
            .filter(NodeInterpretationResult::isBad)
            .count();
        long badVoltageNodes = nodesResults.stream()
            .filter(b -> b.isCalculated() && b.isBadVoltage() && !b.isOk())
            .count();
        long okNodes = nodesResults.stream()
            .filter(NodeInterpretationResult::isCalculated)
            .filter(NodeInterpretationResult::isOk)
            .count();
        long isolatedNodes = nodesResults.stream()
            .filter(NodeInterpretationResult::isIsolated)
            .count();
        double badVoltageNodesError = nodesResults.stream()
            .filter(b -> b.isCalculated() && b.isBadVoltage() && !b.isOk())
            .map(NodeInterpretationResult::error)
            .mapToDouble(Double::doubleValue)
            .sum();
        double badNodesError = nodesResults.stream()
            .filter(b -> b.isCalculated() && !b.isBadVoltage() && !b.isOk())
            .map(NodeInterpretationResult::error)
            .mapToDouble(Double::doubleValue)
            .sum();

        LOG.debug(
            "BALANCE -- total error;total nodes;isolated nodes;non-calculated nodes;ok nodes;bad error;bad nodes;pct;badVoltage error;badVoltage nodes;pct");
        LOG.debug("{};{};{};{};{};{};{};{};{};{};{}",
            alternativeResults.error(),
            totalNodes,
            isolatedNodes,
            notCalculatedNodes,
            okNodes,
            badNodesError,
            badNodes,
            (double) badNodes / (totalNodes - isolatedNodes) * 100.0,
            badVoltageNodesError,
            badVoltageNodes,
            (double) badVoltageNodes / (totalNodes - isolatedNodes) * 100.0);

        TableFormatterConfig config = new TableFormatterConfig(Locale.US, ',', "-", true, true);
        CsvTableFormatterFactory factory = new CsvTableFormatterFactory();
        Column[] columns = new Column[] {
            new Column("total error"),
            new Column("total nodes"),
            new Column("isolated nodes"),
            new Column("non-calculated node"),
            new Column("ok nodes"),
            new Column("bad error"),
            new Column("bad nodes"),
            new Column("pct"),
            new Column("badVoltage error"),
            new Column("badVoltage nodes"),
            new Column("pct")
        };
        try (Writer writer = new StringWriter();
            TableFormatter formatter = factory.create(writer, "BALANCE", config, columns)) {
            try {
                formatter
                    .writeCell(alternativeResults.error())
                    .writeCell((int) totalNodes)
                    .writeCell((int) isolatedNodes)
                    .writeCell((int) notCalculatedNodes)
                    .writeCell((int) okNodes)
                    .writeCell(badNodesError)
                    .writeCell((int) badNodes)
                    .writeCell((double) badNodes / (totalNodes - isolatedNodes) * 100.0)
                    .writeCell(badVoltageNodesError)
                    .writeCell((int) badVoltageNodes)
                    .writeCell((double) badVoltageNodes / (totalNodes - isolatedNodes) * 100.0);
            } catch (IOException x) {
                // Ignored
            }
            modelReportBuilder.append(writer.toString());
        }
    }

    private void badNodes(InterpretationAlternativeResults alternativeResults,
        StringBuilder modelReportBuilder) {
        boolean showOnlyBadNodes = true;
        boolean showOnlyBadVoltageNodes = false;
        nodes("BAD NODES", alternativeResults, modelReportBuilder, showOnlyBadVoltageNodes,
            showOnlyBadNodes);
    }

    private void badVoltageNodes(InterpretationAlternativeResults alternativeResults,
        StringBuilder modelReportBuilder) {
        boolean showOnlyBadNodes = false;
        boolean showOnlyBadVoltageNodes = true;
        nodes("BAD VOLTAGE NODES", alternativeResults, modelReportBuilder,
            showOnlyBadVoltageNodes, showOnlyBadNodes);
    }

    private void nodes(String prefix, InterpretationAlternativeResults alternativeResults,
        StringBuilder modelReportBuilder, boolean showOnlyBadVoltageNodes, boolean showOnlyBadNodes) {
        LOG.debug("{} -- id;balanceP;balanceQ;lines;xfmr2s;xfmr3s;nodes", prefix);

        TableFormatterConfig config = new TableFormatterConfig(Locale.US, ',', "-", true, true);
        CsvTableFormatterFactory factory = new CsvTableFormatterFactory();
        Column[] columns = new Column[] {
            new Column("id"),
            new Column("balanceP"),
            new Column("balanceQ"),
            new Column("lines"),
            new Column("xfmr2s"),
            new Column("xfmr3s"),
            new Column("nodes")
        };
        try (Writer writer = new StringWriter();
            TableFormatter formatter = factory.create(writer, prefix, config, columns)) {
            alternativeResults.nodesResults().stream()
                .filter(b -> {
                    boolean badError = b.isCalculated() && !b.isOk();
                    return showOnlyBadNodes && !b.isBadVoltage() && badError
                        || showOnlyBadVoltageNodes && b.isBadVoltage() && badError;
                })
                .limit(Configuration.REPORT_SUMMARY_NUM_BAD_NODES)
                .forEach(b -> {
                    LOG.debug("{},{},{},{},{},{},{}", b.z0node().nodeIds().iterator().next(),
                        b.p(), b.q(),
                        b.numLines(), b.numTransformers2(), b.numTransformers3(), b.z0node());
                    try {
                        formatter
                            .writeCell(b.z0node().nodeIds().iterator().next())
                            .writeCell(b.p())
                            .writeCell(b.q())
                            .writeCell(b.numLines())
                            .writeCell(b.numTransformers2())
                            .writeCell(b.numTransformers3())
                            .writeCell(b.z0node().toString());
                    } catch (IOException x) {
                        // Ignored
                    }
                });
            modelReportBuilder.append(writer.toString());
        } catch (IOException x) {
            // Ignored
        }
    }

    private void detectedModels(InterpretationAlternativeResults alternativeResults,
        StringBuilder modelReportBuilder) {
        Map<String, DetectedEquipmentModel> sortedByModelReport = new TreeMap<>(
            (String s1, String s2) -> {
                if (s1.length() == s2.length()) {
                    return s1.compareTo(s2);
                }
                return Integer.compare(s1.length(), s2.length());
            });

        LOG.debug("DETECTED MODEL -- code,total,calculated,ok,evaluationCode");
        TableFormatterConfig config = new TableFormatterConfig(Locale.US, ',', "-", true, true);
        CsvTableFormatterFactory factory = new CsvTableFormatterFactory();
        Column[] columns = new Column[] {
            new Column("code"),
            new Column("total"),
            new Column("calculated"),
            new Column("ok"),
            new Column("evaluationCode")
        };

        sortedByModelReport.putAll(alternativeResults.detectedModels());
        try (Writer writer = new StringWriter();
            TableFormatter formatter = factory.create(writer, "DETECTED MODEL", config, columns)) {
            sortedByModelReport.keySet().forEach(model -> {
                LOG.debug("{},{},{},{},{}",
                    sortedByModelReport.get(model).code(),
                    sortedByModelReport.get(model).total(),
                    sortedByModelReport.get(model).calculated(),
                    sortedByModelReport.get(model).ok(),
                    sortedByModelReport.get(model).conversionCode());
                try {
                    formatter
                        .writeCell(sortedByModelReport.get(model).code())
                        .writeCell(sortedByModelReport.get(model).total())
                        .writeCell(sortedByModelReport.get(model).calculated())
                        .writeCell(sortedByModelReport.get(model).ok())
                        .writeCell(sortedByModelReport.get(model).conversionCode());
                } catch (IOException x) {
                    // Ignored
                }
            });
            modelReportBuilder.append(writer.toString());
        } catch (IOException e) {
            // Ignored
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(CatalogInterpretationReport.class);
}
