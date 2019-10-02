/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.substationdiagram.layout.HorizontalSubstationLayoutFactory;
import com.powsybl.substationdiagram.layout.LayoutParameters;
import com.powsybl.substationdiagram.layout.PositionVoltageLevelLayoutFactory;
import com.powsybl.substationdiagram.layout.SubstationLayout;
import com.powsybl.substationdiagram.layout.SubstationLayoutFactory;
import com.powsybl.substationdiagram.layout.VoltageLevelLayoutFactory;
import com.powsybl.substationdiagram.library.ComponentLibrary;
import com.powsybl.substationdiagram.model.SubstationGraph;
import com.powsybl.substationdiagram.svg.DefaultNodeLabelConfiguration;
import com.powsybl.substationdiagram.svg.DefaultSubstationDiagramInitialValueProvider;
import com.powsybl.substationdiagram.svg.DefaultSubstationDiagramStyleProvider;
import com.powsybl.substationdiagram.svg.GraphMetadata;
import com.powsybl.substationdiagram.svg.DefaultSVGWriter;
import com.powsybl.substationdiagram.svg.NodeLabelConfiguration;
import com.powsybl.substationdiagram.svg.SVGWriter;
import com.powsybl.substationdiagram.svg.SubstationDiagramInitialValueProvider;
import com.powsybl.substationdiagram.svg.SubstationDiagramStyleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public final class SubstationDiagram {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubstationDiagram.class);

    private final SubstationGraph subGraph;

    private final SubstationLayout subLayout;

    private SubstationDiagram(SubstationGraph graph, SubstationLayout layout) {
        this.subGraph = Objects.requireNonNull(graph);
        this.subLayout = Objects.requireNonNull(layout);
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

        SubstationLayout layout = sLayoutFactory.create(graph, vLayoutFactory);

        return new SubstationDiagram(graph, layout);
    }

    public void writeSvg(ComponentLibrary componentLibrary, LayoutParameters layoutParameters, Network network, Path svgFile) {
        SVGWriter writer = new DefaultSVGWriter(componentLibrary, layoutParameters);
        writeSvg(writer, svgFile, network);
    }

    public void writeSvg(SVGWriter writer, Network network, Path svgFile) {
        writeSvg(writer, svgFile, network);
    }

    public void writeSvg(SVGWriter writer, Path svgFile, Network network) {
        Path dir = svgFile.toAbsolutePath().getParent();
        String svgFileName = svgFile.getFileName().toString();
        if (!svgFileName.endsWith(".svg")) {
            svgFileName = svgFileName + ".svg";
        }

        try (Writer svgWriter = Files.newBufferedWriter(svgFile, StandardCharsets.UTF_8);
                Writer metadataWriter = Files.newBufferedWriter(dir.resolve(svgFileName.replace(".svg", "_metadata.json")), StandardCharsets.UTF_8)) {
            writeSvg(writer, svgWriter, metadataWriter, network);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeSvg(SVGWriter writer, Writer svgWriter, Writer metadataWriter, Network network) {
        writeSvg(writer,
                new DefaultSubstationDiagramInitialValueProvider(network),
                new DefaultSubstationDiagramStyleProvider(),
                new DefaultNodeLabelConfiguration(writer.getComponentLibrary()),
                svgWriter,
                metadataWriter);
    }

    public void writeSvg(ComponentLibrary componentLibrary, LayoutParameters layoutParameters,
                         SubstationDiagramInitialValueProvider initProvider,
                         SubstationDiagramStyleProvider styleProvider,
                         NodeLabelConfiguration nodeLabelConfiguration,
                         Writer svgWriter, Writer metadataWriter) {
        SVGWriter writer = new DefaultSVGWriter(componentLibrary, layoutParameters);
        writeSvg(writer, initProvider, styleProvider, nodeLabelConfiguration, svgWriter, metadataWriter);
    }

    public void writeSvg(SVGWriter writer,
                         SubstationDiagramInitialValueProvider initProvider,
                         SubstationDiagramStyleProvider styleProvider,
                         NodeLabelConfiguration nodeLabelConfiguration,
                         Writer svgWriter, Writer metadataWriter) {
        Objects.requireNonNull(writer);
        Objects.requireNonNull(writer.getLayoutParameters());
        Objects.requireNonNull(svgWriter);
        Objects.requireNonNull(metadataWriter);

        subLayout.run(writer.getLayoutParameters());

        // write SVG file
        LOGGER.info("Writing SVG and JSON metadata files...");

        GraphMetadata metadata = writer.write(subGraph, initProvider, styleProvider, nodeLabelConfiguration, svgWriter);

        // write metadata file
        metadata.writeJson(metadataWriter);
    }
}
