/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.opf;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.powsybl.ampl.converter.AbstractNetworkApplierFactory;
import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.commons.util.StringToIntMapper;
import org.apache.commons.lang3.tuple.Pair;

import com.powsybl.ampl.converter.NetworkApplier;
import com.powsybl.ampl.converter.OutputFileFormat;
import com.powsybl.ampl.executor.IAmplModel;

import static com.powsybl.ampl.converter.AmplConstants.DEFAULT_VARIANT_INDEX;

/**
 * Enumeration to link resources ampl models to java code
 * It allows to get the list of InputStream of the ampl model resources.
 */
public enum AmplModel implements IAmplModel {
    /**
     * a reactive opf to set target tension of the generators
     */
    REACTIVE_OPF("reactiveopf_results", "reactiveopf_src",
            Arrays.asList("reactiveopf.run", "reactiveopfoutput.run", "reactiveopfexit.run"),
            Arrays.asList("reactiveopf.mod", "reactiveopf.dat"));

    private static final String NETWORK_DATA_PREFIX = "ampl";
    /**
     * A list containing the name of the files and their path in the resources
     */
    private List<Pair<String, String>> modelNameAndPath;
    private List<String> runFiles;
    private String outputFilePrefix;

    /**
     * Create a ampl Model to be executed
     *
     * @param outputFilePrefix The prefix used for the output files, they must be
     *                         compatible with AmplNetworkReader
     * @param resourcesFolder  The resources folder name containing all the files
     * @param runFiles         The names of the file that must be run in AMPL (.run
     *                         files). The order of the list gives the order of
     *                         execution.
     * @param resourcesFiles   All others files needed by the model (.dat and .mod
     *                         files)
     */
    private AmplModel(String outputFilePrefix, String resourcesFolder, List<String> runFiles,
                      List<String> resourcesFiles) {
        this.runFiles = runFiles;
        this.outputFilePrefix = outputFilePrefix;
        List<String> modelFiles = new LinkedList<>();
        modelFiles.addAll(resourcesFiles);
        modelFiles.addAll(runFiles);
        this.modelNameAndPath = modelFiles.stream().map(file -> Pair.of(file, resourcesFolder + "/" + file)).collect(
                Collectors.toCollection(LinkedList::new));
    }

    /**
     * @return each pair contains the name, and the InputStream of the file
     */
    public List<Pair<String, InputStream>> getModelAsStream() {
        return modelNameAndPath.stream()
                .map(nameAndPath -> {
                    try {
                        return Pair.of(nameAndPath.getLeft(),
                                Files.newInputStream(Paths.get("reactive-opf/" + nameAndPath.getRight())));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toCollection(LinkedList::new));
    }

    public List<String> getAmplRunFiles() {
        return this.runFiles;
    }

    @Override
    public String getOutputFilePrefix() {
        return this.outputFilePrefix;
    }

    @Override
    public AbstractNetworkApplierFactory getNetworkApplierFactory() {
        return new AbstractNetworkApplierFactory() {
            @Override
            public NetworkApplier of(StringToIntMapper<AmplSubset> mapper) {
                return new ReactiveOpfNetworkApplier(mapper);
            }
        };
    }

    @Override
    public int getVariant() {
        return DEFAULT_VARIANT_INDEX;
    }

    @Override
    public String getNetworkDataPrefix() {
        return NETWORK_DATA_PREFIX;
    }

    @Override
    public OutputFileFormat getOutputFormat() {
        return new OutputFileFormat() {

            @Override
            public String getTokenSeparator() {
                return ";";
            }

            @Override
            public String getFileExtension() {
                return "csv";
            }

            @Override
            public Charset getFileEncoding() {
                return StandardCharsets.UTF_8;
            }

        };
    }
}
