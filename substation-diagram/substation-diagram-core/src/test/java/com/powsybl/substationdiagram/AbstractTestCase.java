/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram;

import com.google.common.io.ByteStreams;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.substationdiagram.layout.LayoutParameters;
import com.powsybl.substationdiagram.layout.PositionVoltageLevelLayoutFactory;
import com.powsybl.substationdiagram.layout.SubstationLayoutFactory;
import com.powsybl.substationdiagram.library.ResourcesComponentLibrary;
import com.powsybl.substationdiagram.model.Graph;
import com.powsybl.substationdiagram.model.SubstationGraph;
import com.powsybl.substationdiagram.svg.DefaultSubstationDiagramStyleProvider;
import com.powsybl.substationdiagram.svg.SVGWriter;
import com.powsybl.substationdiagram.svg.SubstationDiagramStyleProvider;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public abstract class AbstractTestCase {

    protected VoltageLevel vl;
    protected Substation substation;

    protected final ResourcesComponentLibrary componentLibrary = new ResourcesComponentLibrary("/ConvergenceLibrary");

    protected final SubstationDiagramStyleProvider styleProvider = new DefaultSubstationDiagramStyleProvider();

    private static String normalizeLineSeparator(String str) {
        return str.replace("\r\n", "\n")
                .replace("\r", "\n");
    }

    abstract void setUp();

    AbstractTestCase() {
        setUp();
    }

    String getName() {
        return getClass().getSimpleName();
    }

    VoltageLevel getVl() {
        return vl;
    }

    Substation getSubstation() {
        return substation;
    }

    public void compareSvg(Graph graph, LayoutParameters layoutParameters, String refSvgName) {
        try (StringWriter writer = new StringWriter()) {
            new SVGWriter(componentLibrary, layoutParameters)
                    .write(graph, styleProvider, writer);
            writer.flush();

//            FileWriter fw = new FileWriter(System.getProperty("user.home") + refSvgName);
//            fw.write(writer.toString());
//            fw.close();

            String refSvg = normalizeLineSeparator(new String(ByteStreams.toByteArray(getClass().getResourceAsStream(refSvgName)), StandardCharsets.UTF_8));
            String svg = normalizeLineSeparator(writer.toString());
            assertEquals(refSvg, svg);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void compareSvg(SubstationGraph graph, LayoutParameters layoutParameters,
                           String refSvgName, SubstationLayoutFactory sLayoutFactory) {
        compareSvg(graph, layoutParameters, refSvgName, sLayoutFactory, styleProvider);
    }

    public void compareSvg(SubstationGraph graph, LayoutParameters layoutParameters,
                           String refSvgName, SubstationLayoutFactory sLayoutFactory,
                           SubstationDiagramStyleProvider styleProvider) {
        try (StringWriter writer = new StringWriter()) {
            new SVGWriter(componentLibrary, layoutParameters)
                    .write(graph, styleProvider, writer, sLayoutFactory,
                            new PositionVoltageLevelLayoutFactory());
            writer.flush();

//            FileWriter fw = new FileWriter(System.getProperty("user.home") + refSvgName);
//            fw.write(writer.toString());
//            fw.close();

            String refSvg = normalizeLineSeparator(new String(ByteStreams.toByteArray(getClass().getResourceAsStream(refSvgName)), StandardCharsets.UTF_8));
            String svg = normalizeLineSeparator(writer.toString());
            assertEquals(refSvg, svg);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
