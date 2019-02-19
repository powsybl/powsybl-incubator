/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram;

import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.substationdiagram.layout.LayoutParameters;
import com.powsybl.substationdiagram.layout.PositionVoltageLevelLayoutFactory;
import com.powsybl.substationdiagram.layout.VoltageLevelLayout;
import com.powsybl.substationdiagram.layout.VoltageLevelLayoutFactory;
import com.powsybl.substationdiagram.library.ComponentLibrary;
import com.powsybl.substationdiagram.model.Graph;
import com.powsybl.substationdiagram.svg.GraphMetadata;
import com.powsybl.substationdiagram.svg.SVGWriter;
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
public final class SubstationDiagram {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubstationDiagram.class);

    private final Graph graph;

    private final VoltageLevelLayout layout;

    private SubstationDiagram(Graph graph, VoltageLevelLayout layout) {
        this.graph = Objects.requireNonNull(graph);
        this.layout = Objects.requireNonNull(layout);
    }

    public static SubstationDiagram build(VoltageLevel vl) {
        return build(vl, new PositionVoltageLevelLayoutFactory());
    }

    public static SubstationDiagram build(VoltageLevel vl, VoltageLevelLayoutFactory layoutFactory) {
        Objects.requireNonNull(vl);
        Objects.requireNonNull(layoutFactory);

        Graph graph = Graph.create(vl);
        VoltageLevelLayout layout = layoutFactory.create(graph);

        return new SubstationDiagram(graph, layout);
    }

    public void writeSvg(ComponentLibrary componentLibrary, LayoutParameters layoutParameters, Path svgFile) {
        writeSvg(componentLibrary, layoutParameters, svgFile, false);
    }

    public void writeSvg(ComponentLibrary componentLibrary, LayoutParameters layoutParameters, Path svgFile,
                         boolean debug) {
        Path dir = svgFile.toAbsolutePath().getParent();
        String svgFileName = svgFile.getFileName().toString();
        if (!svgFileName.endsWith(".svg")) {
            svgFileName = svgFileName + ".svg";
        }

        try (Writer svgWriter = Files.newBufferedWriter(svgFile, StandardCharsets.UTF_8);
             Writer metadataWriter = Files.newBufferedWriter(dir.resolve(svgFileName.replace(".svg", "_metadata.json")), StandardCharsets.UTF_8);
             Writer graphWriter = debug ? Files.newBufferedWriter(dir.resolve(svgFileName.replace(".svg", "_graph.json")), StandardCharsets.UTF_8) : null) {
            writeSvg(componentLibrary, layoutParameters, svgWriter, metadataWriter, graphWriter);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeSvg(ComponentLibrary componentLibrary, LayoutParameters layoutParameters, Writer svgWriter,
                         Writer metadataWriter, Writer graphWriter) {
        Objects.requireNonNull(componentLibrary);
        Objects.requireNonNull(layoutParameters);
        Objects.requireNonNull(svgWriter);
        Objects.requireNonNull(metadataWriter);

        // calculate coordinate
        layout.run(layoutParameters);

        // write graph debug file
        if (graphWriter != null) {
            graph.whenSerializingUsingJsonAnyGetterThenCorrect(graphWriter);
        }

        // write SVG file
        LOGGER.info("Writing SVG and JSON metadata files...");

        GraphMetadata metadata = new SVGWriter(componentLibrary, layoutParameters)
                .write(graph, svgWriter);

        // write metadata file
        metadata.writeJson(metadataWriter);
    }
}
