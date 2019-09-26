/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.substationdiagram.layout.LayoutParameters;
import com.powsybl.substationdiagram.layout.VoltageLevelLayout;
import com.powsybl.substationdiagram.layout.VoltageLevelLayoutFactory;
import com.powsybl.substationdiagram.library.ComponentLibrary;
import com.powsybl.substationdiagram.model.Graph;
import com.powsybl.substationdiagram.svg.DefaultSubstationDiagramInitialValueProvider;
import com.powsybl.substationdiagram.svg.DefaultSubstationDiagramStyleProvider;
import com.powsybl.substationdiagram.svg.GraphMetadata;
import com.powsybl.substationdiagram.svg.SVGWriter;
import com.powsybl.substationdiagram.svg.SVGWriterInterface;
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
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public final class VoltageLevelDiagram {

    private static final Logger LOGGER = LoggerFactory.getLogger(VoltageLevelDiagram.class);

    private final Graph graph;

    private final VoltageLevelLayout vlLayout;

    private VoltageLevelDiagram(Graph graph, VoltageLevelLayout layout) {
        this.graph = Objects.requireNonNull(graph);
        this.vlLayout = Objects.requireNonNull(layout);
    }

    public static VoltageLevelDiagram build(VoltageLevel vl, VoltageLevelLayoutFactory layoutFactory,
                                            boolean useName, boolean showInductorFor3WT) {
        Objects.requireNonNull(vl);
        Objects.requireNonNull(layoutFactory);

        Graph graph = Graph.create(vl, useName, true, showInductorFor3WT);

        VoltageLevelLayout layout = layoutFactory.create(graph);

        return new VoltageLevelDiagram(graph, layout);
    }

    public void writeSvg(ComponentLibrary componentLibrary, LayoutParameters layoutParameters, Network network, Path svgFile) {
        SVGWriterInterface writer = new SVGWriter(componentLibrary, layoutParameters);
        writeSvg(writer, new DefaultSubstationDiagramInitialValueProvider(network), new DefaultSubstationDiagramStyleProvider(), svgFile, false);
    }

    public void writeSvg(SVGWriterInterface writer, Network network, Path svgFile) {
        writeSvg(writer, new DefaultSubstationDiagramInitialValueProvider(network), new DefaultSubstationDiagramStyleProvider(), svgFile, false);
    }

    public void writeSvg(SVGWriterInterface writer, SubstationDiagramInitialValueProvider initProvider, SubstationDiagramStyleProvider styleProvider, Path svgFile,
                         boolean debug) {
        Path dir = svgFile.toAbsolutePath().getParent();
        String svgFileName = svgFile.getFileName().toString();
        if (!svgFileName.endsWith(".svg")) {
            svgFileName = svgFileName + ".svg";
        }
        try (Writer svgWriter = Files.newBufferedWriter(svgFile, StandardCharsets.UTF_8);
                Writer metadataWriter = Files.newBufferedWriter(dir.resolve(svgFileName.replace(".svg", "_metadata.json")), StandardCharsets.UTF_8)) {
            writeSvg(writer, initProvider, styleProvider, svgWriter, metadataWriter);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeSvg(ComponentLibrary componentLibrary, LayoutParameters layoutParameters,
                         SubstationDiagramInitialValueProvider initProvider, SubstationDiagramStyleProvider styleProvider,
                         Writer svgWriter,
                         Writer metadataWriter) {
        SVGWriterInterface writer = new SVGWriter(componentLibrary, layoutParameters);
        writeSvg(writer, initProvider, styleProvider, svgWriter, metadataWriter);
    }

    public void writeSvg(SVGWriterInterface writer, SubstationDiagramInitialValueProvider initProvider, SubstationDiagramStyleProvider styleProvider,
            Writer svgWriter,
            Writer metadataWriter) {
        Objects.requireNonNull(writer);
        Objects.requireNonNull(writer.getLayoutParameters());
        Objects.requireNonNull(svgWriter);
        Objects.requireNonNull(metadataWriter);

        // calculate coordinate
        vlLayout.run(writer.getLayoutParameters());

        // write SVG file
        LOGGER.info("Writing SVG and JSON metadata files...");

        GraphMetadata metadata = writer.write(graph, initProvider, styleProvider, svgWriter);

        // write metadata file
        metadata.writeJson(metadataWriter);
    }
}
