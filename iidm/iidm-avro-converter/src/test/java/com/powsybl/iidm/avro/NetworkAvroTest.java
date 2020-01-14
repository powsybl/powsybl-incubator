/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.avro;

import com.powsybl.commons.AbstractConverterTest;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.export.Exporters;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.*;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.eu>
 */
public class NetworkAvroTest extends AbstractConverterTest {

    private final ImportConfig importConfigMock = Mockito.mock(ImportConfig.class);
    private final ComputationManager computationManager = Mockito.mock(ComputationManager.class);

    TestsStatistics testsStatistics = new TestsStatistics();

    private Network timeImport(Path path) {
        Properties properties = new Properties();
        long startTime = System.currentTimeMillis();
        Network network = Importers.loadNetwork(path, computationManager, importConfigMock, properties);
        long time2 = System.currentTimeMillis() - startTime;
        testsStatistics.addEntry(path, "IMPORT", time2);
        return network;
    }

    private void timeExport(String format, Network network, Path path) {
        Properties properties = new Properties();
        long startTime = System.currentTimeMillis();
        Exporters.export(format, network, properties, path);
        long time2 = System.currentTimeMillis() - startTime;
        testsStatistics.addEntry(path, "EXPORT", time2);
    }

    private void testNetwork(Network network, Path parentPath) throws IOException {
        Properties properties = new Properties();

        //create a reference xiidm file for the original network
        Path xmlFile = parentPath.resolve("network1.xiidm");
        timeExport("XIIDM", network, xmlFile);

        //import network from xiidm
        Network networkNew = timeImport(xmlFile);

        //export the network to the protobuf
        Path protoFile = parentPath.resolve("network1.aiidm");
        timeExport("AIIDM", networkNew, protoFile);

        //create a new network form the protobuf
        Network network2 = timeImport(protoFile);
        assertNotNull(network2);

        //export the network to another xiidm file
        Path xmlFile2 = parentPath.resolve("network2.xiidm");
        timeExport("XIIDM", network2, xmlFile2);

        //compare the xiidm reference with the latest
        try (InputStream is1 = Files.newInputStream(xmlFile);
             InputStream is2 = Files.newInputStream(xmlFile2);) {
            compareTxt(is1, is2);
        }

        testsStatistics.dumpStatistics();
    }

    private void testNetwork(Network network) throws IOException {
        testNetwork(network, tmpDir);
    }

    @Test
    public void testBusBranch1() throws IOException {
        Network network = EurostagTutorialExample1Factory.create();
        testNetwork(network, tmpDir);
    }

    @Test
    public void testNodeBreaker1() throws IOException {
        Network network = NetworkTest1Factory.create();
        try {
            testNetwork(network);
        } catch (UnsupportedOperationException e) {
            //current expected behaviour
        }
    }

    @Test
    public void testSvc() throws IOException {
        Network network = SvcTestCaseFactory.create();
        testNetwork(network, tmpDir);
    }

    @Test
    public void testReactiveLimits() throws IOException {
        Network network = ReactiveLimitsTestNetworkFactory.create();
        testNetwork(network, tmpDir);
    }

    @Test
    public void testPhaseShifter() throws IOException {
        Network network = PhaseShifterTestCaseFactory.create();
        testNetwork(network, tmpDir);
    }

    @Test
    public void testNoEquipments() throws IOException {
        Network network = NoEquipmentNetworkFactory.create();
        testNetwork(network, tmpDir);
    }

    @Test
    public void testDanglingLine() throws IOException {
        Network network = DanglingLineNetworkFactory.create();
        testNetwork(network, tmpDir);
    }

    @Test
    public void testBattery() throws IOException {
        Network network = BatteryNetworkFactory.create();
        testNetwork(network, tmpDir);
    }

    @Test
    public void testThreeWindingsTransformer() throws IOException {
        Network network = ThreeWindingsTransformerNetworkFactory.create();
        testNetwork(network, tmpDir);
    }

}
