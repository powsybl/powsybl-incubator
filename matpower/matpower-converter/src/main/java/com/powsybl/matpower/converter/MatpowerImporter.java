/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.matpower.converter;

import com.google.auto.service.AutoService;
import com.google.common.io.ByteStreams;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.iidm.import_.Importer;
import com.powsybl.iidm.network.*;
import com.powsybl.matpower.model.*;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.Pseudograph;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.eu>
 */
@AutoService(Importer.class)
public class MatpowerImporter implements Importer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MatpowerImporter.class);

    private static final String FORMAT = "MATPOWER";

    private static final String EXT = "m";

    public static final LocalDate DEFAULTDATE = LocalDate.of(2020, Month.JANUARY, 1);

    private static ContainersMapping createContainerMapping(MatpowerModel matpowerModel) {
        ContainersMapping containersMapping = new ContainersMapping();

        // group buses connected to non impedant lines to voltage levels
        createVoltageLevelMapping(matpowerModel, containersMapping);

        // group voltage levels connected by transformers to substations
        createSubstationMapping(matpowerModel, containersMapping);

        return containersMapping;
    }

    private static boolean isTransformer(MBranch branch) {
        return branch.getRatio() != 0;
    }

    private static void createSubstationMapping(MatpowerModel model, ContainersMapping containersMapping) {
        UndirectedGraph<String, Object> sGraph = new Pseudograph<>(Object.class);
        for (String voltageLevelId : containersMapping.voltageLevelIdToBusNums.keySet()) {
            sGraph.addVertex(voltageLevelId);
        }
        for (MBranch branch : model.getBranches()) {
            if (isTransformer(branch)) {
                sGraph.addEdge(containersMapping.busNumToVoltageLevelId.get(branch.getFrom()),
                        containersMapping.busNumToVoltageLevelId.get(branch.getTo()));
            }
        }
        int substationNum = 1;
        for (Set<String> voltageLevelIds : new ConnectivityInspector<>(sGraph).connectedSets()) {
            String substationId = "S" + substationNum++;
            for (String voltageLevelId : voltageLevelIds) {
                containersMapping.voltageLevelIdToSubstationId.put(voltageLevelId, substationId);
            }
        }
    }

    private static void createVoltageLevelMapping(MatpowerModel model, ContainersMapping containersMapping) {
        UndirectedGraph<Integer, Object> vlGraph = new Pseudograph<>(Object.class);
        for (MBus mBus : model.getBuses()) {
            vlGraph.addVertex(mBus.getNumber());
        }
        for (MBranch mBranch : model.getBranches()) {
            if (mBranch.getR() == 0 && mBranch.getX() == 0) {
                vlGraph.addEdge(mBranch.getFrom(), mBranch.getTo());
            }
        }
        for (Set<Integer> busNums : new ConnectivityInspector<>(vlGraph).connectedSets()) {
            String voltageLevelId = "VL" + busNums.iterator().next();
            containersMapping.voltageLevelIdToBusNums.put(voltageLevelId, busNums);
            for (int busNum : busNums) {
                containersMapping.busNumToVoltageLevelId.put(busNum, voltageLevelId);
            }
        }
    }

    private static String getBusId(int busNum) {
        return "B" + busNum;
    }

    private static void createBuses(MatpowerModel model, ContainersMapping containerMapping, Network network) {
        for (MBus mBus : model.getBuses()) {
            String voltageLevelId = containerMapping.busNumToVoltageLevelId.get(mBus.getNumber());
            String substationId = containerMapping.voltageLevelIdToSubstationId.get(voltageLevelId);

            // create substation
            Substation substation = createSubstation(network, substationId);

            // create voltage level
            VoltageLevel voltageLevel = createVoltageLevel(mBus, voltageLevelId, substation, network);

            // create bus
            createBus(mBus, voltageLevel);

            // create load
            createLoad(mBus, voltageLevel);

            // create shunt compensator
            createShuntCompensator(mBus, voltageLevel);

            //create generators
            createGenerators(model, mBus, voltageLevel);
        }
    }

    private static void createGenerators(MatpowerModel model, MBus mBus, VoltageLevel voltageLevel) {
        model.getGenerators().stream().filter(gen -> gen.getNumber() == mBus.getNumber()).forEach(mGen -> {
            String busId = getBusId(mGen.getNumber());
            String genId = createGenId(mGen, voltageLevel.getNetwork());
            Generator generator = voltageLevel.newGenerator()
                    .setId(genId)
                    .setConnectableBus(busId)
                    .setBus(busId)
                    .setTargetV(mGen.getVoltageMagnitudeSetpoint())
                    .setTargetP(mGen.getRealPowerOutput())
                    .setTargetQ(mGen.getReactivePowerOutput())
                    .setVoltageRegulatorOn(true)
                    .setMaxP(mGen.getMaximumRealPowerOutput())
                    .setMinP(mGen.getMinimumRealPowerOutput())
                    .add();

            generator.newMinMaxReactiveLimits()
                    .setMinQ(mGen.getMinimumReactivePowerOutput())
                    .setMaxQ(mGen.getMaximumReactivePowerOutput())
                    .add();

            if ((mGen.getPc1() != 0) || (mGen.getPc2() != 0)) {
                generator.newReactiveCapabilityCurve()
                        .beginPoint()
                        .setP(mGen.getPc1())
                        .setMaxQ(mGen.getQc1Max())
                        .setMinQ(mGen.getQc1Min())
                        .endPoint()
                        .beginPoint()
                        .setP(mGen.getPc2())
                        .setMaxQ(mGen.getQc2Max())
                        .setMinQ(mGen.getQc2Min())
                        .endPoint()
                        .add();
            }
        });
    }

    private static Bus createBus(MBus mBus, VoltageLevel voltageLevel) {
        String busId = getBusId(mBus.getNumber());
        LOGGER.debug("Creating bus {}", busId);
        Bus bus = voltageLevel.getBusBreakerView().newBus()
                .setId(busId)
                .setName(busId)
                .add();
        bus.setV(mBus.getVoltageMagnitude() * voltageLevel.getNominalV())
                .setAngle(mBus.getVoltageAngle());
        return bus;
    }

    private static Substation createSubstation(Network network, String substationId) {
        LOGGER.debug("Creating substation {}", substationId);
        Substation substation = network.getSubstation(substationId);
        if (substation == null) {
            substation = network.newSubstation()
                    .setId(substationId)
                    .add();
        }
        return substation;
    }

    private static VoltageLevel createVoltageLevel(MBus mBus, String voltageLevelId, Substation substation, Network network) {
        double nominalV = mBus.getBaseVoltage() == 0 ? 1 : mBus.getBaseVoltage();
        VoltageLevel voltageLevel = network.getVoltageLevel(voltageLevelId);
        if (voltageLevel == null) {
            LOGGER.debug("Creating voltagelevel {}", voltageLevelId);
            voltageLevel = substation.newVoltageLevel()
                    .setId(voltageLevelId)
                    .setNominalV(nominalV)
                    .setTopologyKind(TopologyKind.BUS_BREAKER)
                    .add();
        }
        return voltageLevel;
    }

    private static void createLoad(MBus mBus, VoltageLevel voltageLevel) {
        if (mBus.getRealPowerDemand() != 0 || mBus.getReactivePowerDemand() != 0) {
            String busId = getBusId(mBus.getNumber());
            String loadId = "L" + busId;
            LOGGER.debug("Creating load {}", loadId);
            voltageLevel.newLoad()
                .setId(loadId)
                .setConnectableBus(busId)
                .setBus(busId)
                .setP0(mBus.getRealPowerDemand())
                .setQ0(mBus.getReactivePowerDemand())
                .add();
        }
    }

    private static String createGenId(MGen mGen, Network network) {
        String genIdPrefix = "G" + mGen.getNumber() + "-";
        int uniqueGenSuffix = 0;
        String genId;
        do {
            genId = genIdPrefix + uniqueGenSuffix++;
        } while  (network.getIdentifiable(genId) != null);
        return genId;
    }

    private static void createShuntCompensator(MBus mBus, VoltageLevel voltageLevel) {
        if (mBus.getShuntSusceptance() != 0) {
            String busId = getBusId(mBus.getNumber());
            String shuntId = busId + "SH" + busId;
            LOGGER.debug("Creating shunt {}", shuntId);
            voltageLevel.newShuntCompensator()
                    .setId(shuntId)
                    .setConnectableBus(busId)
                    .setBus(busId)
                    .setbPerSection(mBus.getShuntSusceptance())
                    .setCurrentSectionCount(1)
                    .setMaximumSectionCount(1)
                    .add();
        }
    }

    private static String getBranchId(char type, int from, int to, Network network) {
        String id;
        int uniqueCircuit = 0;
        do {
            id = "" + type + from + "-" + to + "-" + uniqueCircuit++;
        } while (network.getIdentifiable(id) != null);
        return id;
    }

    private static void createLine(MBranch branch, ContainersMapping containerMapping, Network network) {
        String lineId = getBranchId('L', branch.getFrom(), branch.getTo(), network);
        LOGGER.debug("Creating line {}", lineId);
        String bus1Id = getBusId(branch.getFrom());
        String bus2Id = getBusId(branch.getTo());
        String voltageLevel1Id = containerMapping.busNumToVoltageLevelId.get(branch.getFrom());
        String voltageLevel2Id = containerMapping.busNumToVoltageLevelId.get(branch.getTo());
        VoltageLevel voltageLevel2 = network.getVoltageLevel(voltageLevel2Id);
        double zb = Math.pow(voltageLevel2.getNominalV(), 2);
        network.newLine()
                .setId(lineId)
                .setBus1(bus1Id)
                .setConnectableBus1(bus1Id)
                .setVoltageLevel1(voltageLevel1Id)
                .setBus2(bus2Id)
                .setConnectableBus2(bus2Id)
                .setVoltageLevel2(voltageLevel2Id)
                .setR(branch.getR() * zb)
                .setX(branch.getX() * zb)
                .setG1(0)
                .setB1(branch.getB() / zb / 2)
                .setG2(0)
                .setB2(branch.getB() / zb / 2)
                .add();
    }

    private static TwoWindingsTransformer createTransformer(MBranch mBranch, ContainersMapping containerMapping, Network network) {
        String id = getBranchId('T', mBranch.getFrom(), mBranch.getTo(), network);

        String bus1Id = getBusId(mBranch.getFrom());
        String bus2Id = getBusId(mBranch.getTo());

        LOGGER.debug("Creating two winding transformer {} {} {}", id, bus1Id, bus2Id);

        // taps at from bus
        String voltageLevel1Id = containerMapping.busNumToVoltageLevelId.get(mBranch.getFrom());
        String voltageLevel2Id = containerMapping.busNumToVoltageLevelId.get(mBranch.getTo());
        VoltageLevel voltageLevel1 = network.getVoltageLevel(voltageLevel1Id);
        VoltageLevel voltageLevel2 = network.getVoltageLevel(voltageLevel2Id);
        double zb = Math.pow(voltageLevel2.getNominalV(), 2);
        return voltageLevel2.getSubstation().newTwoWindingsTransformer()
                .setId(id)
                .setBus1(bus1Id)
                .setConnectableBus1(bus1Id)
                .setVoltageLevel1(voltageLevel1Id)
                .setBus2(bus2Id)
                .setConnectableBus2(bus2Id)
                .setVoltageLevel2(voltageLevel2Id)
                .setRatedU1(voltageLevel1.getNominalV() * mBranch.getRatio())
                .setRatedU2(voltageLevel2.getNominalV())
                .setR(mBranch.getR() * zb)
                .setX(mBranch.getX() * zb)
                .setG(0)
                .setB(mBranch.getB() / zb)
            .add();
    }

    private static void createBranches(MatpowerModel model, ContainersMapping containerMapping, Network network) {
        for (MBranch mBranch : model.getBranches()) {
            if (mBranch.getRatio() == 0) {
                createLine(mBranch, containerMapping, network);
            } else {
                createTransformer(mBranch, containerMapping, network);
            }
        }
    }

    @Override
    public String getFormat() {
        return FORMAT;
    }

    @Override
    public String getComment() {
        return "MATPOWER Format to IIDM converter";
    }

    @Override
    public boolean exists(ReadOnlyDataSource dataSource) {
        try {
            return dataSource.exists(null, EXT);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void copy(ReadOnlyDataSource fromDataSource, DataSource toDataSource) {
        Objects.requireNonNull(fromDataSource);
        Objects.requireNonNull(toDataSource);
        try {
            try (InputStream is = fromDataSource.newInputStream(null, EXT);
                 OutputStream os = toDataSource.newOutputStream(null, EXT, false)) {
                ByteStreams.copy(is, os);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Network importData(ReadOnlyDataSource dataSource, NetworkFactory networkFactory, Properties parameters) {
        Objects.requireNonNull(dataSource);
        Objects.requireNonNull(networkFactory);
        Network network = networkFactory.createNetwork(dataSource.getBaseName(), FORMAT);

        // no info abount time & date from the matpower file, set a  default
        ZonedDateTime caseDateTime = DEFAULTDATE.atStartOfDay(ZoneOffset.UTC.normalized());
        network.setCaseDate(new DateTime(caseDateTime.toInstant().toEpochMilli(), DateTimeZone.UTC));

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(dataSource.newInputStream(null, EXT)))) {
            // parse file
            MatpowerModel model = new MatpowerReader().read(reader);
            LOGGER.debug("MATPOWER model {}", model);

            ContainersMapping containerMapping = createContainerMapping(model);

            createBuses(model, containerMapping, network);

            createBranches(model, containerMapping, network);

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return network;
    }

    private static class ContainersMapping {

        private final Map<Integer, String> busNumToVoltageLevelId = new HashMap<>();

        private final Map<String, Set<Integer>> voltageLevelIdToBusNums = new HashMap<>();

        private final Map<String, String> voltageLevelIdToSubstationId = new HashMap<>();
    }
}
