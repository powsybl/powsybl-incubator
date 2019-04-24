/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.interpretation.report;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.powsybl.cgmes.catalog.Catalog;
import com.powsybl.cgmes.interpretation.InterpretationResults;
import com.powsybl.cgmes.interpretation.InterpretationResults.InterpretationAlternativeResults;
import com.powsybl.cgmes.interpretation.NodeInterpretationResult;
import com.powsybl.commons.io.table.Column;
import com.powsybl.commons.io.table.CsvTableFormatterFactory;
import com.powsybl.commons.io.table.TableFormatter;
import com.powsybl.commons.io.table.TableFormatterConfig;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public class BestInterpretationReport extends AbstractReport {

    public BestInterpretationReport(Path output) {
        super(output);
    }

    public void report(Collection<InterpretationResults> results) throws IOException {
        Comparator<InterpretationResults> byError = (
            InterpretationResults r1,
            InterpretationResults r2) -> Double.compare(r1.error(), r2.error());
        List<InterpretationResults> sortedResults = new ArrayList<>(results);
        sortedResults.sort(byError.reversed());
        write(sortedResults);
    }

    private void write(List<InterpretationResults> results) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(
            outputFile("BestInterpretation", "csv"),
            StandardCharsets.UTF_8)) {

            TableFormatterConfig config = new TableFormatterConfig(Locale.US, ',', "-", true, false);
            CsvTableFormatterFactory factory = new CsvTableFormatterFactory();
            Column[] columns = new Column[] {
                new Column("TSO"),
                new Column("Best interpretation"),
                new Column("Num nodes"),
                new Column("Pct of bad nodes"),
                new Column("Num bad nodes"),
                new Column("Total error bad nodes"),
                new Column("Total error"),
                new Column("Isolated nodes"),
                new Column("Non-calculated nodes"),
                new Column("Num nodes ok"),
                new Column("Total bad voltage error"),
                new Column("Num bad voltage nodes"),
                new Column("Pct bad voltage nodes"),
                new Column("Model")
            };
            try (TableFormatter formatter = factory.create(w, "BEST INTERPRETATION", config, columns)) {
                results.forEach(r -> write(r, formatter));
            }
        }
    }

    private void write(InterpretationResults interpretation, TableFormatter formatter) {
        if (interpretation.exception() != null) {
            return;
        }

        // Select best alternative results
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
        InterpretationAlternativeResults bestAlternativeResults = sorted.get(0);

        try {
            write(
                interpretation.cgmesName(),
                bestAlternativeResults,
                formatter);
        } catch (IOException e) {
            // Ignored
        }
    }

    private void write(
        String model,
        InterpretationAlternativeResults results,
        TableFormatter formatter) throws IOException {

        Collection<NodeInterpretationResult> nodesResults = results.nodesResults();
        long totalNodes = nodesResults.size();
        long notCalculatedNodes = nodesResults.stream()
            .filter(b -> !b.isCalculated() && !b.isIsolated())
            .count();
        long okNodes = nodesResults.stream()
            .filter(NodeInterpretationResult::isCalculated)
            .filter(NodeInterpretationResult::isOk)
            .count();
        long badNodes = nodesResults.stream()
            .filter(NodeInterpretationResult::isBad)
            .count();
        long isolatedNodes = nodesResults.stream()
            .filter(NodeInterpretationResult::isIsolated)
            .count();
        long badVoltageNodes = nodesResults.stream()
            .filter(b -> b.isCalculated() && b.isBadVoltage() && !b.isOk())
            .count();
        double badNodesError = nodesResults.stream()
            .filter(NodeInterpretationResult::isBad)
            .map(NodeInterpretationResult::error)
            .mapToDouble(Double::doubleValue)
            .sum();
        double badVoltageNodesError = nodesResults.stream()
            .filter(b -> b.isCalculated() && b.isBadVoltage() && !b.isOk())
            .map(NodeInterpretationResult::error)
            .mapToDouble(Double::doubleValue)
            .sum();

        try {
            formatter
                .writeCell(Catalog.tsoNameFromPathname(model))
                .writeCell(results.alternative().toString())
                .writeCell((int) totalNodes)
                .writeCell(1.0 * badNodes / (totalNodes - isolatedNodes))
                .writeCell((int) badNodes)
                .writeCell(badNodesError)
                .writeCell(results.error())
                .writeCell((int) isolatedNodes)
                .writeCell((int) notCalculatedNodes)
                .writeCell((int) okNodes)
                .writeCell(badVoltageNodesError)
                .writeCell((int) badVoltageNodes)
                .writeCell(1.0 * badVoltageNodes / (totalNodes - isolatedNodes))
                .writeCell(model);
        } catch (IOException x) {
            // Ignored
        }
    }
}
