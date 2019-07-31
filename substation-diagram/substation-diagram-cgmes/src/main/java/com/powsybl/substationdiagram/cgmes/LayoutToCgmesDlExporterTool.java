/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.cgmes;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableMap;
import com.powsybl.cgmes.dl.conversion.CgmesDLExporter;
import com.powsybl.cgmes.dl.conversion.CgmesDLUtils;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.datasource.FileDataSource;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.substationdiagram.layout.*;
import com.powsybl.substationdiagram.library.ResourcesComponentLibrary;
import com.powsybl.substationdiagram.svg.DefaultSubstationDiagramStyleProvider;
import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import com.powsybl.tools.ToolOptions;
import com.powsybl.tools.ToolRunningContext;
import com.powsybl.triplestore.api.TripleStore;
import com.powsybl.triplestore.api.TripleStoreFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.eu>
 */
@AutoService(Tool.class)
public class LayoutToCgmesDlExporterTool implements Tool {

    private static final String INPUT_FILE = "input-file";
    private static final String OUTPUT_DIR = "output-dir";
    private static final String VOLTAGE_LEVEL_LAYOUT = "voltage-level-layout";
    private static final String SUBSTATION_LAYOUT = "substation-layout";
    private static final String DEFAULT_VOLTAGE_LAYOUT = "auto-without-extensions";
    private static final String DEFAULT_SUBSTATION_LAYOUT = "horizontal";

    private final Map<String, VoltageLevelLayoutFactory> voltageLevelsLayouts
            = ImmutableMap.of("auto-extensions", new PositionVoltageLevelLayoutFactory(new PositionFromExtension()),
            DEFAULT_VOLTAGE_LAYOUT, new PositionVoltageLevelLayoutFactory(new PositionFree()));

    private final Map<String, SubstationLayoutFactory> substationsLayouts
            = ImmutableMap.of(DEFAULT_SUBSTATION_LAYOUT, new HorizontalSubstationLayoutFactory(),
            "vertical", new VerticalSubstationLayoutFactory());

    @Override
    public Command getCommand() {
        return new Command() {

            @Override
            public String getName() {
                return "generate-cgmes-dl";
            }

            @Override
            public String getTheme() {
                return "Substation diagram";
            }

            @Override
            public String getDescription() {
                return "apply a layout to a network, generate and export a new CGMES-DL profile";
            }

            @Override
            public Options getOptions() {
                Options options = new Options();
                options.addOption(Option.builder().longOpt(INPUT_FILE)
                        .desc("input file")
                        .hasArg()
                        .argName("INPUT_FILE")
                        .required()
                        .build());
                options.addOption(Option.builder().longOpt(OUTPUT_DIR)
                        .desc("output directory")
                        .hasArg()
                        .argName("OUTPUT_DIR")
                        .required()
                        .build());
                options.addOption(Option.builder().longOpt(VOLTAGE_LEVEL_LAYOUT)
                        .desc("voltage level layout")
                        .hasArg()
                        .argName("VOLTAGE LEVEL LAYOUT")
                        .build());
                options.addOption(Option.builder().longOpt(SUBSTATION_LAYOUT)
                        .desc("substation layout")
                        .hasArg()
                        .argName("SUBSTATION LAYOUT")
                        .build());
                return options;
            }

            @Override
            public String getUsageFooter() {
                return "Where SUBSTATION LAYOUT is one of: " + substationsLayouts.keySet().stream().collect(Collectors.joining(", ")) + " (default is: " + DEFAULT_SUBSTATION_LAYOUT + ")"
                        + " and VOLTAGE LEVEL LAYOUT is one of: " + voltageLevelsLayouts.keySet().stream().collect(Collectors.joining(", ")) + " (default is: " + DEFAULT_VOLTAGE_LAYOUT + ")";
            }
        };
    }

    @Override
    public void run(CommandLine line, ToolRunningContext context) throws UnsupportedEncodingException {
        ToolOptions toolOptions = new ToolOptions(line, context);
        Path inputFile = toolOptions.getPath(INPUT_FILE).orElseThrow(() -> new PowsyblException(INPUT_FILE + " parameter is missing"));
        Path outputDir = toolOptions.getPath(OUTPUT_DIR).orElseThrow(() -> new PowsyblException(OUTPUT_DIR + " parameter is missing"));

        String substationLayout = toolOptions.getValue(SUBSTATION_LAYOUT).orElse(DEFAULT_SUBSTATION_LAYOUT);
        String voltageLayout = toolOptions.getValue(VOLTAGE_LEVEL_LAYOUT).orElse(DEFAULT_VOLTAGE_LAYOUT);
        SubstationLayoutFactory sFactory = substationsLayouts.get(substationLayout);
        if (sFactory == null) {
            throw new PowsyblException("invalid " + SUBSTATION_LAYOUT + ": " + substationLayout);
        }
        VoltageLevelLayoutFactory vFactory = voltageLevelsLayouts.get(voltageLayout);
        if (vFactory == null) {
            throw new PowsyblException("invalid " + VOLTAGE_LEVEL_LAYOUT + ": " + voltageLayout);
        }

        context.getOutputStream().println("Loading network '" + inputFile + "'...");
        Network network = Importers.loadNetwork(inputFile);

        context.getOutputStream().println("Generating layout for the network ...");
        ResourcesComponentLibrary componentLibrary = new ResourcesComponentLibrary("/ConvergenceLibrary");
        LayoutToCgmesExtensionsConverter lTranslator = new LayoutToCgmesExtensionsConverter(sFactory, vFactory, new LayoutParameters(), componentLibrary, new DefaultSubstationDiagramStyleProvider(), true);
        lTranslator.convertLayout(network, null);

        context.getOutputStream().println("Exporting network data (including the DL file) to " + outputDir);
        TripleStore tStore = CgmesDLUtils.getTripleStore(network);
        if (tStore == null) {
            tStore = TripleStoreFactory.create();
        }
        CgmesDLExporter dlExporter = new CgmesDLExporter(network, tStore);
        dlExporter.exportDLData(new FileDataSource(outputDir, network.getName()));
    }
}
