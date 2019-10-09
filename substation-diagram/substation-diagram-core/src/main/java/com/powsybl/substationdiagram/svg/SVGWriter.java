/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.svg;

import com.powsybl.substationdiagram.layout.LayoutParameters;
import com.powsybl.substationdiagram.library.ComponentLibrary;
import com.powsybl.substationdiagram.model.Graph;
import com.powsybl.substationdiagram.model.SubstationGraph;

import java.io.Writer;
import java.nio.file.Path;

/**
 * @author Gilles Brada <gilles.brada at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public interface SVGWriter {

    GraphMetadata write(Graph graph,
                        SubstationDiagramInitialValueProvider initProvider,
                        SubstationDiagramStyleProvider styleProvider,
                        NodeLabelConfiguration nodeLabelConfiguration,
                        Path svgFile);

    GraphMetadata write(Graph graph,
                        SubstationDiagramInitialValueProvider initProvider,
                        SubstationDiagramStyleProvider styleProvider,
                        NodeLabelConfiguration nodeLabelConfiguration,
                        Writer writer);

    GraphMetadata write(SubstationGraph graph,
                        SubstationDiagramInitialValueProvider initProvider,
                        SubstationDiagramStyleProvider styleProvider,
                        NodeLabelConfiguration nodeLabelConfiguration,
                        Path svgFile);

    GraphMetadata write(SubstationGraph graph,
                        SubstationDiagramInitialValueProvider initProvider,
                        SubstationDiagramStyleProvider styleProvider,
                        NodeLabelConfiguration nodeLabelConfiguration,
                        Writer writer);

    LayoutParameters getLayoutParameters();

    ComponentLibrary getComponentLibrary();
}
