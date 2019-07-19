/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Test;

import com.google.common.io.ByteStreams;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.NetworkFactory;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.substationdiagram.SubstationDiagram;
import com.powsybl.substationdiagram.layout.LayoutParameters;
import com.powsybl.substationdiagram.library.ComponentLibrary;
import com.powsybl.substationdiagram.library.ResourcesComponentLibrary;
import com.powsybl.substationdiagram.svg.DefaultSubstationDiagramInitialValueProvider;
import com.rte_france.powsybl.iidm.network.extensions.cvg.BusbarSectionPosition;
import com.rte_france.powsybl.iidm.network.extensions.cvg.ConnectablePosition;

/**
 * @author Giovanni Ferrari <giovanni.ferrari at techrain.eu>
 */
public class NominalVoltageStyleTest {

    private Network network;
    private Substation substation;
    private VoltageLevel vl;
    private FileSystem fileSystem;
    private Path tmpDir;

    @Before
    public void setUp() {
        network = NetworkFactory.create("testCase1", "test");
        substation = network.newSubstation().setId("s").setCountry(Country.FR).add();
        vl = substation.newVoltageLevel().setId("vl").setTopologyKind(TopologyKind.NODE_BREAKER).setNominalV(400).add();
        VoltageLevel.NodeBreakerView view = vl.getNodeBreakerView().setNodeCount(10);
        BusbarSection bbs = view.newBusbarSection().setId("bbs").setNode(0).add();
        bbs.addExtension(BusbarSectionPosition.class, new BusbarSectionPosition(bbs, 1, 1));
        Load l = vl.newLoad().setId("l").setNode(2).setP0(10).setQ0(10).add();
        l.addExtension(ConnectablePosition.class, new ConnectablePosition<>(l,
                new ConnectablePosition.Feeder("l", 0, ConnectablePosition.Direction.TOP), null, null, null));
        view.newDisconnector().setId("d").setNode1(0).setNode2(1).add();
        view.newBreaker().setId("b").setNode1(1).setNode2(2).add();

        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        try {
            tmpDir = Files.createDirectory(fileSystem.getPath("/tmp"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test() throws IOException {
        ComponentLibrary componentLibrary = new ResourcesComponentLibrary("/ConvergenceLibrary");
        LayoutParameters layoutParameters = new LayoutParameters();

        Path outSvg = tmpDir.resolve("sub.svg");
        Path meta = tmpDir.resolve("meta.json");

        SubstationDiagram.build(substation).writeSvg(componentLibrary, layoutParameters, new DefaultSubstationDiagramInitialValueProvider(network), new NominalVoltageSubstationDiagramStyleProvider(), Files.newBufferedWriter(outSvg, StandardCharsets.UTF_8), Files.newBufferedWriter(meta));

        String svgStr = normalizeLineSeparator(new String(Files.readAllBytes(outSvg), StandardCharsets.UTF_8));

        String refSvg = normalizeLineSeparator(
                new String(ByteStreams.toByteArray(getClass().getResourceAsStream("/nominalVoltage.svg")),
                        StandardCharsets.UTF_8));
        assertEquals(refSvg, svgStr);
    }

    protected static String normalizeLineSeparator(String str) {
        return str.replace("\r\n", "\n")
                .replace("\r", "\n");
    }
}
