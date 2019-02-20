/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.view;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.substationdiagram.SubstationDiagram;
import com.powsybl.substationdiagram.layout.LayoutParameters;
import com.powsybl.substationdiagram.layout.PositionVoltageLevelLayoutFactory;
import com.powsybl.substationdiagram.layout.VoltageLevelLayoutFactory;
import com.powsybl.substationdiagram.library.ComponentLibrary;
import com.powsybl.substationdiagram.library.ResourcesComponentLibrary;

import javafx.scene.layout.Pane;

/**
 * @author Giovanni Ferrari <giovanni.ferrari at techrain.eu>
 */
public class SvgVoltageLevelView implements VoltageLevelView {

    private VoltageLevelLayoutFactory layoutFactory = new PositionVoltageLevelLayoutFactory();

    @Override
    public Pane view(VoltageLevel voltageLevel, NavigationListener navigationListener, StyleHandler styleHandler) {
        Objects.requireNonNull(voltageLevel);

        ComponentLibrary componentLibrary = new ResourcesComponentLibrary("/biblioSVG");

        LayoutParameters layoutParameters = new LayoutParameters()
                .setShowGrid(true)
                .setShowInternalNodes(true);

        Path svgFile = Paths.get(voltageLevel.getId() + ".svg");

        String svgData;
        String metadataData;
        try (StringWriter svgWriter = new StringWriter();
             StringWriter metadataWriter = new StringWriter()) {
            SubstationDiagram diagram = SubstationDiagram.build(voltageLevel, layoutFactory);
            diagram.writeSvg(componentLibrary, layoutParameters, svgWriter, metadataWriter, null);
            svgWriter.flush();
            metadataWriter.flush();
            svgData = svgWriter.toString();
            metadataData = metadataWriter.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        SubstationDiagramView diagramView;
        try (InputStream svgInputStream = new ByteArrayInputStream(svgData.getBytes(StandardCharsets.UTF_8));
             InputStream metadataInputStream = new ByteArrayInputStream(metadataData.getBytes(StandardCharsets.UTF_8))) {
            diagramView = SubstationDiagramView.load(svgInputStream, metadataInputStream, navigationListener, styleHandler);
            return diagramView;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
