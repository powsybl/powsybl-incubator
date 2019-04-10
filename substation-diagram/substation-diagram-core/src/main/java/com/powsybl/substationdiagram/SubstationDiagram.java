/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.substationdiagram.layout.HorizontalSubstationLayoutFactory;
import com.powsybl.substationdiagram.layout.LayoutParameters;
import com.powsybl.substationdiagram.layout.PositionVoltageLevelLayoutFactory;
import com.powsybl.substationdiagram.layout.SubstationLayoutFactory;
import com.powsybl.substationdiagram.layout.VoltageLevelLayoutFactory;
import com.powsybl.substationdiagram.library.ComponentLibrary;
import com.powsybl.substationdiagram.model.SubstationGraph;
import com.powsybl.substationdiagram.svg.DefaultSubstationDiagramInitialValueProvider;
import com.powsybl.substationdiagram.svg.DefaultSubstationDiagramStyleProvider;
import com.powsybl.substationdiagram.svg.GraphMetadata;
import com.powsybl.substationdiagram.svg.SVGWriter;
import com.powsybl.substationdiagram.svg.SubstationDiagramInitialValueProvider;
import com.powsybl.substationdiagram.svg.SubstationDiagramStyleProvider;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public final class SubstationDiagram {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubstationDiagram.class);

    private final SubstationGraph graph;

    private final SubstationLayoutFactory sLayoutFactory;

    private final VoltageLevelLayoutFactory vLayoutFactory;

    private SubstationDiagram(SubstationGraph graph, SubstationLayoutFactory sLayoutFactory,
                              VoltageLevelLayoutFactory vLayoutFactory) {
        this.graph = Objects.requireNonNull(graph);
        this.sLayoutFactory = Objects.requireNonNull(sLayoutFactory);
        this.vLayoutFactory = Objects.requireNonNull(vLayoutFactory);
    }

    public static SubstationDiagram build(Substation s) {
        return build(s, new HorizontalSubstationLayoutFactory(), new PositionVoltageLevelLayoutFactory(), false);
    }

    public static SubstationDiagram build(Substation s, SubstationLayoutFactory sLayoutFactory,
                                          VoltageLevelLayoutFactory vLayoutFactory, boolean useName) {
        Objects.requireNonNull(s);
        Objects.requireNonNull(sLayoutFactory);
        Objects.requireNonNull(vLayoutFactory);

        SubstationGraph graph = SubstationGraph.create(s, useName);

        return new SubstationDiagram(graph, sLayoutFactory, vLayoutFactory);
    }

    public void writeSvg(ComponentLibrary componentLibrary, LayoutParameters layoutParameters, Path svgFile, Network network) {
        writeSvg(componentLibrary, layoutParameters, svgFile, network, false);
    }

    public void writeSvg(ComponentLibrary componentLibrary, LayoutParameters layoutParameters,
                         Path svgFile, Network network, boolean debug) {
        Path dir = svgFile.toAbsolutePath().getParent();
        String svgFileName = svgFile.getFileName().toString();
        if (!svgFileName.endsWith(".svg")) {
            svgFileName = svgFileName + ".svg";
        }

        try (Writer svgWriter = Files.newBufferedWriter(svgFile, StandardCharsets.UTF_8);
             Writer metadataWriter = Files.newBufferedWriter(dir.resolve(svgFileName.replace(".svg", "_metadata.json")), StandardCharsets.UTF_8);
             Writer graphWriter = debug ? Files.newBufferedWriter(dir.resolve(svgFileName.replace(".svg", "_graph.json")), StandardCharsets.UTF_8) : null) {
            writeSvg(componentLibrary, layoutParameters, svgWriter, metadataWriter, graphWriter, network);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeSvg(ComponentLibrary componentLibrary, LayoutParameters layoutParameters,
                         Writer svgWriter, Writer metadataWriter, Writer graphWriter, Network network) {
        writeSvg(componentLibrary, layoutParameters, new DefaultSubstationDiagramInitialValueProvider(network), new DefaultSubstationDiagramStyleProvider(), svgWriter, metadataWriter, graphWriter);
    }

    public void writeSvg(ComponentLibrary componentLibrary, LayoutParameters layoutParameters, SubstationDiagramInitialValueProvider initProvider, SubstationDiagramStyleProvider styleProvider,
                         Writer svgWriter, Writer metadataWriter, Writer graphWriter) {
        Objects.requireNonNull(componentLibrary);
        Objects.requireNonNull(layoutParameters);
        Objects.requireNonNull(svgWriter);
        Objects.requireNonNull(metadataWriter);

        // write graph debug file
        if (graphWriter != null) {
            graph.whenSerializingUsingJsonAnyGetterThenCorrect(graphWriter);
        }

        // write SVG file
        LOGGER.info("Writing SVG and JSON metadata files...");

        GraphMetadata metadata = new SVGWriter(componentLibrary, layoutParameters)
                .write(graph, initProvider, styleProvider, svgWriter, sLayoutFactory, vLayoutFactory);

        // write metadata file
        metadata.writeJson(metadataWriter);
    }
}
