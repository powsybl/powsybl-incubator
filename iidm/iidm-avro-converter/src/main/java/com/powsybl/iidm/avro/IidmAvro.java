/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.avro;

import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.iidm.import_.ImportOptions;
import com.powsybl.iidm.network.*;
import org.apache.avro.io.*;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.eu>
 */
public final class IidmAvro {

    private static final Logger LOGGER = LoggerFactory.getLogger(IidmAvro.class);

    private IidmAvro() {
    }

    public static void write(Network network, DataSource dataSource, String dataSourceExt) throws IOException {
        ANetwork.Builder nBuilder = ANetwork.newBuilder();
        nBuilder.setId(network.getId())
                .setCaseDate(network.getCaseDate().toString())
                .setForecastDistance(network.getForecastDistance())
                .setSourceFormat(network.getSourceFormat());
        writeSubstations(network, nBuilder);
        writeLines(network, nBuilder);
        writeHvdcLines(network, nBuilder);

        ANetwork aNetwork = nBuilder.build();
        try (OutputStream osb = dataSource.newOutputStream("", dataSourceExt, false);
             BufferedOutputStream bosb = new BufferedOutputStream(osb)) {

            DatumWriter<ANetwork> outputDatumWriter = new SpecificDatumWriter<ANetwork>(ANetwork.class);
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(bosb, null);
            outputDatumWriter.write(aNetwork, encoder);
            encoder.flush();
        }

    }

    private static void writeLines(Network network, ANetwork.Builder nBuilder) {
        nBuilder.setLine(network.getLineStream().map(IidmAvro::writeLine).collect(Collectors.toList()));
    }

    private static ALine writeLine(Line line) {
        if (line.isTieLine()) {
            throw new UnsupportedOperationException("tie lines not yet supported, tie line: " + line);
        } else {
            ALine.Builder lBuilder = ALine.newBuilder();
            lBuilder.setId(line.getId());
            if (line.getName() != null) {
                lBuilder.setName(line.getName());
            }

            lBuilder.setR(line.getR());
            lBuilder.setX(line.getX());
            lBuilder.setB1(line.getB1());
            lBuilder.setB2(line.getB2());
            lBuilder.setG1(line.getG1());
            lBuilder.setG2(line.getG2());

            writeNodeOrBus(1, line.getTerminal1(), lBuilder);
            writeNodeOrBus(2, line.getTerminal2(), lBuilder);

            if (!Double.isNaN(line.getTerminal1().getP())) {
                lBuilder.setP1(line.getTerminal1().getP());
            }
            if (!Double.isNaN(line.getTerminal1().getQ())) {
                lBuilder.setQ1(line.getTerminal1().getQ());
            }

            if (!Double.isNaN(line.getTerminal2().getP())) {
                lBuilder.setP2(line.getTerminal2().getP());
            }
            if (!Double.isNaN(line.getTerminal2().getQ())) {
                lBuilder.setQ2(line.getTerminal2().getQ());
            }

            if (line.getCurrentLimits1() != null) {
                writeCurrentLimits(1, line.getCurrentLimits1(), lBuilder);
            }
            if (line.getCurrentLimits2() != null) {
                writeCurrentLimits(2, line.getCurrentLimits2(), lBuilder);
            }
            return lBuilder.build();
        }
    }

    private static void readLines(ANetwork aNetwork, Network network) {
        List<ALine> lines = aNetwork.getLine();
        lines.forEach(pl -> {
            LineAdder lAdder = network.newLine();
            lAdder.setId(pl.getId());
            lAdder.setName(pl.getName());
            lAdder.setR(pl.getR());
            lAdder.setX(pl.getX());
            lAdder.setB1(pl.getB1());
            lAdder.setB2(pl.getB2());
            lAdder.setG1(pl.getG1());
            lAdder.setG2(pl.getG2());

            readNodeOrBus(lAdder, pl);

            Line line = lAdder.add();
            if (pl.getP1() != null) {
                line.getTerminal1().setP(pl.getP1());
            }

            if (pl.getQ1() != null) {
                line.getTerminal1().setQ(pl.getQ1());
            }

            if (pl.getP2() != null) {
                line.getTerminal2().setP(pl.getP2());
            }
            if (pl.getQ2() != null) {
                line.getTerminal2().setQ(pl.getQ2());
            }

            if (pl.getCurrentLimits1() != null) {
                readCurrentLimits(line::newCurrentLimits1, pl.getCurrentLimits1());
            }
            if (pl.getCurrentLimits2() != null) {
                readCurrentLimits(line::newCurrentLimits2, pl.getCurrentLimits2());
            }
        });

    }

    private static void readNodeOrBus(LineAdder adder, ALine pLine) {
        if (pLine.getBus1() != null) {
            adder.setBus1(pLine.getBus1());
        }
        if (pLine.getConnectableBus1() != null) {
            adder.setConnectableBus1(pLine.getConnectableBus1());
        }
        if (pLine.getNode1() != null) {
            adder.setNode1(pLine.getNode1());
        }
        adder.setVoltageLevel1(pLine.getVoltageLevelId1());
        if (pLine.getBus2() != null) {
            adder.setBus2(pLine.getBus2());
        }
        if (pLine.getConnectableBus2() != null) {
            adder.setConnectableBus2(pLine.getConnectableBus2());
        }
        if (pLine.getNode2() != null) {
            adder.setNode2(pLine.getNode2());
        }
        adder.setVoltageLevel2(pLine.getVoltageLevelId2());
    }

    private static void writeNodeOrBus(Integer index, Terminal t, ALine.Builder builder) {
        TopologyKind topologyKind = t.getVoltageLevel().getTopologyKind();
        switch (topologyKind) {
            case NODE_BREAKER:
                writeNode(index, t, builder);
                break;
            case BUS_BREAKER:
                writeBus(index, t.getBusBreakerView().getBus(), t.getBusBreakerView().getConnectableBus(), builder);
                break;
            default:
                throw new AssertionError("Unexpected topologyKind value: " + topologyKind);
        }
        if (index != null) {
            writeVoltageLevelId(index, t, builder);
        }

    }

    private static void writeNode(Integer index, Terminal t, ALine.Builder builder) {
        int nodeIndex = t.getNodeBreakerView().getNode();
        switch (index) {
            case 1:
                builder.setNode1(nodeIndex);
                break;
            case 2:
                builder.setNode2(nodeIndex);
                break;
            default:
                throw new AssertionError("Unexpected index value: " + index);
        }
    }

    private static void writeBus(Integer index, Bus bus, Bus connectableBus, ALine.Builder builder) {
        switch (index) {
            case 1:
                if (bus != null) {
                    builder.setBus1(bus.getId());
                }
                if (connectableBus != null) {
                    builder.setConnectableBus1(connectableBus.getId());
                }
                break;
            case 2:
                if (bus != null) {
                    builder.setBus2(bus.getId());
                }
                if (connectableBus != null) {
                    builder.setConnectableBus2(connectableBus.getId());
                }
                break;
            default:
                throw new AssertionError("Unexpected index value: " + index);

        }
    }

    private static void writeVoltageLevelId(Integer index, Terminal t, ALine.Builder builder) {
        switch (index) {
            case 1:
                builder.setVoltageLevelId1(t.getVoltageLevel().getId());
                break;
            case 2:
                builder.setVoltageLevelId2(t.getVoltageLevel().getId());
                break;
            default:
                throw new AssertionError("Unexpected index value: " + index);

        }
    }

    private static void writeCurrentLimits(int i, CurrentLimits limits, ALine.Builder builder) {
        CurrentLimit.Builder limitBuilder = CurrentLimit.newBuilder();
        if (!Double.isNaN(limits.getPermanentLimit())
                || !limits.getTemporaryLimits().isEmpty()) {
            limitBuilder.setPermanentLimit(limits.getPermanentLimit());

            List<TemporaryLimit> tlimits = new ArrayList<>();
            for (CurrentLimits.TemporaryLimit tl : limits.getTemporaryLimits()) {
                TemporaryLimit.Builder tempBuilder = TemporaryLimit.newBuilder();
                tempBuilder.setName(tl.getName());
                tempBuilder.setAcceptableDuration(tl.getAcceptableDuration());
                tempBuilder.setValue(tl.getValue());
                tempBuilder.setFictitious(tl.isFictitious());
                tlimits.add(tempBuilder.build());
            }
            if (tlimits.size() > 0) {
                limitBuilder.setTemporaryLimit(tlimits);
            }
        }
        switch (i) {
            case 1:
                builder.setCurrentLimits1(limitBuilder.build());
                break;
            case 2:
                builder.setCurrentLimits2(limitBuilder.build());
                break;
            default:
                throw new AssertionError("Unexpected value: " + i);
        }
    }

    private static void writeSubstations(Network network, ANetwork.Builder nBuilder) {
        nBuilder.setSubstation(network.getSubstationStream().map(IidmAvro::writeSubstation).collect(Collectors.toList()));
    }

    private static ASubstation writeSubstation(Substation sub) {
        ASubstation.Builder sBuilder = ASubstation.newBuilder()
                .setId(sub.getId())
                .setName(sub.getName())
                .setTso(sub.getTso())
                .setGeographicalTags(new ArrayList<>(sub.getGeographicalTags()));

        Optional<Country> country = sub.getCountry();
        if (country.isPresent()) {
            sBuilder.setCountry(country.get().toString());
        }

        List<AVoltageLevel> avls = new ArrayList<>();
        sub.getVoltageLevelStream().forEach(vl -> {
            avls.add(writeVoltageLevel(vl, sBuilder));
        });
        sBuilder.setVoltageLevel(avls);

        List<ATwoWindingsTransformer> twts = new ArrayList<>();
        sub.getTwoWindingsTransformerStream().forEach(twt -> {
            twts.add(writeTwowindingsTransformer(twt, sBuilder));
        });
        sBuilder.setTwoWindingsTransformer(twts);

        List<AThreeWindingsTransformer> threeWindingsTransformers = new ArrayList<>();
        sub.getThreeWindingsTransformerStream().forEach(twt -> {
            threeWindingsTransformers.add(writeThreeWindingsTransformer(twt, sBuilder));
        });
        sBuilder.setThreeWindingsTransformer(threeWindingsTransformers);

        sBuilder.setProperty(writeProperties(sub));

        return sBuilder.build();
    }

    private static AThreeWindingsTransformer writeThreeWindingsTransformer(ThreeWindingsTransformer twt, ASubstation.Builder sBuilder) {
        AThreeWindingsTransformer.Builder tBuilder = AThreeWindingsTransformer.newBuilder();
        tBuilder.setId(twt.getId());
        if (twt.getName() != null) {
            tBuilder.setName(twt.getName());
        }
        tBuilder.setB1(twt.getLeg1().getB());
        tBuilder.setG1(twt.getLeg1().getG());
        tBuilder.setR1(twt.getLeg1().getR());
        tBuilder.setR2(twt.getLeg2().getR());
        tBuilder.setR3(twt.getLeg3().getR());
        tBuilder.setRatedU1(twt.getLeg1().getRatedU());
        tBuilder.setRatedU2(twt.getLeg2().getRatedU());
        tBuilder.setRatedU3(twt.getLeg3().getRatedU());
        tBuilder.setX1(twt.getLeg1().getX());
        tBuilder.setX2(twt.getLeg2().getX());
        tBuilder.setX3(twt.getLeg3().getX());

        writeNodeOrBus(1, twt.getLeg1().getTerminal(), tBuilder);
        writeNodeOrBus(2, twt.getLeg2().getTerminal(), tBuilder);
        writeNodeOrBus(3, twt.getLeg3().getTerminal(), tBuilder);

        if (twt.getLeg1().getCurrentLimits() != null) {
            writeCurrentLimits(1, twt.getLeg1().getCurrentLimits(), tBuilder);
        }
        if (twt.getLeg2().getCurrentLimits() != null) {
            writeCurrentLimits(2, twt.getLeg2().getCurrentLimits(), tBuilder);
        }
        if (twt.getLeg3().getCurrentLimits() != null) {
            writeCurrentLimits(3, twt.getLeg3().getCurrentLimits(), tBuilder);
        }

        RatioTapChanger rtc2 = twt.getLeg2().getRatioTapChanger();
        if (rtc2 != null) {
            tBuilder.setRatioTapChanger2(writeRatioTapChanger(rtc2, tBuilder));
        }
        RatioTapChanger rtc3 = twt.getLeg3().getRatioTapChanger();
        if (rtc3 != null) {
            tBuilder.setRatioTapChanger3(writeRatioTapChanger(rtc3, tBuilder));
        }

        if (!Double.isNaN(twt.getLeg1().getTerminal().getP())) {
            tBuilder.setP1(twt.getLeg1().getTerminal().getP());
        }
        if (!Double.isNaN(twt.getLeg1().getTerminal().getQ())) {
            tBuilder.setQ1(twt.getLeg1().getTerminal().getQ());
        }

        if (!Double.isNaN(twt.getLeg2().getTerminal().getP())) {
            tBuilder.setP2(twt.getLeg2().getTerminal().getP());
        }
        if (!Double.isNaN(twt.getLeg2().getTerminal().getQ())) {
            tBuilder.setQ2(twt.getLeg2().getTerminal().getQ());
        }

        if (!Double.isNaN(twt.getLeg3().getTerminal().getP())) {
            tBuilder.setP3(twt.getLeg3().getTerminal().getP());
        }
        if (!Double.isNaN(twt.getLeg3().getTerminal().getQ())) {
            tBuilder.setQ3(twt.getLeg3().getTerminal().getQ());
        }

        tBuilder.setProperty(writeProperties(twt));
        return tBuilder.build();

    }

    private static ATwoWindingsTransformer writeTwowindingsTransformer(TwoWindingsTransformer twt, ASubstation.Builder sBuilder) {
        ATwoWindingsTransformer.Builder tBuilder = ATwoWindingsTransformer.newBuilder();
        tBuilder.setId(twt.getId());
        if (twt.getName() != null) {
            tBuilder.setName(twt.getName());
        }
        tBuilder.setR(twt.getR());
        tBuilder.setX(twt.getX());
        tBuilder.setG(twt.getG());
        tBuilder.setB(twt.getB());
        tBuilder.setRatedU1(twt.getRatedU1());
        tBuilder.setRatedU2(twt.getRatedU2());

        writeNodeOrBus(1, twt.getTerminal1(), tBuilder);
        writeNodeOrBus(2, twt.getTerminal2(), tBuilder);

        if (!Double.isNaN(twt.getTerminal1().getP())) {
            tBuilder.setP1(twt.getTerminal1().getP());
        }
        if (!Double.isNaN(twt.getTerminal1().getQ())) {
            tBuilder.setQ1(twt.getTerminal1().getQ());
        }

        if (!Double.isNaN(twt.getTerminal2().getP())) {
            tBuilder.setP2(twt.getTerminal2().getP());
        }
        if (!Double.isNaN(twt.getTerminal2().getQ())) {
            tBuilder.setQ2(twt.getTerminal2().getQ());
        }

        RatioTapChanger rtc = twt.getRatioTapChanger();
        if (rtc != null) {
            writeRatioTapChanger(rtc, tBuilder);
        }
        PhaseTapChanger ptc = twt.getPhaseTapChanger();
        if (ptc != null) {
            writePhaseTapChanger(ptc, tBuilder);
        }
        if (twt.getCurrentLimits1() != null) {
            writeCurrentLimits(1, twt.getCurrentLimits1(), tBuilder);
        }
        if (twt.getCurrentLimits2() != null) {
            writeCurrentLimits(2, twt.getCurrentLimits2(), tBuilder);
        }
        tBuilder.setProperty(writeProperties(twt));
        ATwoWindingsTransformer pTwt = tBuilder.build();
        return pTwt;

    }

    private static void writeRatioTapChanger(RatioTapChanger rtc, ATwoWindingsTransformer.Builder tBuilder) {
        ARatioTapChanger.Builder rtcBuilder = ARatioTapChanger.newBuilder();

        rtcBuilder.setTapPosition(rtc.getTapPosition());
        rtcBuilder.setLowTapPosition(rtc.getLowTapPosition());
        if (!Double.isNaN(rtc.getTargetDeadband())) {
            rtcBuilder.setTargetDeadband(rtc.getTargetDeadband());
        }

        rtcBuilder.setLoadTapChangingCapabilities(rtc.hasLoadTapChangingCapabilities());
        if (rtc.hasLoadTapChangingCapabilities() || rtc.isRegulating()) {
            rtcBuilder.setRegulating(rtc.isRegulating());
        }
        if (rtc.hasLoadTapChangingCapabilities() || !Double.isNaN(rtc.getTargetV())) {
            rtcBuilder.setTargetV(rtc.getTargetV());
        }
        if (rtc.getRegulationTerminal() != null) {
            rtcBuilder.setTerminalRef(writeTerminalRef(rtc.getRegulationTerminal()));
        }

        List<com.powsybl.iidm.avro.ARatioTapChangerStep> ptrcSteps = new ArrayList<>();
        for (int p = rtc.getLowTapPosition(); p <= rtc.getHighTapPosition(); p++) {
            RatioTapChangerStep rtcs = rtc.getStep(p);
            ARatioTapChangerStep.Builder stepBuilder = ARatioTapChangerStep.newBuilder();
            stepBuilder.setB(rtcs.getB());
            stepBuilder.setG(rtcs.getG());
            stepBuilder.setR(rtcs.getR());
            stepBuilder.setRho(rtcs.getRho());
            stepBuilder.setX(rtcs.getX());
            ptrcSteps.add(stepBuilder.build());
        }
        rtcBuilder.setStep(ptrcSteps);
        tBuilder.setRatioTapChanger(rtcBuilder.build());
    }

    private static ARatioTapChanger writeRatioTapChanger(RatioTapChanger rtc, AThreeWindingsTransformer.Builder tBuilder) {
        ARatioTapChanger.Builder rtcBuilder = ARatioTapChanger.newBuilder();

        rtcBuilder.setTapPosition(rtc.getTapPosition());
        rtcBuilder.setLowTapPosition(rtc.getLowTapPosition());
        if (!Double.isNaN(rtc.getTargetDeadband())) {
            rtcBuilder.setTargetDeadband(rtc.getTargetDeadband());
        }

        rtcBuilder.setLoadTapChangingCapabilities(rtc.hasLoadTapChangingCapabilities());
        if (rtc.hasLoadTapChangingCapabilities() || rtc.isRegulating()) {
            rtcBuilder.setRegulating(rtc.isRegulating());
        }
        if (rtc.hasLoadTapChangingCapabilities() || !Double.isNaN(rtc.getTargetV())) {
            rtcBuilder.setTargetV(rtc.getTargetV());
        }
        if (rtc.getRegulationTerminal() != null) {
            rtcBuilder.setTerminalRef(writeTerminalRef(rtc.getRegulationTerminal()));
        }

        List<ARatioTapChangerStep> ptrcSteps = new ArrayList<>();
        for (int p = rtc.getLowTapPosition(); p <= rtc.getHighTapPosition(); p++) {
            RatioTapChangerStep rtcs = rtc.getStep(p);
            ARatioTapChangerStep.Builder stepBuilder = ARatioTapChangerStep.newBuilder();
            stepBuilder.setB(rtcs.getB());
            stepBuilder.setG(rtcs.getG());
            stepBuilder.setR(rtcs.getR());
            stepBuilder.setRho(rtcs.getRho());
            stepBuilder.setX(rtcs.getX());
            ptrcSteps.add(stepBuilder.build());
        }
        rtcBuilder.setStep(ptrcSteps);
        return rtcBuilder.build();
    }

    private static void writePhaseTapChanger(PhaseTapChanger ptc, ATwoWindingsTransformer.Builder tBuilder) {
        APhaseTapChanger.Builder ptcBuilder = APhaseTapChanger.newBuilder();

        ptcBuilder.setTapPosition(ptc.getTapPosition());
        ptcBuilder.setLowTapPosition(ptc.getLowTapPosition());
        ptcBuilder.setRegulationMode(ptc.getRegulationMode().toString());

        if (!Double.isNaN(ptc.getTargetDeadband())) {
            ptcBuilder.setTargetDeadband(ptc.getTargetDeadband());
        }

        if (ptc.getRegulationMode() != PhaseTapChanger.RegulationMode.FIXED_TAP || !Double.isNaN(ptc.getRegulationValue())) {
            ptcBuilder.setRegulationValue(ptc.getRegulationValue());
        }
        if (ptc.getRegulationMode() != PhaseTapChanger.RegulationMode.FIXED_TAP || ptc.isRegulating()) {
            ptcBuilder.setRegulating(ptc.isRegulating());
        }
        if (ptc.getRegulationTerminal() != null) {
            ptcBuilder.setTerminalRef(writeTerminalRef(ptc.getRegulationTerminal()));
        }

        List<APhaseTapChangerStep> pSteps = new ArrayList<>();
        for (int p = ptc.getLowTapPosition(); p <= ptc.getHighTapPosition(); p++) {
            PhaseTapChangerStep ptcs = ptc.getStep(p);
            APhaseTapChangerStep.Builder pstepBuilder = APhaseTapChangerStep.newBuilder();
            pstepBuilder.setR(ptcs.getR());
            pstepBuilder.setX(ptcs.getX());
            pstepBuilder.setG(ptcs.getG());
            pstepBuilder.setB(ptcs.getB());
            pstepBuilder.setRho(ptcs.getRho());
            pstepBuilder.setAlpha(ptcs.getAlpha());
            pSteps.add(pstepBuilder.build());
        }
        ptcBuilder.setStep(pSteps);
        tBuilder.setPhaseTapChanger(ptcBuilder.build());
    }

    private static ATerminalRef writeTerminalRef(Terminal regulationTerminal) {
        ATerminalRef.Builder termBuilder = ATerminalRef.newBuilder();
        Connectable c = regulationTerminal.getConnectable();
        termBuilder.setId(c.getId());
        if (c.getTerminals().size() > 1) {
            if (c instanceof Injection) {
                // nothing to do
            } else if (c instanceof Branch) {
                Branch branch = (Branch) c;
                termBuilder.setSide(branch.getSide(regulationTerminal).toString());
            } else if (c instanceof com.powsybl.iidm.network.ThreeWindingsTransformer) {
                com.powsybl.iidm.network.ThreeWindingsTransformer twt = (ThreeWindingsTransformer) c;
                termBuilder.setSide(twt.getSide(regulationTerminal).toString());
            } else {
                throw new AssertionError("Unexpected Connectable instance: " + c.getClass());
            }
        }
        return termBuilder.build();
    }

    private static void writeNodeOrBus(Integer index, Terminal t, ATwoWindingsTransformer.Builder builder) {
        TopologyKind topologyKind = t.getVoltageLevel().getTopologyKind();
        switch (topologyKind) {
            case NODE_BREAKER:
                writeNode(index, t, builder);
                break;
            case BUS_BREAKER:
                writeBus(index, t.getBusBreakerView().getBus(), t.getBusBreakerView().getConnectableBus(), builder);
                break;
            default:
                throw new AssertionError("Unexpected topologyKind value: " + topologyKind);
        }
        if (index != null) {
            writeVoltageLevelId(index, t, builder);
        }

    }

    private static void writeNodeOrBus(Integer index, Terminal t, AThreeWindingsTransformer.Builder builder) {
        TopologyKind topologyKind = t.getVoltageLevel().getTopologyKind();
        switch (topologyKind) {
            case NODE_BREAKER:
                writeNode(index, t, builder);
                break;
            case BUS_BREAKER:
                writeBus(index, t.getBusBreakerView().getBus(), t.getBusBreakerView().getConnectableBus(), builder);
                break;
            default:
                throw new AssertionError("Unexpected topologyKind value: " + topologyKind);
        }
        if (index != null) {
            writeVoltageLevelId(index, t, builder);
        }
    }

    private static void writeVoltageLevelId(Integer index, Terminal t, ATwoWindingsTransformer.Builder builder) {
        switch (index) {
            case 1:
                builder.setVoltageLevelId1(t.getVoltageLevel().getId());
                break;
            case 2:
                builder.setVoltageLevelId2(t.getVoltageLevel().getId());
                break;
            default:
                throw new AssertionError("Unexpected index value: " + index);

        }
    }

    private static void writeVoltageLevelId(Integer index, Terminal t, AThreeWindingsTransformer.Builder builder) {
        switch (index) {
            case 1:
                builder.setVoltageLevelId1(t.getVoltageLevel().getId());
                break;
            case 2:
                builder.setVoltageLevelId2(t.getVoltageLevel().getId());
                break;
            case 3:
                builder.setVoltageLevelId3(t.getVoltageLevel().getId());
                break;
            default:
                throw new AssertionError("Unexpected index value: " + index);

        }
    }

    private static void writeNode(Integer index, Terminal t, ATwoWindingsTransformer.Builder builder) {
        int nodeIndex = t.getNodeBreakerView().getNode();
        switch (index) {
            case 1:
                builder.setNode1(nodeIndex);
                break;
            case 2:
                builder.setNode2(nodeIndex);
                break;
            default:
                throw new AssertionError("Unexpected index value: " + index);
        }
    }

    private static void writeBus(Integer index, Bus bus, Bus connectableBus, AThreeWindingsTransformer.Builder builder) {
        switch (index) {
            case 1:
                if (bus != null) {
                    builder.setBus1(bus.getId());
                }
                if (connectableBus != null) {
                    builder.setConnectableBus1(connectableBus.getId());
                }
                break;
            case 2:
                if (bus != null) {
                    builder.setBus2(bus.getId());
                }
                if (connectableBus != null) {
                    builder.setConnectableBus2(connectableBus.getId());
                }
                break;
            case 3:
                if (bus != null) {
                    builder.setBus3(bus.getId());
                }
                if (connectableBus != null) {
                    builder.setConnectableBus3(connectableBus.getId());
                }
                break;
            default:
                throw new AssertionError("Unexpected index value: " + index);

        }
    }

    private static void writeNode(Integer index, Terminal t, AThreeWindingsTransformer.Builder builder) {
        int nodeIndex = t.getNodeBreakerView().getNode();
        switch (index) {
            case 1:
                builder.setNode1(nodeIndex);
                break;
            case 2:
                builder.setNode2(nodeIndex);
                break;
            case 3:
                builder.setNode3(nodeIndex);
                break;
            default:
                throw new AssertionError("Unexpected index value: " + index);
        }
    }

    private static void writeBus(Integer index, Bus bus, Bus connectableBus, ATwoWindingsTransformer.Builder builder) {
        switch (index) {
            case 1:
                if (bus != null) {
                    builder.setBus1(bus.getId());
                }
                if (connectableBus != null) {
                    builder.setConnectableBus1(connectableBus.getId());
                }
                break;
            case 2:
                if (bus != null) {
                    builder.setBus2(bus.getId());
                }
                if (connectableBus != null) {
                    builder.setConnectableBus2(connectableBus.getId());
                }
                break;
            default:
                throw new AssertionError("Unexpected index value: " + index);

        }
    }

    private static void writeCurrentLimits(int i, CurrentLimits limits, ATwoWindingsTransformer.Builder tBuilder) {
        CurrentLimit.Builder limitBuilder = CurrentLimit.newBuilder();
        if (!Double.isNaN(limits.getPermanentLimit())
                || !limits.getTemporaryLimits().isEmpty()) {
            limitBuilder.setPermanentLimit(limits.getPermanentLimit());
            List<TemporaryLimit> tlimits = new ArrayList<>();
            for (CurrentLimits.TemporaryLimit tl : limits.getTemporaryLimits()) {
                TemporaryLimit.Builder tempBuilder = TemporaryLimit.newBuilder();
                tempBuilder.setName(tl.getName());
                tempBuilder.setAcceptableDuration(tl.getAcceptableDuration());
                tempBuilder.setValue(tl.getValue());
                tempBuilder.setFictitious(tl.isFictitious());
                tlimits.add(tempBuilder.build());

            }
            if (tlimits.size() > 0) {
                limitBuilder.setTemporaryLimit(tlimits);
            }
        }
        switch (i) {
            case 1:
                tBuilder.setCurrentLimits1(limitBuilder.build());
                break;
            case 2:
                tBuilder.setCurrentLimits2(limitBuilder.build());
                break;
            default:
                throw new AssertionError("Unexpected value: " + i);
        }
    }

    private static void writeCurrentLimits(int i, CurrentLimits limits, AThreeWindingsTransformer.Builder tBuilder) {
        CurrentLimit.Builder limitBuilder = CurrentLimit.newBuilder();
        if (!Double.isNaN(limits.getPermanentLimit())
                || !limits.getTemporaryLimits().isEmpty()) {
            limitBuilder.setPermanentLimit(limits.getPermanentLimit());
            List<TemporaryLimit> tlimits = new ArrayList<>();
            for (CurrentLimits.TemporaryLimit tl : limits.getTemporaryLimits()) {
                TemporaryLimit.Builder tempBuilder = TemporaryLimit.newBuilder();
                tempBuilder.setName(tl.getName());
                tempBuilder.setAcceptableDuration(tl.getAcceptableDuration());
                tempBuilder.setValue(tl.getValue());
                tempBuilder.setFictitious(tl.isFictitious());
                tlimits.add(tempBuilder.build());
            }
            if (tlimits.size() > 0) {
                limitBuilder.setTemporaryLimit(tlimits);
            }
        }
        switch (i) {
            case 1:
                tBuilder.setCurrentLimits1(limitBuilder.build());
                break;
            case 2:
                tBuilder.setCurrentLimits2(limitBuilder.build());
                break;
            case 3:
                tBuilder.setCurrentLimits3(limitBuilder.build());
                break;
            default:
                throw new AssertionError("Unexpected value: " + i);
        }
    }

    private static AVoltageLevel writeVoltageLevel(VoltageLevel vl, ASubstation.Builder sBuilder) {
        TopologyKind topologyKind = vl.getTopologyKind();
        AVoltageLevel.Builder vBuilder = AVoltageLevel.newBuilder();
        vBuilder.setId(vl.getId());
        vBuilder.setName(vl.getName());
        vBuilder.setTopologyKind(vl.getTopologyKind().toString());
        vBuilder.setNominalV(vl.getNominalV());
        if (!Double.isNaN(vl.getHighVoltageLimit())) {
            vBuilder.setHighVoltageLimit(vl.getHighVoltageLimit());
        }
        if (!Double.isNaN(vl.getLowVoltageLimit())) {
            vBuilder.setLowVoltageLimit(vl.getLowVoltageLimit());
        }

        switch (topologyKind) {
            case NODE_BREAKER:
                throw new UnsupportedOperationException("topology " + topologyKind + " is not supported, yet");

            case BUS_BREAKER:
                ABusBreakerTopology.Builder bbtBuilder = ABusBreakerTopology.newBuilder();

                List<ABus> aBuses = new ArrayList<>();
                for (com.powsybl.iidm.network.Bus b : vl.getBusBreakerView().getBuses()) {
                    ABus.Builder bBuilder = ABus.newBuilder();
                    bBuilder.setId(b.getId());
                    if (b.getName() != null) {
                        bBuilder.setName(b.getName());
                    }
                    if (!Double.isNaN(b.getAngle())) {
                        bBuilder.setAngle(b.getAngle());
                    }
                    if (!Double.isNaN(b.getV())) {
                        bBuilder.setV(b.getV());
                    }
                    aBuses.add(bBuilder.build());
                }
                bbtBuilder.setBus(aBuses);

                List<ASwitchBus> switchBuses = new ArrayList<>();
                for (Switch sw : vl.getBusBreakerView().getSwitches()) {
                    com.powsybl.iidm.network.Bus b1 = vl.getBusBreakerView().getBus1(sw.getId());
                    Bus b2 = vl.getBusBreakerView().getBus2(sw.getId());

                    ASwitchBus.Builder bswBuilder = ASwitchBus.newBuilder();
                    bswBuilder.setId(sw.getId());
                    if (sw.getName() != null) {
                        bswBuilder.setName(sw.getName());
                    }
                    bswBuilder.setKind(sw.getKind().toString());
                    bswBuilder.setOpen(sw.isOpen());
                    bswBuilder.setRetained(sw.isRetained());
                    bswBuilder.setBus1(b1.getId());
                    bswBuilder.setBus2(b2.getId());
                    bswBuilder.setFictitious(sw.isFictitious());
                    bswBuilder.setProperty(sw.getPropertyNames().stream().map(pName -> new Property(pName, sw.getProperty(pName))).collect(Collectors.toList()));
                    switchBuses.add(bswBuilder.build());
                }
                bbtBuilder.setSwitch$(switchBuses);

                vBuilder.setBusBreakerTopology(bbtBuilder.build());
                break;

            default:
                throw new AssertionError("Unexpected topologyKind value: " + topologyKind);
        }

        writeBatteries(vl, topologyKind, vBuilder);
        writeLoads(vl, topologyKind, vBuilder);
        writeGenerators(vl, topologyKind, vBuilder);
        writeShunts(vl, topologyKind, vBuilder);
        writeDanglingLines(vl, topologyKind, vBuilder);
        writeStaticVarCompensators(vl, topologyKind, vBuilder);
        writeVscConverterStations(vl, topologyKind, vBuilder);
        writeLccConverterStations(vl, topologyKind, vBuilder);
        vBuilder.setProperty(writeProperties(vl));
        return vBuilder.build();
    }

    private static void writeLccConverterStations(VoltageLevel vl, TopologyKind topologyKind, AVoltageLevel.Builder vBuilder) {
        vBuilder.setLccConverterStation(vl.getLccConverterStationStream().map(lcc -> writeLccConverterStation(lcc, topologyKind, vBuilder)).collect(Collectors.toList()));
    }

    private static ALccConverterStation writeLccConverterStation(LccConverterStation lcc, TopologyKind topologyKind, AVoltageLevel.Builder vBuilder) {
        ALccConverterStation.Builder builder = ALccConverterStation.newBuilder();
        builder.setId(lcc.getId());
        if (lcc.getName() != null) {
            builder.setName(lcc.getName());
        }
        builder.setLossFactor(lcc.getLossFactor());
        builder.setPowerFactor(lcc.getPowerFactor());

        addNodeOrBus(builder, topologyKind, lcc);

        if (!Double.isNaN(lcc.getTerminal().getP())) {
            builder.setP(lcc.getTerminal().getP());
        }
        if (!Double.isNaN(lcc.getTerminal().getQ())) {
            builder.setQ(lcc.getTerminal().getQ());
        }

        builder.setProperty(writeProperties(lcc));
        return builder.build();
    }

    private static void addNodeOrBus(ALccConverterStation.Builder builder, TopologyKind topologyKind, LccConverterStation lcc) {
        switch (topologyKind) {
            case NODE_BREAKER:
                builder.setNode(lcc.getTerminal().getNodeBreakerView().getNode());
                break;
            case BUS_BREAKER:
                builder.setBus(lcc.getTerminal().getBusBreakerView().getBus().getId());
                if (lcc.getTerminal().getBusBreakerView().getConnectableBus() != null) {
                    builder.setConnectableBus(lcc.getTerminal().getBusBreakerView().getConnectableBus().getId());
                }
                break;
            default:
                throw new AssertionError("Unexpected TopologyKind value: " + topologyKind);
        }
    }

    private static void writeVscConverterStations(VoltageLevel vl, TopologyKind topologyKind, AVoltageLevel.Builder vBuilder) {
        vBuilder.setVscConverterStation(vl.getVscConverterStationStream().map(cs -> writeVscConverterStation(cs, topologyKind, vBuilder)).collect(Collectors.toList()));
    }

    private static AVscConverterStation writeVscConverterStation(VscConverterStation vsc, TopologyKind topologyKind, AVoltageLevel.Builder vBuilder) {
        AVscConverterStation.Builder builder = AVscConverterStation.newBuilder();
        builder.setId(vsc.getId());
        if (vsc.getName() != null) {
            builder.setName(vsc.getName());
        }
        builder.setLossFactor(vsc.getLossFactor());
        builder.setVoltageRegulatorOn(vsc.isVoltageRegulatorOn());

        switch (vsc.getReactiveLimits().getKind()) {
            case CURVE:
                builder.setReactiveCapabilityCurve(ReactiveLimitsAvro.INSTANCE.writeReactiveCababilityCurve(vsc));
                break;
            case MIN_MAX:
                builder.setMinMaxReactiveLimits(ReactiveLimitsAvro.INSTANCE.writeMixMaxReactiveLimits(vsc));
                break;
            default:
                throw new AssertionError();
        }

        if (!Double.isNaN(vsc.getReactivePowerSetpoint())) {
            builder.setReactivePowerSetpoint(vsc.getReactivePowerSetpoint());
        }
        if (!Double.isNaN(vsc.getVoltageSetpoint())) {
            builder.setVoltageSetpoint(vsc.getVoltageSetpoint());
        }

        addNodeOrBus(builder, topologyKind, vsc);

        if (!Double.isNaN(vsc.getTerminal().getP())) {
            builder.setP(vsc.getTerminal().getP());
        }
        if (!Double.isNaN(vsc.getTerminal().getQ())) {
            builder.setQ(vsc.getTerminal().getQ());
        }

        builder.setProperty(writeProperties(vsc));
        return builder.build();
    }

    private static void writeStaticVarCompensators(VoltageLevel vl, TopologyKind topologyKind, AVoltageLevel.Builder vBuilder) {
        vBuilder.setStaticVarCompensator(vl.getStaticVarCompensatorStream().map(svc -> writeStaticVarCompensator(svc, topologyKind, vBuilder)).collect(Collectors.toList()));
    }

    private static AStaticVarCompensator writeStaticVarCompensator(StaticVarCompensator svc, TopologyKind topologyKind, AVoltageLevel.Builder vBuilder) {
        AStaticVarCompensator.Builder pSvcBuilder = AStaticVarCompensator.newBuilder();
        pSvcBuilder.setId(svc.getId());
        if (svc.getName() != null) {
            pSvcBuilder.setName(svc.getName());
        }
        pSvcBuilder.setRegulationMode(svc.getRegulationMode().toString());
        pSvcBuilder.setBMin(svc.getBmin());
        pSvcBuilder.setBMax(svc.getBmax());
        if (!Double.isNaN(svc.getReactivePowerSetPoint())) {
            pSvcBuilder.setReactivePowerSetPoint(svc.getReactivePowerSetPoint());
        }
        if (!Double.isNaN(svc.getVoltageSetPoint())) {
            pSvcBuilder.setVoltageSetPoint(svc.getVoltageSetPoint());
        }

        addNodeOrBus(pSvcBuilder, topologyKind, svc);

        if (!Double.isNaN(svc.getTerminal().getP())) {
            pSvcBuilder.setP(svc.getTerminal().getP());
        }
        if (!Double.isNaN(svc.getTerminal().getQ())) {
            pSvcBuilder.setQ(svc.getTerminal().getQ());
        }

        pSvcBuilder.setProperty(writeProperties(svc));
        return pSvcBuilder.build();
    }

    private static void addNodeOrBus(AVscConverterStation.Builder builder, TopologyKind topologyKind, VscConverterStation vsc) {
        switch (topologyKind) {
            case NODE_BREAKER:
                builder.setNode(vsc.getTerminal().getNodeBreakerView().getNode());
                break;
            case BUS_BREAKER:
                builder.setBus(vsc.getTerminal().getBusBreakerView().getBus().getId());
                if (vsc.getTerminal().getBusBreakerView().getConnectableBus() != null) {
                    builder.setConnectableBus(vsc.getTerminal().getBusBreakerView().getConnectableBus().getId());
                }
                break;
            default:
                throw new AssertionError("Unexpected TopologyKind value: " + topologyKind);
        }
    }

    private static void addNodeOrBus(AStaticVarCompensator.Builder builder, TopologyKind topologyKind, StaticVarCompensator svc) {
        switch (topologyKind) {
            case NODE_BREAKER:
                builder.setNode(svc.getTerminal().getNodeBreakerView().getNode());
                break;
            case BUS_BREAKER:
                builder.setBus(svc.getTerminal().getBusBreakerView().getBus().getId());
                if (svc.getTerminal().getBusBreakerView().getConnectableBus() != null) {
                    builder.setConnectableBus(svc.getTerminal().getBusBreakerView().getConnectableBus().getId());
                }
                break;
            default:
                throw new AssertionError("Unexpected TopologyKind value: " + topologyKind);
        }
    }

    private static void writeDanglingLines(VoltageLevel vl, TopologyKind topologyKind, AVoltageLevel.Builder vBuilder) {
        vBuilder.setDanglingLine(vl.getDanglingLineStream().map(dline -> writeDanglingLine(dline, topologyKind, vBuilder)).collect(Collectors.toList()));
    }

    private static ADanglingLine writeDanglingLine(DanglingLine dl, TopologyKind topologyKind, AVoltageLevel.Builder vBuilder) {
        ADanglingLine.Builder dlBuilder = ADanglingLine.newBuilder();
        dlBuilder.setId(dl.getId());
        if (dl.getName() != null) {
            dlBuilder.setName(dl.getName());
        }

        dlBuilder.setB(dl.getB());
        dlBuilder.setG(dl.getG());
        dlBuilder.setR(dl.getR());
        dlBuilder.setP0(dl.getP0());
        dlBuilder.setQ0(dl.getQ0());
        dlBuilder.setX(dl.getX());

        if (dl.getUcteXnodeCode() != null) {
            dlBuilder.setUcteXnodeCode(dl.getUcteXnodeCode());
        }

        if (dl.getCurrentLimits() != null) {
            writeCurrentLimits(dl.getCurrentLimits(), dlBuilder);
        }

        addNodeOrBus(dlBuilder, topologyKind, dl);

        if (!Double.isNaN(dl.getTerminal().getP())) {
            dlBuilder.setP(dl.getTerminal().getP());
        }
        if (!Double.isNaN(dl.getTerminal().getQ())) {
            dlBuilder.setQ(dl.getTerminal().getQ());
        }

        dlBuilder.setProperty(writeProperties(dl));
        return dlBuilder.build();
    }

    private static void writeCurrentLimits(CurrentLimits limits, ADanglingLine.Builder builder) {
        CurrentLimit.Builder limitBuilder = CurrentLimit.newBuilder();
        if (!Double.isNaN(limits.getPermanentLimit())
                || !limits.getTemporaryLimits().isEmpty()) {
            limitBuilder.setPermanentLimit(limits.getPermanentLimit());
            List<TemporaryLimit> tlimits = new ArrayList<>();
            for (CurrentLimits.TemporaryLimit tl : limits.getTemporaryLimits()) {
                TemporaryLimit.Builder tempBuilder = TemporaryLimit.newBuilder();
                tempBuilder.setName(tl.getName());
                tempBuilder.setAcceptableDuration(tl.getAcceptableDuration());
                tempBuilder.setValue(tl.getValue());
                tempBuilder.setFictitious(tl.isFictitious());
                tlimits.add(tempBuilder.build());
            }
            if (tlimits.size() > 0) {
                limitBuilder.setTemporaryLimit(tlimits);
            }
        }
        builder.setCurrentLimits(limitBuilder.build());
    }

    private static void addNodeOrBus(ADanglingLine.Builder dlBuilder, TopologyKind topologyKind, DanglingLine dl) {
        switch (topologyKind) {
            case NODE_BREAKER:
                dlBuilder.setNode(dl.getTerminal().getNodeBreakerView().getNode());
                break;
            case BUS_BREAKER:
                dlBuilder.setBus(dl.getTerminal().getBusBreakerView().getBus().getId());
                if (dl.getTerminal().getBusBreakerView().getConnectableBus() != null) {
                    dlBuilder.setConnectableBus(dl.getTerminal().getBusBreakerView().getConnectableBus().getId());
                }
                break;
            default:
                throw new AssertionError("Unexpected TopologyKind value: " + topologyKind);
        }
    }

    private static void writeShunts(VoltageLevel vl, TopologyKind topologyKind, AVoltageLevel.Builder vBuilder) {
        vBuilder.setShunt(vl.getShuntCompensatorStream().map(shunt -> writeShunt(shunt, topologyKind, vBuilder)).collect(Collectors.toList()));
    }

    private static AShuntCompensator writeShunt(ShuntCompensator s, TopologyKind topologyKind, AVoltageLevel.Builder vBuilder) {
        AShuntCompensator.Builder shBuilder = AShuntCompensator.newBuilder();
        shBuilder.setId(s.getId());
        if (s.getName() != null) {
            shBuilder.setName(s.getName());
        }
        shBuilder.setBPerSection(s.getB());
        shBuilder.setCurrentSectionCount(s.getSectionCount());
        shBuilder.setMaximumSectionCount(s.getMaximumSectionCount());

        addNodeOrBus(shBuilder, topologyKind, s);

        if (!Double.isNaN(s.getTerminal().getP())) {
            shBuilder.setP(s.getTerminal().getP());
        }
        if (!Double.isNaN(s.getTerminal().getQ())) {
            shBuilder.setQ(s.getTerminal().getQ());
        }

        shBuilder.setProperty(writeProperties(s));
        return shBuilder.build();
    }

    private static void addNodeOrBus(AShuntCompensator.Builder shBuilder, TopologyKind topologyKind, ShuntCompensator s) {
        switch (topologyKind) {
            case NODE_BREAKER:
                shBuilder.setNode(s.getTerminal().getNodeBreakerView().getNode());
                break;
            case BUS_BREAKER:
                shBuilder.setBus(s.getTerminal().getBusBreakerView().getBus().getId());
                if (s.getTerminal().getBusBreakerView().getConnectableBus() != null) {
                    shBuilder.setConnectableBus(s.getTerminal().getBusBreakerView().getConnectableBus().getId());
                }
                break;
            default:
                throw new AssertionError("Unexpected TopologyKind value: " + topologyKind);
        }
    }

    private static void writeBatteries(VoltageLevel vl, TopologyKind topologyKind, AVoltageLevel.Builder vBuilder) {
        vBuilder.setBattery(vl.getBatteryStream().map(battery -> writeBattery(battery, topologyKind, vBuilder)).collect(Collectors.toList()));
    }

    private static ABattery writeBattery(Battery b, TopologyKind topologyKind, AVoltageLevel.Builder vBuilder) {
        ABattery.Builder builder = ABattery.newBuilder();
        builder.setId(b.getId());
        if (b.getName() != null) {
            builder.setName(b.getName());
        }
        builder.setMinP(b.getMinP());
        builder.setMaxP(b.getMaxP());
        builder.setP0(b.getP0());
        builder.setQ0(b.getQ0());

        switch (b.getReactiveLimits().getKind()) {
            case CURVE:
                builder.setReactiveCapabilityCurve(ReactiveLimitsAvro.INSTANCE.writeReactiveCababilityCurve(b));
                break;
            case MIN_MAX:
                builder.setMinMaxReactiveLimits(ReactiveLimitsAvro.INSTANCE.writeMixMaxReactiveLimits(b));
                break;
            default:
                throw new AssertionError();
        }

        addNodeOrBus(builder, topologyKind, b);

        if (!Double.isNaN(b.getTerminal().getP())) {
            builder.setP(b.getTerminal().getP());
        }
        if (!Double.isNaN(b.getTerminal().getQ())) {
            builder.setQ(b.getTerminal().getQ());
        }
        builder.setProperty(writeProperties(b));
        return builder.build();
    }

    private static void addNodeOrBus(ABattery.Builder builder, TopologyKind topologyKind, Battery b) {
        switch (topologyKind) {
            case NODE_BREAKER:
                builder.setNode(b.getTerminal().getNodeBreakerView().getNode());
                break;
            case BUS_BREAKER:
                builder.setBus(b.getTerminal().getBusBreakerView().getBus().getId());
                if (b.getTerminal().getBusBreakerView().getConnectableBus() != null) {
                    builder.setConnectableBus(b.getTerminal().getBusBreakerView().getConnectableBus().getId());
                }
                break;
            default:
                throw new AssertionError("Unexpected TopologyKind value: " + topologyKind);
        }

    }

    private static void writeLoads(VoltageLevel vl, TopologyKind topologyKind, AVoltageLevel.Builder vBuilder) {
        vBuilder.setLoad(vl.getLoadStream().map(load -> writeLoad(load, topologyKind, vBuilder)).collect(Collectors.toList()));
    }

    private static void writeGenerators(VoltageLevel vl, TopologyKind topologyKind, AVoltageLevel.Builder vBuilder) {
        vBuilder.setGenerator(vl.getGeneratorStream().map(gen -> writeGenerator(gen, topologyKind, vBuilder)).collect(Collectors.toList()));
    }

    protected static void addNodeOrBus(AGenerator.Builder generatorBuilder, TopologyKind topologyKind, Generator generator) {
        switch (topologyKind) {
            case NODE_BREAKER:
                generatorBuilder.setNode(generator.getTerminal().getNodeBreakerView().getNode());
                break;
            case BUS_BREAKER:
                generatorBuilder.setBus(generator.getTerminal().getBusBreakerView().getBus().getId());
                if (generator.getTerminal().getBusBreakerView().getConnectableBus() != null) {
                    generatorBuilder.setConnectableBus(generator.getTerminal().getBusBreakerView().getConnectableBus().getId());
                }
                break;
            default:
                throw new AssertionError("Unexpected TopologyKind value: " + topologyKind);
        }
    }

    protected static void addNodeOrBus(ALoad.Builder builder, TopologyKind topologyKind, Load load) {
        switch (topologyKind) {
            case NODE_BREAKER:
                builder.setNode(load.getTerminal().getNodeBreakerView().getNode());
                break;
            case BUS_BREAKER:
                builder.setBus(load.getTerminal().getBusBreakerView().getBus().getId());
                if (load.getTerminal().getBusBreakerView().getConnectableBus() != null) {
                    builder.setConnectableBus(load.getTerminal().getBusBreakerView().getConnectableBus().getId());
                }
                break;
            default:
                throw new AssertionError("Unexpected TopologyKind value: " + topologyKind);
        }
    }

    private static AGenerator writeGenerator(Generator generator, TopologyKind topologyKind, AVoltageLevel.Builder vBuilder) {
        AGenerator.Builder builder = AGenerator.newBuilder();

        builder.setId(generator.getId());
        if (generator.getName() != null) {
            builder.setName(generator.getName());
        }
        builder.setEnergySource(generator.getEnergySource().toString());
        builder.setMinP(generator.getMinP());
        builder.setMaxP(generator.getMaxP());
        builder.setTargetP(generator.getTargetP());
        builder.setVoltageRegulatorOn(generator.isVoltageRegulatorOn());

        if (!Double.isNaN(generator.getRatedS())) {
            builder.setRatedS(generator.getRatedS());
        }

        if (!Double.isNaN(generator.getTargetQ())) {
            builder.setTargetQ(generator.getTargetQ());
        }

        if (!Double.isNaN(generator.getTargetV())) {
            builder.setTargetV(generator.getTargetV());
        }

        if (!Double.isNaN(generator.getTerminal().getP())) {
            builder.setP(generator.getTerminal().getP());
        }

        if (!Double.isNaN(generator.getTerminal().getQ())) {
            builder.setQ(generator.getTerminal().getQ());
        }

        switch (generator.getReactiveLimits().getKind()) {
            case CURVE:
                builder.setReactiveCapabilityCurve(ReactiveLimitsAvro.INSTANCE.writeReactiveCababilityCurve(generator));
                break;

            case MIN_MAX:
                builder.setMinMaxReactiveLimits(ReactiveLimitsAvro.INSTANCE.writeMixMaxReactiveLimits(generator));
                break;

            default:
                throw new AssertionError();
        }

        addNodeOrBus(builder, topologyKind, generator);
        builder.setProperty(writeProperties(generator));

        return builder.build();

    }

    private static ALoad writeLoad(Load load, TopologyKind topologyKind, AVoltageLevel.Builder vBuilder) {
        ALoad.Builder builder = ALoad.newBuilder();

        builder.setId(load.getId());
        if (load.getName() != null) {
            builder.setName(load.getName());
        }

        builder.setP0(load.getP0());
        builder.setQ0(load.getQ0());
        builder.setLoadType(load.getLoadType().toString());

        if (!Double.isNaN(load.getTerminal().getP())) {
            builder.setP(load.getTerminal().getP());
        }
        if (!Double.isNaN(load.getTerminal().getQ())) {
            builder.setQ(load.getTerminal().getQ());
        }

        addNodeOrBus(builder, topologyKind, load);
        if (!Double.isNaN(load.getTerminal().getP())) {
            builder.setP(load.getTerminal().getP());
        }
        if (!Double.isNaN(load.getTerminal().getQ())) {
            builder.setQ(load.getTerminal().getQ());
        }

        builder.setProperty(writeProperties(load));

        return builder.build();

    }

    public static Network read(ReadOnlyDataSource dataSource, NetworkFactory networkFactory, ImportOptions options, String ext) throws IOException {
        Objects.requireNonNull(dataSource);
        Network network = null;
        try (InputStream isb = dataSource.newInputStream(null, ext)) {
            DatumReader<ANetwork> empDatumReader = new SpecificDatumReader<ANetwork>(ANetwork.class);
            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(isb, null);
            ANetwork aNetwork = empDatumReader.read(null, decoder);

            network = networkFactory.createNetwork(aNetwork.getId().toString(), aNetwork.getSourceFormat().toString());
            network.setCaseDate(DateTime.parse(aNetwork.getCaseDate().toString()));
            network.setForecastDistance(aNetwork.getForecastDistance());

            final Network network1 = network;
            List<ASubstation> aSubstations = aNetwork.getSubstation();
            aSubstations.forEach(asub -> {
                SubstationAdder subsAdder = network1.newSubstation();
                Country country = Optional.ofNullable(Country.valueOf(asub.getCountry().toString())).orElse(null);
                subsAdder.setId(asub.getId());
                subsAdder.setName(asub.getName());
                subsAdder.setTso(asub.getTso());
                subsAdder.setGeographicalTags(asub.getGeographicalTags().stream().toArray(String[]::new));
                if (country != null) {
                    subsAdder.setCountry(country);
                }
                Substation sub = subsAdder.add();
                readProperties(sub, asub.getProperty());

                List<AVoltageLevel> avls = asub.getVoltageLevel();
                avls.forEach(avl -> {
                    VoltageLevelAdder vlAdder = sub.newVoltageLevel();
                    vlAdder.setId(avl.getId());
                    vlAdder.setName(avl.getName());
                    vlAdder.setNominalV(avl.getNominalV());

                    if (avl.getHighVoltageLimit() != null) {
                        vlAdder.setHighVoltageLimit(avl.getHighVoltageLimit());
                    }
                    if (avl.getLowVoltageLimit() != null) {
                        vlAdder.setLowVoltageLimit(avl.getLowVoltageLimit());
                    }

                    TopologyKind topologyKind = TopologyKind.valueOf(avl.getTopologyKind());

                    vlAdder.setTopologyKind(topologyKind);

                    VoltageLevel newVoltageLevel = vlAdder.add();
                    switch (topologyKind) {
                        case NODE_BREAKER:
                            break;

                        case BUS_BREAKER:
                            List<ABus> bList = avl.getBusBreakerTopology().getBus();
                            bList.forEach(b -> {
                                BusAdder busAdder = newVoltageLevel.getBusBreakerView().newBus();
                                busAdder.setId(b.getId());
                                busAdder.setName(b.getName());
                                Bus newBus = busAdder.add();
                                if (b.getAngle() != null) {
                                    newBus.setAngle(b.getAngle());
                                }
                                if (b.getV() != null) {
                                    newBus.setV(b.getV());
                                }

                            });
                            List<ASwitchBus> swList = avl.getBusBreakerTopology().getSwitch$();
                            swList.forEach(asw -> {
                                VoltageLevel.BusBreakerView.SwitchAdder bswAdder = newVoltageLevel.getBusBreakerView().newSwitch();
                                bswAdder.setId(asw.getId());
                                bswAdder.setName(asw.getName());
                                bswAdder.setOpen(asw.getOpen());
                                bswAdder.setBus1(asw.getBus1());
                                bswAdder.setBus2(asw.getBus2());
                                bswAdder.setFictitious(asw.getFictitious());
                                Switch newSwitch = bswAdder.add();
                                List<Property> properties = asw.getProperty();
                                properties.stream().forEach(property -> {
                                    newSwitch.setProperty(property.getName(), property.getValue());
                                });
                            });
                            break;
                        default:
                            throw new AssertionError("Unexpected topologyKind value: " + topologyKind);
                    }

                    readBatteries(newVoltageLevel, avl, topologyKind);
                    readLoads(newVoltageLevel, avl, topologyKind);
                    readGenerators(newVoltageLevel, avl);
                    readShunts(newVoltageLevel, avl, topologyKind);
                    readDanglingLines(newVoltageLevel, avl, topologyKind);
                    readStaticVarCompensators(newVoltageLevel, avl, topologyKind);
                    readVscConverterStations(newVoltageLevel, avl, topologyKind);
                    readLccConverterStations(newVoltageLevel, avl, topologyKind);

                    readProperties(newVoltageLevel, avl.getProperty());
                });

                readTwoWindingTransformers(asub, sub, network1);
                readThreeWindingTransformers(asub, sub, network1);

            });

            readLines(aNetwork, network);
            readHvdcLines(aNetwork, network);

        }
        return network;
    }

    private static void readLccConverterStations(VoltageLevel newVoltageLevel, AVoltageLevel avl, TopologyKind topologyKind) {
        List<ALccConverterStation> plccList = avl.getLccConverterStation();
        plccList.forEach(plcc -> {
            LccConverterStationAdder adder = newVoltageLevel.newLccConverterStation();
            adder.setId(plcc.getId());
            if (plcc.getName() != null) {
                adder.setName(plcc.getName());
            }
            adder.setLossFactor(plcc.getLossFactor());
            adder.setPowerFactor(plcc.getPowerFactor());

            if (plcc.getBus() != null) {
                adder.setBus(plcc.getBus());
            }

            if (plcc.getConnectableBus() != null) {
                adder.setConnectableBus(plcc.getConnectableBus());
            }

            if (plcc.getNode() != null) {
                adder.setNode(plcc.getNode());
            }

            LccConverterStation lcc = adder.add();

            if (plcc.getP() != null) {
                lcc.getTerminal().setP(plcc.getP());
            }
            if (plcc.getQ() != null) {
                lcc.getTerminal().setQ(plcc.getQ());
            }
            readProperties(lcc, plcc.getProperty());
        });
    }

    private static void readVscConverterStations(VoltageLevel newVoltageLevel, AVoltageLevel avl, TopologyKind topologyKind) {
        List<AVscConverterStation> pvscList = avl.getVscConverterStation();
        pvscList.forEach(pvsc -> {
            VscConverterStationAdder adder = newVoltageLevel.newVscConverterStation();
            adder.setId(pvsc.getId());
            if (pvsc.getName() != null) {
                adder.setName(pvsc.getName());
            }
            adder.setLossFactor(pvsc.getLossFactor());
            adder.setVoltageRegulatorOn(pvsc.getVoltageRegulatorOn());

            if (pvsc.getReactivePowerSetpoint() != null) {
                adder.setReactivePowerSetpoint(pvsc.getReactivePowerSetpoint());
            }

            if (pvsc.getVoltageSetpoint() != null) {
                adder.setVoltageSetpoint(pvsc.getVoltageSetpoint());
            }

            if (pvsc.getBus() != null) {
                adder.setBus(pvsc.getBus());
            }

            if (pvsc.getConnectableBus() != null) {
                adder.setConnectableBus(pvsc.getConnectableBus());
            }

            if (pvsc.getNode() != null) {
                adder.setNode(pvsc.getNode());
            }

            VscConverterStation vsc = adder.add();

            if (pvsc.getMinMaxReactiveLimits() != null) {
                ReactiveLimitsAvro.INSTANCE.readMinMaxReactiveLimits(vsc, pvsc.getMinMaxReactiveLimits());
            }
            if (pvsc.getReactiveCapabilityCurve() != null) {
                ReactiveLimitsAvro.INSTANCE.readReactiveCapabilityCurve(vsc, pvsc.getReactiveCapabilityCurve());
            }

            if (pvsc.getP() != null) {
                vsc.getTerminal().setP(pvsc.getP());
            }
            if (pvsc.getQ() != null) {
                vsc.getTerminal().setQ(pvsc.getQ());
            }
            readProperties(vsc, pvsc.getProperty());
        });
    }

    private static void readStaticVarCompensators(VoltageLevel newVoltageLevel, AVoltageLevel avl, TopologyKind topologyKind) {
        List<AStaticVarCompensator> psvcList = avl.getStaticVarCompensator();
        psvcList.forEach(psvc -> {
            StaticVarCompensatorAdder adder = newVoltageLevel.newStaticVarCompensator();
            adder.setId(psvc.getId());
            if (psvc.getName() != null) {
                adder.setName(psvc.getName());
            }

            adder.setRegulationMode(StaticVarCompensator.RegulationMode.valueOf(psvc.getRegulationMode()));
            adder.setBmin(psvc.getBMin());
            adder.setBmax(psvc.getBMax());
            if (psvc.getReactivePowerSetPoint() != null) {
                if (!Double.isNaN(psvc.getReactivePowerSetPoint())) {
                    adder.setReactivePowerSetPoint(psvc.getReactivePowerSetPoint());
                }
            }
            if (psvc.getVoltageSetPoint() != null) {
                if (!Double.isNaN(psvc.getVoltageSetPoint())) {
                    adder.setVoltageSetPoint(psvc.getVoltageSetPoint());
                }
            }

            if (psvc.getBus() != null) {
                adder.setBus(psvc.getBus());
            }

            if (psvc.getConnectableBus() != null) {
                adder.setConnectableBus(psvc.getConnectableBus());
            }

            if (psvc.getNode() != null) {
                adder.setNode(psvc.getNode());
            }

            StaticVarCompensator svc = adder.add();
            if (psvc.getP() != null) {
                svc.getTerminal().setP(psvc.getP());
            }
            if (psvc.getQ() != null) {
                svc.getTerminal().setQ(psvc.getQ());
            }
            readProperties(svc, psvc.getProperty());
        });
    }

    private static void readDanglingLines(VoltageLevel newVoltageLevel, AVoltageLevel avl, TopologyKind topologyKind) {
        List<ADanglingLine> pDanglingLines = avl.getDanglingLine();
        pDanglingLines.forEach(pdl -> {
            DanglingLineAdder adder = newVoltageLevel.newDanglingLine();
            adder.setId(pdl.getId());
            if (pdl.getName() != null) {
                adder.setName(pdl.getName());
            }

            adder.setB(pdl.getB());
            adder.setG(pdl.getG());
            adder.setR(pdl.getR());
            adder.setP0(pdl.getP0());
            adder.setQ0(pdl.getQ0());
            adder.setX(pdl.getX());
            if (pdl.getUcteXnodeCode() != null) {
                adder.setUcteXnodeCode(pdl.getUcteXnodeCode());
            }

            if (pdl.getBus() != null) {
                adder.setBus(pdl.getBus());
            }

            if (pdl.getConnectableBus() != null) {
                adder.setConnectableBus(pdl.getConnectableBus());
            }

            if (pdl.getNode() != null) {
                adder.setNode(pdl.getNode());
            }

            DanglingLine newDanglingLine = adder.add();

            if (pdl.getCurrentLimits() != null) {
                readCurrentLimits(newDanglingLine::newCurrentLimits, pdl.getCurrentLimits());
            }

            if (pdl.getP() != null) {
                newDanglingLine.getTerminal().setP(pdl.getP());
            }
            if (pdl.getQ() != null) {
                newDanglingLine.getTerminal().setQ(pdl.getQ());
            }
            readProperties(newDanglingLine, pdl.getProperty());
        });
    }

    private static void readShunts(VoltageLevel newVoltageLevel, AVoltageLevel avl, TopologyKind topologyKind) {
        List<AShuntCompensator> pShuntsList = avl.getShunt();
        pShuntsList.forEach(pshunt -> {
            ShuntCompensatorAdder adder = newVoltageLevel.newShuntCompensator();
            adder.setId(pshunt.getId());
            if (pshunt.getName() != null) {
                adder.setName(pshunt.getName());
            }
            adder.newLinearModel()
                    .setBPerSection(pshunt.getBPerSection())
                    .setMaximumSectionCount(pshunt.getMaximumSectionCount())
                    .add()
                    .setSectionCount(pshunt.getCurrentSectionCount());

            if (pshunt.getBus() != null) {
                adder.setBus(pshunt.getBus());
            }

            if (pshunt.getConnectableBus() != null) {
                adder.setConnectableBus(pshunt.getConnectableBus());
            }

            if (pshunt.getNode() != null) {
                adder.setNode(pshunt.getNode());
            }

            ShuntCompensator newShunt = adder.add();
            if (pshunt.getP() != null) {
                newShunt.getTerminal().setP(pshunt.getP());
            }
            if (pshunt.getQ() != null) {
                newShunt.getTerminal().setQ(pshunt.getQ());
            }
            readProperties(newShunt, pshunt.getProperty());
        });

    }

    private static void readBatteries(VoltageLevel vl, AVoltageLevel avl, TopologyKind topologyKind) {
        List<ABattery> pbatteries = avl.getBattery();

        pbatteries.forEach(pb -> {
            BatteryAdder adder = vl.newBattery();
            adder.setId(pb.getId());
            if (pb.getName() != null) {
                adder.setName(pb.getName());
            }

            adder.setMinP(pb.getMinP());
            adder.setMaxP(pb.getMaxP());
            adder.setP0(pb.getP0());
            adder.setQ0(pb.getQ0());

            if (pb.getBus() != null) {
                adder.setBus(pb.getBus());
            }

            if (pb.getConnectableBus() != null) {
                adder.setConnectableBus(pb.getConnectableBus());
            }

            if (pb.getNode() != null) {
                adder.setNode(pb.getNode());
            }
            Battery newBattery = adder.add();

            if (pb.getMinMaxReactiveLimits() != null) {
                ReactiveLimitsAvro.INSTANCE.readMinMaxReactiveLimits(newBattery, pb.getMinMaxReactiveLimits());
            }
            if (pb.getReactiveCapabilityCurve() != null) {
                ReactiveLimitsAvro.INSTANCE.readReactiveCapabilityCurve(newBattery, pb.getReactiveCapabilityCurve());
            }

            if (pb.getP() != null) {
                newBattery.getTerminal().setP(pb.getP());
            }
            if (pb.getQ() != null) {
                newBattery.getTerminal().setQ(pb.getQ());
            }

            readProperties(newBattery, pb.getProperty());
        });

    }

    private static void readTwoWindingTransformers(ASubstation asub, Substation sub, Network network1) {
        List<ATwoWindingsTransformer> ptList = asub.getTwoWindingsTransformer();
        ptList.forEach(pTwt -> {
            TwoWindingsTransformerAdder tAdder = sub.newTwoWindingsTransformer();
            tAdder.setId(pTwt.getId());
            tAdder.setName(pTwt.getName());
            tAdder.setR(pTwt.getR());
            tAdder.setX(pTwt.getX());
            tAdder.setG(pTwt.getG());
            tAdder.setB(pTwt.getB());
            tAdder.setRatedU1(pTwt.getRatedU1());
            tAdder.setRatedU2(pTwt.getRatedU2());

            readNodeOrBus(tAdder, pTwt);
            TwoWindingsTransformer twt = tAdder.add();
            if (pTwt.getRatioTapChanger() != null) {
                readRatioTapChanger(twt, pTwt.getRatioTapChanger(), network1);
            }

            if (pTwt.getPhaseTapChanger() != null) {
                readPhaseTapChanger(twt, pTwt.getPhaseTapChanger(), network1);
            }

            if (pTwt.getP1() != null) {
                twt.getTerminal1().setP(pTwt.getP1());
            }

            if (pTwt.getQ1() != null) {
                twt.getTerminal1().setQ(pTwt.getQ1());
            }

            if (pTwt.getP2() != null) {
                twt.getTerminal2().setP(pTwt.getP2());
            }
            if (pTwt.getQ2() != null) {
                twt.getTerminal2().setQ(pTwt.getQ2());
            }

            if (pTwt.getCurrentLimits1() != null) {
                readCurrentLimits(twt::newCurrentLimits1, pTwt.getCurrentLimits1());
            }
            if (pTwt.getCurrentLimits2() != null) {
                readCurrentLimits(twt::newCurrentLimits2, pTwt.getCurrentLimits2());
            }

            readProperties(twt, pTwt.getProperty());
        });
    }

    public static void readCurrentLimits(Supplier<CurrentLimitsAdder> currentLimitOwner, CurrentLimit plimit) {
        CurrentLimitsAdder adder = currentLimitOwner.get();
        if (plimit.getPermanentLimit() != null) {
            adder.setPermanentLimit(plimit.getPermanentLimit());
        }

        List<TemporaryLimit> tempLimits = plimit.getTemporaryLimit();
        if (tempLimits != null) {
            tempLimits.forEach(ptl -> {
                adder.beginTemporaryLimit()
                        .setName(ptl.getName())
                        .setAcceptableDuration((ptl.getAcceptableDuration() != null) ? ptl.getAcceptableDuration() : Integer.MAX_VALUE)
                        .setValue((ptl.getValue() != null) ? ptl.getValue() : Double.MAX_VALUE)
                        .setFictitious((ptl.getFictitious() != null) ? ptl.getFictitious() : false)
                        .endTemporaryLimit();
            });
        }
        adder.add();
    }

    private static void readThreeWindingTransformers(ASubstation asub, Substation sub, Network network1) {
        List<AThreeWindingsTransformer> ptList = asub.getThreeWindingsTransformer();
        ptList.forEach(pTwt -> {
            ThreeWindingsTransformerAdder adder = sub.newThreeWindingsTransformer();
            adder.setId(pTwt.getId());
            if (pTwt.getName() != null) {
                adder.setName(pTwt.getName());
            }

            ThreeWindingsTransformerAdder.LegAdder legAdder1 = adder.newLeg1().setR(pTwt.getR1()).setX(pTwt.getX1()).setG(pTwt.getG1()).setB(pTwt.getB1()).setRatedU(pTwt.getRatedU1());
            ThreeWindingsTransformerAdder.LegAdder legAdder2 = adder.newLeg2().setR(pTwt.getR2()).setX(pTwt.getX2()).setRatedU(pTwt.getRatedU2());
            ThreeWindingsTransformerAdder.LegAdder legAdder3 = adder.newLeg3().setR(pTwt.getR3()).setX(pTwt.getX3()).setRatedU(pTwt.getRatedU3());

            readNodeOrBus(legAdder1, legAdder2, legAdder3, pTwt);

            legAdder1.add();
            legAdder2.add();
            legAdder3.add();

            ThreeWindingsTransformer twt = adder.add();
            if (pTwt.getP1() != null) {
                twt.getLeg1().getTerminal().setP(pTwt.getP1());
            }

            if (pTwt.getQ1() != null) {
                twt.getLeg1().getTerminal().setQ(pTwt.getQ1());
            }

            if (pTwt.getP2() != null) {
                twt.getLeg2().getTerminal().setP(pTwt.getP2());
            }

            if (pTwt.getQ2() != null) {
                twt.getLeg2().getTerminal().setQ(pTwt.getQ2());
            }

            if (pTwt.getRatioTapChanger2() != null) {
                readRatioTapChanger(twt.getLeg2(), pTwt.getRatioTapChanger2(), network1);

            }
            if (pTwt.getRatioTapChanger3() != null) {
                readRatioTapChanger(twt.getLeg3(), pTwt.getRatioTapChanger3(), network1);

            }
            if (pTwt.getCurrentLimits1() != null) {
                readCurrentLimits(twt.getLeg1()::newCurrentLimits, pTwt.getCurrentLimits1());
            }
            if (pTwt.getCurrentLimits2() != null) {
                readCurrentLimits(twt.getLeg2()::newCurrentLimits, pTwt.getCurrentLimits2());
            }
            readProperties(twt, pTwt.getProperty());
        });

    }

    private static void readRatioTapChanger(ThreeWindingsTransformer.Leg twl, ARatioTapChanger prtc, Network network) {
        RatioTapChangerAdder rtcAdder = twl.newRatioTapChanger();
        rtcAdder.setLowTapPosition(prtc.getLowTapPosition());
        rtcAdder.setTapPosition(prtc.getTapPosition());
        rtcAdder.setLoadTapChangingCapabilities(prtc.getLoadTapChangingCapabilities());
        if (prtc.getTargetDeadband() != null) {
            rtcAdder.setTargetDeadband(prtc.getTargetDeadband());
        }
        if (prtc.getTargetV() != null) {
            rtcAdder.setTargetV(prtc.getTargetV());
        }
        if (prtc.getLoadTapChangingCapabilities()) {
            rtcAdder.setRegulating(prtc.getRegulating());
        }

        if (prtc.getTerminalRef() != null) {
            rtcAdder.setRegulationTerminal(readTerminalRef(prtc, network));
        }

        List<ARatioTapChangerStep> psteps = prtc.getStep();
        psteps.forEach(pstep -> {
            rtcAdder.beginStep()
                    .setR(pstep.getR())
                    .setX(pstep.getX())
                    .setG(pstep.getG())
                    .setB(pstep.getB())
                    .setRho(pstep.getRho())
                    .endStep();
        });
        rtcAdder.add();
    }

    private static void readRatioTapChanger(TwoWindingsTransformer twt, ARatioTapChanger prtc, Network network1) {
        RatioTapChangerAdder rtcAdder = twt.newRatioTapChanger();
        rtcAdder.setLowTapPosition(prtc.getLowTapPosition());
        rtcAdder.setTapPosition(prtc.getTapPosition());
        rtcAdder.setLoadTapChangingCapabilities(prtc.getLoadTapChangingCapabilities());
        if (prtc.getTargetDeadband() != null) {
            rtcAdder.setTargetDeadband(prtc.getTargetDeadband());
        }
        if (prtc.getTargetV() != null) {
            rtcAdder.setTargetV(prtc.getTargetV());
        }
        if (prtc.getLoadTapChangingCapabilities()) {
            rtcAdder.setRegulating(prtc.getRegulating());
        }

        if (prtc.getTerminalRef() != null) {
            rtcAdder.setRegulationTerminal(readTerminalRef(prtc, network1));
        }

        List<ARatioTapChangerStep> psteps = prtc.getStep();
        psteps.forEach(pstep -> {
            rtcAdder.beginStep()
                    .setR(pstep.getR())
                    .setX(pstep.getX())
                    .setG(pstep.getG())
                    .setB(pstep.getB())
                    .setRho(pstep.getRho())
                    .endStep();
        });
        rtcAdder.add();
    }

    private static void readPhaseTapChanger(TwoWindingsTransformer twt, APhaseTapChanger ptc, Network network) {
        PhaseTapChangerAdder ptcAdder = twt.newPhaseTapChanger();
        ptcAdder.setTapPosition(ptc.getTapPosition());
        ptcAdder.setLowTapPosition(ptc.getLowTapPosition());
        ptcAdder.setRegulationMode(PhaseTapChanger.RegulationMode.valueOf(ptc.getRegulationMode()));
        if (ptc.getRegulationValue() != null) {
            ptcAdder.setRegulationValue(ptc.getRegulationValue());
        }
        if (ptc.getTargetDeadband() != null) {
            ptcAdder.setTargetDeadband(ptc.getTargetDeadband());
        }
        if (ptc.getRegulating() != null) {
            ptcAdder.setRegulating(ptc.getRegulating());
        } else {
            ptcAdder.setRegulating(false);
        }
        if (ptc.getTerminalRef() != null) {
            ptcAdder.setRegulationTerminal(readTerminalRef(ptc, network));
        }

        List<APhaseTapChangerStep> psteps = ptc.getStep();
        psteps.forEach(pstep -> {
            ptcAdder.beginStep()
                    .setR(pstep.getR())
                    .setX(pstep.getX())
                    .setG(pstep.getG())
                    .setB(pstep.getB())
                    .setRho(pstep.getRho())
                    .setAlpha(pstep.getAlpha())
                    .endStep();
        });
        ptcAdder.add();
    }

    private static Terminal readTerminalRef(ARatioTapChanger prtc, Network network1) {
        String id = prtc.getTerminalRef().getId();
        String pSide = prtc.getTerminalRef().getSide();
        Identifiable identifiable = network1.getIdentifiable(id);
        if (identifiable instanceof Injection) {
            return ((Injection) identifiable).getTerminal();
        } else if (identifiable instanceof Branch) {
            return pSide.equals(Branch.Side.ONE.toString()) ? ((Branch) identifiable).getTerminal1()
                    : ((Branch) identifiable).getTerminal2();
        } else if (identifiable instanceof ThreeWindingsTransformer) {
            ThreeWindingsTransformer twt = (ThreeWindingsTransformer) identifiable;
            return twt.getTerminal(ThreeWindingsTransformer.Side.valueOf(pSide));
        } else {
            throw new AssertionError("Unexpected Identifiable instance: " + identifiable.getClass());
        }
    }

    private static Terminal readTerminalRef(APhaseTapChanger ptc, Network network) {
        String id = ptc.getTerminalRef().getId();
        Identifiable identifiable = network.getIdentifiable(id);
        if (identifiable instanceof Injection) {
            return ((Injection) identifiable).getTerminal();
        } else if (identifiable instanceof Branch) {
            return Branch.Side.ONE.equals(Branch.Side.valueOf(ptc.getTerminalRef().getSide())) ? ((Branch) identifiable).getTerminal1()
                    : ((Branch) identifiable).getTerminal2();
        } else if (identifiable instanceof ThreeWindingsTransformer) {
            ThreeWindingsTransformer twt = (ThreeWindingsTransformer) identifiable;
            return twt.getTerminal(ThreeWindingsTransformer.Side.valueOf(ptc.getTerminalRef().getSide()));
        } else {
            throw new AssertionError("Unexpected Identifiable instance: " + identifiable.getClass());
        }
    }

    private static void readNodeOrBus(TwoWindingsTransformerAdder tAdder, ATwoWindingsTransformer pTwt) {
        if (pTwt.getBus1() != null) {
            tAdder.setBus1(pTwt.getBus1());
        }
        if (pTwt.getConnectableBus1() != null) {
            tAdder.setConnectableBus1(pTwt.getConnectableBus1());
        }
        if (pTwt.getNode1() != null) {
            tAdder.setNode1(pTwt.getNode1());
        }
        tAdder.setVoltageLevel1(pTwt.getVoltageLevelId1());
        if (pTwt.getBus2() != null) {
            tAdder.setBus2(pTwt.getBus2());
        }
        if (pTwt.getConnectableBus2() != null) {
            tAdder.setConnectableBus2(pTwt.getConnectableBus2());
        }
        if (pTwt.getNode2() != null) {
            tAdder.setNode2(pTwt.getNode2());
        }
        tAdder.setVoltageLevel2(pTwt.getVoltageLevelId2());
    }

    protected static void readNodeOrBus(ThreeWindingsTransformerAdder.LegAdder adder1, ThreeWindingsTransformerAdder.LegAdder adder2, ThreeWindingsTransformerAdder.LegAdder adder3, AThreeWindingsTransformer aTwt) {
        if (aTwt.getBus1() != null) {
            adder1.setBus(aTwt.getBus1());
        }
        if (aTwt.getConnectableBus1() != null) {
            adder1.setConnectableBus(aTwt.getConnectableBus1());
        }
        if (aTwt.getNode1() != null) {
            adder1.setNode(aTwt.getNode1());
        }
        adder1.setVoltageLevel(aTwt.getVoltageLevelId1());

        if (aTwt.getBus2() != null) {
            adder2.setBus(aTwt.getBus2());
        }
        if (aTwt.getConnectableBus2() != null) {
            adder2.setConnectableBus(aTwt.getConnectableBus2());
        }
        if (aTwt.getNode2() != null) {
            adder2.setNode(aTwt.getNode2());
        }
        adder2.setVoltageLevel(aTwt.getVoltageLevelId2());

        if (aTwt.getBus3() != null) {
            adder3.setBus(aTwt.getBus3());
        }
        if (aTwt.getConnectableBus3() != null) {
            adder3.setConnectableBus(aTwt.getConnectableBus3());
        }
        if (aTwt.getNode3() != null) {
            adder3.setNode(aTwt.getNode3());
        }
        adder3.setVoltageLevel(aTwt.getVoltageLevelId3());
    }

    private static void readLoads(VoltageLevel vl, AVoltageLevel avl, TopologyKind topologyKind) {
        List<ALoad> aloads = avl.getLoad();
        aloads.forEach(aload -> {
            LoadAdder adder = vl.newLoad();
            adder.setId(aload.getId());
            adder.setName(aload.getName());
            adder.setP0(aload.getP0());
            adder.setQ0(aload.getQ0());
            adder.setLoadType(LoadType.valueOf(aload.getLoadType()));

            adder.setBus(aload.getBus());
            adder.setConnectableBus(aload.getConnectableBus());
            if (aload.getNode() != null) {
                adder.setNode(aload.getNode());
            }

            Load newLoad = adder.add();

            readProperties(newLoad, aload.getProperty());

            if (aload.getP() != null) {
                newLoad.getTerminal().setP(aload.getP());
            }
            if (aload.getQ() != null) {
                newLoad.getTerminal().setQ(aload.getQ());
            }
        });

    }

    private static void readGenerators(VoltageLevel vl, AVoltageLevel avl) {
        List<AGenerator> agenerators = avl.getGenerator();
        agenerators.forEach(pgen -> {
            GeneratorAdder genAdder = vl.newGenerator();
            genAdder.setId(pgen.getId());
            genAdder.setName(pgen.getName());

            genAdder.setEnergySource(EnergySource.valueOf(pgen.getEnergySource()));
            genAdder.setMinP(pgen.getMinP());
            genAdder.setMaxP(pgen.getMaxP());
            genAdder.setTargetP(pgen.getTargetP());
            genAdder.setVoltageRegulatorOn(pgen.getVoltageRegulatorOn());
            genAdder.setBus(pgen.getBus());
            genAdder.setConnectableBus(pgen.getConnectableBus());
            if (pgen.getNode() != null) {
                genAdder.setNode(pgen.getNode());
            }
            genAdder.setTargetV(pgen.getTargetV());
            if (pgen.getTargetQ() != null) {
                genAdder.setTargetQ(pgen.getTargetQ());
            }

            Generator newGen = genAdder.add();

            if (pgen.getMinMaxReactiveLimits() != null) {
                ReactiveLimitsAvro.INSTANCE.readMinMaxReactiveLimits(newGen, pgen.getMinMaxReactiveLimits());
            }
            if (pgen.getReactiveCapabilityCurve() != null) {
                ReactiveLimitsAvro.INSTANCE.readReactiveCapabilityCurve(newGen, pgen.getReactiveCapabilityCurve());
            }

            if (pgen.getP() != null) {
                newGen.getTerminal().setP(pgen.getP());
            }
            if (pgen.getQ() != null) {
                newGen.getTerminal().setQ(pgen.getQ());
            }

            readProperties(newGen, pgen.getProperty());

        });
    }

    private static List<Property> writeProperties(Identifiable<?> identifiable) {
        List<Property> pList = new ArrayList<>();
        if (identifiable.hasProperty()) {
            for (String name : identifiable.getPropertyNames()) {
                String value = identifiable.getProperty(name);
                Property.Builder propertyBuilder = Property.newBuilder();
                propertyBuilder.setName(name);
                propertyBuilder.setValue(value);
                pList.add(propertyBuilder.build());
            }
        }
        return pList;
    }

    private static void readProperties(Identifiable<?> identifiable, List<Property> properties) {
        properties.forEach(property -> {
            identifiable.setProperty(property.getName(), property.getValue());
        });
    }

    private static void writeHvdcLines(Network network, ANetwork.Builder nBuilder) {
        nBuilder.setHvdcLine(network.getHvdcLineStream().map(IidmAvro::writeHvdcLine).collect(Collectors.toList()));
    }

    private static AHvdcLine writeHvdcLine(HvdcLine line) {
        AHvdcLine.Builder builder = AHvdcLine.newBuilder();
        builder.setId(line.getId());
        if (line.getName() != null) {
            builder.setName(line.getName());
        }
        builder.setConvertersMode(line.getConvertersMode().toString());
        builder.setNominalV(line.getNominalV());
        builder.setActivePowerSetpoint(line.getActivePowerSetpoint());
        builder.setMaxP(line.getMaxP());
        builder.setR(line.getR());
        builder.setConverterStation1(line.getConverterStation1().getId());
        builder.setConverterStation2(line.getConverterStation2().getId());
        builder.setProperty(writeProperties(line));
        return builder.build();
    }

    private static void readHvdcLines(ANetwork aNetwork, Network network) {
        List<AHvdcLine> lines = aNetwork.getHvdcLine();
        lines.forEach(pline -> {
            HvdcLineAdder adder = network.newHvdcLine();
            adder.setId(pline.getId());
            if (pline.getName() != null) {
                adder.setName(pline.getName());
            }
            adder.setConvertersMode(HvdcLine.ConvertersMode.valueOf(pline.getConvertersMode()));
            adder.setNominalV(pline.getNominalV());
            adder.setActivePowerSetpoint(pline.getActivePowerSetpoint());
            adder.setMaxP(pline.getMaxP());
            adder.setR(pline.getR());
            adder.setConverterStationId1(pline.getConverterStation1());
            adder.setConverterStationId2(pline.getConverterStation2());

            HvdcLine hvdcLine = adder.add();
            readProperties(hvdcLine, pline.getProperty());
        });
    }
}
