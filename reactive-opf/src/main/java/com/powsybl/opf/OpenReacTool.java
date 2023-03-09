package com.powsybl.opf;

import com.google.auto.service.AutoService;
import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Exporter;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.tools.ConversionToolUtils;
import com.powsybl.opf.parameters.OpenReacParameters;
import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import com.powsybl.tools.ToolRunningContext;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.nio.file.Path;
import java.util.Properties;

import static com.powsybl.iidm.network.tools.ConversionToolUtils.*;

@AutoService(Tool.class)
public class OpenReacTool implements Tool {
    private static final String CASE_FILE = "case-file";
    private static final String OUTPUT_CASE_FORMAT = "output-case-format";
    private static final String OUTPUT_CASE_FILE = "output-case-file";

    @Override
    public Command getCommand() {
        return new Command() {
            @Override
            public String getName() {
                return "openreac";
            }

            @Override
            public String getTheme() {
                return "Optimal Power Flow";
            }

            @Override
            public String getDescription() {
                return "An optimal powerflow on reactive components";
            }

            public Options getOptions() {
                Options options = new Options();
                options.addOption(Option.builder()
                                        .longOpt(CASE_FILE)
                                        .desc("the case path")
                                        .hasArg()
                                        .argName("FILE")
                                        .required()
                                        .build());
                options.addOption(Option.builder()
                                        .longOpt(OUTPUT_CASE_FORMAT)
                                        .desc("modified network output format " + Exporter.getFormats())
                                        .hasArg()
                                        .argName("CASEFORMAT")
                                        .required()
                                        .build());
                options.addOption(Option.builder()
                                        .longOpt(OUTPUT_CASE_FILE)
                                        .desc("modified network base name")
                                        .hasArg()
                                        .argName("FILE")
                                        .required()
                                        .build());
                options.addOption(createImportParametersFileOption());
                options.addOption(createImportParameterOption());
                options.addOption(createExportParametersFileOption());
                options.addOption(createExportParameterOption());
                return options;
            }

            @Override
            public String getUsageFooter() {
                return null;
            }
        };
    }

    @Override
    public void run(CommandLine commandLine, ToolRunningContext context) throws Exception {
        // getting parameters
        Path inputCaseFile = context.getFileSystem().getPath(commandLine.getOptionValue(CASE_FILE));
        Path outputCaseFile = context.getFileSystem().getPath(commandLine.getOptionValue(OUTPUT_CASE_FILE));
        Properties inputParams = readProperties(commandLine, ConversionToolUtils.OptionType.IMPORT, context);

        // loading network
        context.getOutputStream().println("Loading network '" + inputCaseFile + "'...");
        Network network = Network.read(inputCaseFile, context.getShortTimeExecutionComputationManager(),
                ImportConfig.load(), inputParams);
        if (network == null) {
            throw new PowsyblException("Case '" + inputCaseFile + "' not found");
        }

        // Running model
        context.getOutputStream().println("Running OpenReac on the network...");
        OpenReacRunner.runOpenReac(network, network.getVariantManager().getWorkingVariantId(),
                OpenReacParameters.load());
        context.getOutputStream().println("OpenReac optimisation done");

        // Exporting the modified network
        context.getOutputStream().println("Exporting network '" + outputCaseFile + "'with the results...");
        String outputCaseFormat = commandLine.getOptionValue(OUTPUT_CASE_FORMAT);
        Properties outputParams = readProperties(commandLine, ConversionToolUtils.OptionType.EXPORT, context);
        network.write(outputCaseFormat, outputParams, outputCaseFile);
        context.getOutputStream().println("Done");
    }
}
