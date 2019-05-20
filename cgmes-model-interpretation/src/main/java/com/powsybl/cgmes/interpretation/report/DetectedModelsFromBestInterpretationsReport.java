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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.powsybl.cgmes.interpretation.InterpretationResults;
import com.powsybl.cgmes.interpretation.InterpretationResults.InterpretationAlternativeResults;
import com.powsybl.cgmes.interpretation.model.interpreted.DetectedEquipmentModel;
import com.powsybl.commons.io.table.Column;
import com.powsybl.commons.io.table.CsvTableFormatterFactory;
import com.powsybl.commons.io.table.TableFormatter;
import com.powsybl.commons.io.table.TableFormatterConfig;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public class DetectedModelsFromBestInterpretationsReport extends AbstractReport {

    public DetectedModelsFromBestInterpretationsReport(Path output) {
        super(output);
    }

    public void report(Collection<InterpretationResults> results) throws IOException {
        Map<String, DetectedEquipmentModel> detectedModels = new HashMap<>();
        results
            .forEach(r -> addDetectedModelsFromBestInterpretationAlternative(r, detectedModels));
        write(sortByComplexity(detectedModels.values()));
    }

    List<DetectedEquipmentModel> sortByComplexity(Collection<DetectedEquipmentModel> detectedModels) {
        Comparator<DetectedEquipmentModel> byComplexity = (DetectedEquipmentModel m1, DetectedEquipmentModel m2) -> {
            String code1 = m1.code();
            String code2 = m2.code();
            int c = Integer.compare(code1.length(), code2.length());
            if (c == 0) {
                c = code1.compareTo(code2);
            }
            return c;
        };
        List<DetectedEquipmentModel> sorted = new ArrayList<>(detectedModels);
        Collections.sort(sorted, byComplexity);
        return sorted;
    }

    private void addDetectedModelsFromBestInterpretationAlternative(
        InterpretationResults interpretation,
        Map<String, DetectedEquipmentModel> targetDetectedModels) {

        // TODO We do not need to sort,
        // just select the alternative with best (lowest) error
        // In case of same errors, the alternative with less changes from default
        // (should be equivalent to minimal length of string description)
        Collection<InterpretationAlternativeResults> alternatives = interpretation.interpretationAlternativeResults()
            .values();
        Comparator<InterpretationAlternativeResults> byError = (
            InterpretationAlternativeResults a1,
            InterpretationAlternativeResults a2) -> {
            int cp = Double.compare(a1.error(), a2.error());
            if (cp == 0) {
                // TODO Comparator by "priority" should be provided by the Alternative
                return Integer.compare(a1.alternative().length(), a2.alternative().length());
            }
            return cp;
        };
        List<InterpretationAlternativeResults> alternativesSorted = new ArrayList<>(alternatives);
        Collections.sort(alternativesSorted, byError);

        addDetectedModels(alternativesSorted.get(0).detectedModels(), targetDetectedModels);
    }

    private void addDetectedModels(Map<String, DetectedEquipmentModel> source,
        Map<String, DetectedEquipmentModel> target) {
        source.keySet().forEach(m -> {
            DetectedEquipmentModel sourcem = source.get(m);
            DetectedEquipmentModel targetm = target.computeIfAbsent(m,
                id -> new DetectedEquipmentModel(sourcem.detectedBranchModels()));
            targetm.incTotal(sourcem.total());
            targetm.incCalculated(sourcem.calculated());
            targetm.incOk(sourcem.ok());
        });
    }

    private void write(List<DetectedEquipmentModel> detectedModels) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(
            outputFile("DetectedModelsFromBestInterpretations", "csv"),
            StandardCharsets.UTF_8)) {

            TableFormatterConfig csvConfig = new TableFormatterConfig(Locale.US, ',', "-", true, false);
            CsvTableFormatterFactory csvFactory = new CsvTableFormatterFactory();
            Column[] columns = new Column[] {
                new Column("code"),
                new Column("total"),
                new Column("calculated"),
                new Column("ok"),
                new Column("evaluationCode")
            };
            try (TableFormatter formatter = csvFactory.create(w, "All Detected Equipment Model", csvConfig, columns)) {
                detectedModels.forEach(model -> {
                    try {
                        formatter
                            .writeCell(model.code())
                            .writeCell(model.total())
                            .writeCell(model.calculated())
                            .writeCell(model.ok())
                            .writeCell(model.conversionCode());
                    } catch (IOException x) {
                        // Ignored
                    }
                });
            }
        }
    }
}
