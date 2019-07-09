/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.network;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.base.Stopwatch;
import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class NetworkContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkContext.class);

    private final Network network;

    private final List<LfBus> buses;

    private final List<LfBranch> branches;

    private final LfBus slackBus;

    private static class CreationContext {

        private final Set<Branch> branchSet = new LinkedHashSet<>();

        private final List<DanglingLine> danglingLines = new ArrayList<>();

        private final Set<ThreeWindingsTransformer> t3wtSet = new LinkedHashSet<>();

        private final Map<String, Integer> busIdToNum = new HashMap<>();

        private double maxNominalV = Double.MIN_VALUE;
    }

    public NetworkContext(Network network, List<Bus> buses, SlackBusSelectionMode slackBusSelectionMode,
                          Map<HvdcConverterStation, HvdcLine> hvdcLines) {
        this.network = Objects.requireNonNull(network);
        Objects.requireNonNull(buses);
        if (buses.isEmpty()) {
            throw new IllegalArgumentException("Empty bus list");
        }
        Objects.requireNonNull(slackBusSelectionMode);
        Objects.requireNonNull(hvdcLines);

        CreationContext creationContext = new CreationContext();
        this.buses = createBuses(buses, hvdcLines, creationContext);
        branches = createBranches(this.buses, creationContext);

        slackBus = selectSlackBus(slackBusSelectionMode, creationContext.maxNominalV);
    }

    private static List<LfBus> createBuses(List<Bus> buses, Map<HvdcConverterStation, HvdcLine> hvdcLines, CreationContext creationContext) {
        List<LfBus> lfBuses = new ArrayList<>(buses.size());
        int[] generatorCount = new int[1];

        for (Bus bus : buses) {
            LfBusImpl lfBus = addLfBus(bus, lfBuses, creationContext.busIdToNum);

            creationContext.maxNominalV = Math.max(creationContext.maxNominalV, lfBus.getNominalV());

            bus.visitConnectedEquipments(new DefaultTopologyVisitor() {

                private void visitBranch(Branch branch) {
                    creationContext.branchSet.add(branch);
                    // add to neighbors if connected at both sides
                    Bus bus1 = branch.getTerminal1().getBusView().getBus();
                    Bus bus2 = branch.getTerminal2().getBusView().getBus();
                    if (bus1 != null && bus2 != null) {
                        lfBus.addNeighbor();
                    }
                }

                @Override
                public void visitLine(Line line, Line.Side side) {
                    visitBranch(line);
                }

                @Override
                public void visitTwoWindingsTransformer(TwoWindingsTransformer transformer, TwoWindingsTransformer.Side side) {
                    visitBranch(transformer);
                }

                @Override
                public void visitThreeWindingsTransformer(ThreeWindingsTransformer transformer, ThreeWindingsTransformer.Side side) {
                    creationContext.t3wtSet.add(transformer);
                }

                @Override
                public void visitGenerator(Generator generator) {
                    lfBus.addGenerator(generator);
                    generatorCount[0]++;
                }

                @Override
                public void visitLoad(Load load) {
                    lfBus.addLoad(load);
                }

                @Override
                public void visitShuntCompensator(ShuntCompensator sc) {
                    lfBus.addShuntCompensator(sc);
                }

                @Override
                public void visitDanglingLine(DanglingLine danglingLine) {
                    creationContext.danglingLines.add(danglingLine);
                }

                @Override
                public void visitStaticVarCompensator(StaticVarCompensator staticVarCompensator) {
                    lfBus.addStaticVarCompensator(staticVarCompensator);
                }

                @Override
                public void visitBattery(Battery battery) {
                    lfBus.addBattery(battery);
                }

                @Override
                public void visitHvdcConverterStation(HvdcConverterStation<?> converterStation) {
                    switch (converterStation.getHvdcType()) {
                        case VSC:
                            visitVscConverterStation((VscConverterStation) converterStation);
                            break;
                        case LCC:
                            throw new UnsupportedOperationException("TODO: LCC");
                        default:
                            throw new IllegalStateException("Unknown HVDC converter station type: " + converterStation.getHvdcType());
                    }
                }

                private void visitVscConverterStation(VscConverterStation vscCs) {
                    HvdcLine line = hvdcLines.get(vscCs);
                    lfBus.addVscConverterStattion(vscCs, line);
                }
            });
        }

        if (generatorCount[0] == 0) {
            throw new PowsyblException("Connected component without any regulating generator");
        }

        return lfBuses;
    }

    private static List<LfBranch> createBranches(List<LfBus> lfBuses, CreationContext creationContext) {
        List<LfBranch> lfBranches = new ArrayList<>();

        for (Branch branch : creationContext.branchSet) {
            LfBus lfBus1 = getLfBus(branch.getTerminal1(), lfBuses, creationContext.busIdToNum);
            LfBus lfBus2 = getLfBus(branch.getTerminal2(), lfBuses, creationContext.busIdToNum);
            lfBranches.add(LfBranchImpl.create(branch, lfBus1, lfBus2));
        }

        for (DanglingLine danglingLine : creationContext.danglingLines) {
            LfDanglingLineBus lfBus2 = addLfBus(danglingLine, lfBuses, creationContext.busIdToNum);
            LfBus lfBus1 = getLfBus(danglingLine.getTerminal(), lfBuses, creationContext.busIdToNum);
            lfBranches.add(LfDanglingLineBranch.create(danglingLine, lfBus1, lfBus2));
        }

        for (ThreeWindingsTransformer t3wt : creationContext.t3wtSet) {
            LfStarBus lfBus0 = addLfBus(t3wt, lfBuses, creationContext.busIdToNum);
            LfBus lfBus1 = getLfBus(t3wt.getLeg1().getTerminal(), lfBuses, creationContext.busIdToNum);
            LfBus lfBus2 = getLfBus(t3wt.getLeg2().getTerminal(), lfBuses, creationContext.busIdToNum);
            LfBus lfBus3 = getLfBus(t3wt.getLeg3().getTerminal(), lfBuses, creationContext.busIdToNum);
            lfBranches.add(LfLeg1Branch.create(lfBus1, lfBus0, t3wt.getLeg1()));
            lfBranches.add(LfLeg2or3Branch.create(lfBus2, lfBus0, t3wt, t3wt.getLeg2()));
            lfBranches.add(LfLeg2or3Branch.create(lfBus3, lfBus0, t3wt, t3wt.getLeg3()));
        }

        return lfBranches;
    }

    private LfBus selectSlackBus(SlackBusSelectionMode slackBusSelectionMode, double maxNominalV) {
        LfBus selectedSlackBus;
        switch (slackBusSelectionMode) {
            case FIRST:
                selectedSlackBus = this.buses.get(0);
                break;
            case MOST_MESHED:
                // select most meshed bus among buses with highest nominal voltage
                selectedSlackBus = this.buses.stream()
                        .filter(bus -> bus.getNominalV() == maxNominalV)
                        .max(Comparator.comparingInt(LfBus::getNeighbors))
                        .orElseThrow(AssertionError::new);
                break;
            default:
                throw new IllegalStateException("Slack bus selection mode unknown:" + slackBusSelectionMode);
        }
        selectedSlackBus.setSlack(true);
        LOGGER.debug("Selected slack bus (mode={}): {}", slackBusSelectionMode, selectedSlackBus.getId());
        return selectedSlackBus;
    }

    private static LfBus getLfBus(Terminal terminal, List<LfBus> lfBuses, Map<String, Integer> busIdToNum) {
        Bus bus = terminal.getBusView().getBus();
        if (bus != null) {
            int num = busIdToNum.get(bus.getId());
            return lfBuses.get(num);
        }
        return null;
    }

    private static LfBusImpl addLfBus(Bus bus, List<LfBus> lfBuses, Map<String, Integer> busIdToNum) {
        int busNum = lfBuses.size();
        LfBusImpl lfBus = LfBusImpl.create(bus, busNum);
        busIdToNum.put(bus.getId(), busNum);
        lfBuses.add(lfBus);
        return lfBus;
    }

    private static LfDanglingLineBus addLfBus(DanglingLine danglingLine, List<LfBus> lfBuses, Map<String, Integer> busIdToNum) {
        int busNum = lfBuses.size();
        LfDanglingLineBus lfBus = new LfDanglingLineBus(danglingLine, busNum);
        busIdToNum.put(lfBus.getId(), busNum);
        lfBuses.add(lfBus);
        return lfBus;
    }

    private static LfStarBus addLfBus(ThreeWindingsTransformer t3wt, List<LfBus> lfBuses, Map<String, Integer> busIdToNum) {
        int busNum = lfBuses.size();
        LfStarBus lfBus = new LfStarBus(t3wt, busNum);
        busIdToNum.put(lfBus.getId(), busNum);
        lfBuses.add(lfBus);
        return lfBus;
    }

    public static List<NetworkContext> of(Network network, SlackBusSelectionMode slackBusSelectionMode) {
        Objects.requireNonNull(network);
        Map<Integer, List<Bus>> buseByCc = new TreeMap<>();
        for (Bus bus : network.getBusView().getBuses()) {
            Component cc = bus.getConnectedComponent();
            if (cc != null) {
                buseByCc.computeIfAbsent(cc.getNum(), k -> new ArrayList<>()).add(bus);
            }
        }

        // hack because there is no way to get HVDC line from converter station
        Map<HvdcConverterStation, HvdcLine> hvdcLines = new HashMap<>();
        for (HvdcLine line : network.getHvdcLines()) {
            hvdcLines.put(line.getConverterStation1(), line);
            hvdcLines.put(line.getConverterStation2(), line);
        }

        return buseByCc.entrySet().stream()
                .filter(e -> e.getKey() == ComponentConstants.MAIN_NUM)
                .map(e -> new NetworkContext(network, e.getValue(), slackBusSelectionMode, hvdcLines))
                .collect(Collectors.toList());
    }

    public static void resetState(Network network) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        for (Bus b : network.getBusView().getBuses()) {
            b.setV(Double.NaN);
            b.setAngle(Double.NaN);
        }
        for (ShuntCompensator sc : network.getShuntCompensators()) {
            sc.getTerminal().setQ(Double.NaN);
        }
        for (Branch b : network.getBranches()) {
            b.getTerminal1().setP(Double.NaN);
            b.getTerminal1().setQ(Double.NaN);
            b.getTerminal2().setP(Double.NaN);
            b.getTerminal2().setQ(Double.NaN);
        }

        stopwatch.stop();
        LOGGER.debug("Network reset done in {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    public List<LfBranch> getBranches() {
        return branches;
    }

    public List<LfBus> getBuses() {
        return buses;
    }

    public LfBus getBus(int num) {
        return buses.get(num);
    }

    public LfBus getSlackBus() {
        return slackBus;
    }

    public void updateState() {
        for (LfBus bus : buses) {
            bus.updateState();
            for (LfShunt shunt : bus.getShunts()) {
                shunt.updateState();
            }
        }
        for (LfBranch branch : branches) {
            branch.updateState();
        }
    }

    public void writeJson(Path file) {
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writeJson(writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeJson(LfBus bus, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStringField("id", bus.getId());
        jsonGenerator.writeNumberField("num", bus.getNum());
        if (bus.getGenerationTargetP() != 0) {
            jsonGenerator.writeNumberField("generationTargetP", bus.getGenerationTargetP());
        }
        if (bus.getGenerationTargetQ() != 0) {
            jsonGenerator.writeNumberField("generationTargetQ", bus.getGenerationTargetQ());
        }
        if (bus.getLoadTargetP() != 0) {
            jsonGenerator.writeNumberField("loadTargetP", bus.getLoadTargetP());
        }
        if (bus.getLoadTargetQ() != 0) {
            jsonGenerator.writeNumberField("loadTargetQ", bus.getLoadTargetQ());
        }
        if (!Double.isNaN(bus.getTargetV())) {
            jsonGenerator.writeNumberField("targetV", bus.getTargetV());
        }
        if (!Double.isNaN(bus.getV())) {
            jsonGenerator.writeNumberField("v", bus.getV());
        }
        if (!Double.isNaN(bus.getAngle())) {
            jsonGenerator.writeNumberField("angle", bus.getAngle());
        }
    }

    public void writeJson(LfBranch branch, JsonGenerator jsonGenerator) throws IOException {
        LfBus bus1 = branch.getBus1();
        LfBus bus2 = branch.getBus2();
        if (bus1 != null) {
            jsonGenerator.writeNumberField("num1", bus1.getNum());
        }
        if (bus2 != null) {
            jsonGenerator.writeNumberField("num2", bus2.getNum());
        }
        jsonGenerator.writeNumberField("x", branch.x());
        jsonGenerator.writeNumberField("y", branch.y());
        jsonGenerator.writeNumberField("ksi", branch.ksi());
        if (branch.g1() != 0) {
            jsonGenerator.writeNumberField("g1", branch.g1());
        }
        if (branch.g2() != 0) {
            jsonGenerator.writeNumberField("g2", branch.g2());
        }
        if (branch.b1() != 0) {
            jsonGenerator.writeNumberField("b1", branch.b1());
        }
        if (branch.b2() != 0) {
            jsonGenerator.writeNumberField("b2", branch.b2());
        }
        if (branch.r1() != 1) {
            jsonGenerator.writeNumberField("r1", branch.r1());
        }
        if (branch.r2() != 1) {
            jsonGenerator.writeNumberField("r2", branch.r2());
        }
        if (branch.a1() != 0) {
            jsonGenerator.writeNumberField("a1", branch.a1());
        }
        if (branch.a2() != 0) {
            jsonGenerator.writeNumberField("a2", branch.a2());
        }
    }

    public void writeJson(LfShunt shunt, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeNumberField("b", shunt.getB());
    }

    public void writeJson(Writer writer) {
        Objects.requireNonNull(writer);
        try (JsonGenerator jsonGenerator = new JsonFactory()
                .createGenerator(writer)
                .useDefaultPrettyPrinter()) {
            jsonGenerator.writeStartObject();

            jsonGenerator.writeFieldName("buses");
            jsonGenerator.writeStartArray();
            for (LfBus bus : buses) {
                jsonGenerator.writeStartObject();

                writeJson(bus, jsonGenerator);

                List<LfShunt> shunts = bus.getShunts();
                if (!shunts.isEmpty()) {
                    jsonGenerator.writeFieldName("shunts");
                    jsonGenerator.writeStartArray();
                    for (LfShunt shunt : shunts) {
                        jsonGenerator.writeStartObject();

                        writeJson(shunt, jsonGenerator);

                        jsonGenerator.writeEndObject();
                    }
                    jsonGenerator.writeEndArray();
                }

                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndArray();

            jsonGenerator.writeFieldName("branches");
            jsonGenerator.writeStartArray();
            for (LfBranch branch : branches) {
                jsonGenerator.writeStartObject();

                writeJson(branch, jsonGenerator);

                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndArray();

            jsonGenerator.writeEndObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

