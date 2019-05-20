/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.interpretation;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.google.auto.service.AutoService;
import com.powsybl.cgmes.catalog.CatalogLocation;
import com.powsybl.cgmes.interpretation.report.CatalogInterpretationReport;
import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import com.powsybl.tools.ToolRunningContext;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
@AutoService(Tool.class)
public class InterpretationTool implements Tool {

    private static final String INPUT_PATH = "input-path";
    private static final String INPUT_PATTERN = "input-pattern";
    private static final String OUTPUT_PATH = "output-path";
    private static final String BOUNDARY_PATH = "boundary-path";

    @Override
    public Command getCommand() {
        return new Command() {

            @Override
            public String getDescription() {
                return "Infere best interpretation of all CGMES models found in a folder";
            }

            @Override
            public String getName() {
                return "cgmes-model-interpretation";
            }

            @Override
            public Options getOptions() {
                Options options = new Options();
                options.addOption(Option.builder()
                    .longOpt(INPUT_PATH)
                    .desc("input path")
                    .hasArg()
                    .argName("INPUT_PATH")
                    .required()
                    .build());
                options.addOption(Option.builder()
                    .longOpt(INPUT_PATTERN)
                    .desc("pattern for model names to be considered")
                    .hasArg()
                    .argName("INPUT_PATTERN")
                    .required()
                    .build());
                options.addOption(Option.builder()
                    .longOpt(OUTPUT_PATH)
                    .desc("output path")
                    .hasArg()
                    .argName("OUTPUT_PATH")
                    .required()
                    .build());
                options.addOption(Option.builder()
                    .longOpt(BOUNDARY_PATH)
                    .desc("path for boundary data")
                    .hasArg()
                    .argName("BOUNDARY_PATH")
                    .required(false)
                    .build());
                return options;
            }

            @Override
            public String getTheme() {
                return "Data conversion";
            }

            @Override
            public String getUsageFooter() {
                return null;
            }
        };
    }

    @Override
    public void run(CommandLine line, ToolRunningContext context) throws Exception {
        String inputPath = line.getOptionValue(INPUT_PATH);
        String inputPattern = line.getOptionValue(INPUT_PATTERN);
        String outputPath = line.getOptionValue(OUTPUT_PATH);
        String boundaryPath = line.getOptionValue(BOUNDARY_PATH);

        CatalogLocation location = new CatalogLocation() {
            public Path dataRoot() {
                return Paths.get(inputPath);
            }

            public Path boundary() {
                return Paths.get(boundaryPath);
            }
        };
        new CatalogInterpretationReport(Paths.get(outputPath)).report(
            new CatalogInterpretation(location, InterpretationAlternatives.configured())
                .reviewAll(inputPattern));
    }
}
