/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.network;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import org.bouncycastle.util.io.Streams;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class NetworkContextTest {

    private FileSystem fileSystem;

    @Before
    public void setUp() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
    }

    @After
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    @Test
    public void test() throws IOException {
        Network network = EurostagTutorialExample1Factory.create();
        network.getVoltageLevel("VLLOAD").newShuntCompensator()
                .setId("SC")
                .setBus("NLOAD")
                .setConnectableBus("NLOAD")
                .setbPerSection(3.25 * Math.pow(10, -3))
                .setMaximumSectionCount(1)
                .setCurrentSectionCount(1)
                .add();

        List<NetworkContext> networkContexts = NetworkContext.of(network, SlackBusSelectionMode.MOST_MESHED);
        assertEquals(1, networkContexts.size());
        Path file = fileSystem.getPath("/work/n.json");
        networkContexts.get(0).writeJson(file);
        String json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        String jsonRef = new String(Streams.readAll(getClass().getResourceAsStream("/n.json")), StandardCharsets.UTF_8);
        assertEquals(jsonRef, json);
    }
}