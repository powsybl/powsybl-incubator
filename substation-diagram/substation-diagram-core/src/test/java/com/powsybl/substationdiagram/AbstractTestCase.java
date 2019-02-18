/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram;

import com.google.common.io.ByteStreams;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.substationdiagram.layout.LayoutParameters;
import com.powsybl.substationdiagram.library.ResourcesComponentLibrary;
import com.powsybl.substationdiagram.model.Graph;
import com.powsybl.substationdiagram.svg.SVGWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

/**
 * @author Jeanson Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public abstract class AbstractTestCase {

    protected VoltageLevel vl;

    private final ResourcesComponentLibrary componentLibrary = new ResourcesComponentLibrary("/ConvergenceLibrary");

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


    public void compareSvg(Graph graph, LayoutParameters layoutParameters, String refSvgName) {
        try (StringWriter writer = new StringWriter()) {
            new SVGWriter(componentLibrary, layoutParameters)
                    .write(graph, writer);
            writer.flush();

            String refSvg = normalizeLineSeparator(new String(ByteStreams.toByteArray(getClass().getResourceAsStream(refSvgName)), StandardCharsets.UTF_8));
            String svg = normalizeLineSeparator(writer.toString());
            assertEquals(refSvg, svg);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
