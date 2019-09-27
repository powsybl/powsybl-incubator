/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.svg;

import java.io.Writer;
import java.nio.file.Path;

import com.powsybl.substationdiagram.layout.LayoutParameters;
import com.powsybl.substationdiagram.model.Graph;
import com.powsybl.substationdiagram.model.SubstationGraph;

/**
 * @author Gilles Brada <gilles.brada at rte-france.com>
 */
public interface SVGWriter {

    GraphMetadata write(Graph graph, SubstationDiagramInitialValueProvider initProvider, SubstationDiagramStyleProvider styleProvider, Path svgFile);

    GraphMetadata write(Graph graph, SubstationDiagramInitialValueProvider initProvider, SubstationDiagramStyleProvider styleProvider, Writer writer);

    GraphMetadata write(SubstationGraph graph, SubstationDiagramInitialValueProvider initProvider, SubstationDiagramStyleProvider styleProvider,
            Path svgFile);

    GraphMetadata write(SubstationGraph graph, SubstationDiagramInitialValueProvider initProvider, SubstationDiagramStyleProvider styleProvider,
            Writer writer);

    LayoutParameters getLayoutParameters();
}
