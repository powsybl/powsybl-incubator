/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.protobuf;

import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.iidm.import_.ImportOptions;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.protobuf.proto.Iidm;
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

import static com.powsybl.iidm.protobuf.IidmProtobufUtils.*;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.eu>
 */
public final class IidmProtobuf {

    private static final Logger LOGGER = LoggerFactory.getLogger(IidmProtobuf.class);

    private IidmProtobuf() {
    }

    public static void write(Network network, DataSource dataSource, String dataSourceExt) throws IOException {
        try (OutputStream osb = dataSource.newOutputStream("", dataSourceExt, false);
             BufferedOutputStream bosb = new BufferedOutputStream(osb)) {

            Iidm.Network.Builder networkBuilder = Iidm.Network.newBuilder();
            networkBuilder.setId(network.getId())
                    .setCaseDate(network.getCaseDate().toString())
                    .setForecastDistance(network.getForecastDistance())
                    .setSourceFormat(network.getSourceFormat());

            writeSubstations(network, networkBuilder);
            writeLines(network, networkBuilder);
            writeHvdcLines(network, networkBuilder);
            Iidm.Network pNetwork = networkBuilder.build();
            pNetwork.writeTo(bosb);
        }
    }

    private static void writeHvdcLines(Network network, Iidm.Network.Builder networkBuilder) {
        network.getHvdcLineStream().forEach(line -> {
            Iidm.HvdcLine.Builder builder = Iidm.HvdcLine.newBuilder();
            builder.setId(line.getId());
            if (line.getName() != null) {
                builder.setName(line.getName());
            }
            builder.setConvertersMode(iidmToProtoConvertersMode(line.getConvertersMode()));
            builder.setNominalV(line.getNominalV());
            builder.setActivePowerSetpoint(line.getActivePowerSetpoint());
            builder.setMaxP(line.getMaxP());
            builder.setR(line.getR());
            builder.setConverterStation1(line.getConverterStation1().getId());
            builder.setConverterStation2(line.getConverterStation2().getId());
            builder.addAllProperty(writeProperties(line));

            networkBuilder.addHvdcLine(builder);
        });
    }

    private static void writeSubstations(Network network, Iidm.Network.Builder networkBuilder) {
        network.getSubstationStream().forEach(sub -> {
            Iidm.Substation.Builder sBuilder = Iidm.Substation.newBuilder();
            sBuilder.setId(sub.getId());
            sBuilder.setName(sub.getName());
            Optional<Country> country = sub.getCountry();
            if (country.isPresent()) {
                sBuilder.setCountry(country.get().toString());
            }
            String tso = sub.getTso();
            if (tso != null) {
                sBuilder.setTso(tso);
            }
            sBuilder.addAllGeographicalTags(sub.getGeographicalTags());

            sub.getVoltageLevelStream().forEach(vl -> {
                writeVoltageLevel(vl, sBuilder);
            });

            sub.getTwoWindingsTransformerStream().forEach(twt -> {
                writeTwowindingsTransformer(twt, sBuilder);
            });

            sub.getThreeWindingsTransformerStream().forEach(twt -> {
                writeThreeWindingsTransformer(twt, sBuilder);
            });

            sBuilder.addAllProperty(writeProperties(sub));
            networkBuilder.addSubstation(sBuilder.build());
        });
    }

    private static void writeVoltageLevel(VoltageLevel vl, Iidm.Substation.Builder sBuilder) {
        TopologyKind topologyKind = vl.getTopologyKind();
        Iidm.VoltageLevel.Builder vBuilder = Iidm.VoltageLevel.newBuilder();
        vBuilder.setId(vl.getId());
        vBuilder.setName(vl.getName());
        vBuilder.setTopologyKind(IidmProtobufUtils.iidmToProtoTopologyKind(topologyKind));
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
                Iidm.BusBreakerTopology.Builder bbtBuilder = Iidm.BusBreakerTopology.newBuilder();
                for (Bus b : vl.getBusBreakerView().getBuses()) {
                    Iidm.Bus.Builder bBuilder = Iidm.Bus.newBuilder();
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
                    bbtBuilder.addBus(bBuilder);
                }
                for (Switch sw : vl.getBusBreakerView().getSwitches()) {
                    Bus b1 = vl.getBusBreakerView().getBus1(sw.getId());
                    Bus b2 = vl.getBusBreakerView().getBus2(sw.getId());

                    Iidm.SwitchBus.Builder bswBuilder = Iidm.SwitchBus.newBuilder();
                    bswBuilder.setId(sw.getId());
                    if (sw.getName() != null) {
                        bswBuilder.setName(sw.getName());
                    }
                    bswBuilder.setKind(iidmToProtoSwitchKind(sw.getKind()));
                    bswBuilder.setOpen(sw.isOpen());
                    bswBuilder.setRetained(sw.isRetained());
                    bswBuilder.setBus1(b1.getId());
                    bswBuilder.setBus2(b2.getId());
                    bswBuilder.setFictitious(sw.isFictitious());
                    bswBuilder.addAllProperty(writeProperties(sw));
                    bbtBuilder.addSwitch(bswBuilder);
                }

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

        vBuilder.addAllProperty(writeProperties(vl));
        sBuilder.addVoltageLevel(vBuilder);
    }

    private static void writeLines(Network n, Iidm.Network.Builder networkBuilder) {
        for (Line l : n.getLines()) {
            if (l.isTieLine()) {
                throw new UnsupportedOperationException("tie lines not yet supported, tie line: " + l);
            } else {
                Iidm.Line.Builder lBuilder = Iidm.Line.newBuilder();
                lBuilder.setId(l.getId());
                if (l.getName() != null) {
                    lBuilder.setName(l.getName());
                }

                lBuilder.setR(l.getR());
                lBuilder.setX(l.getX());
                lBuilder.setB1(l.getB1());
                lBuilder.setB2(l.getB2());
                lBuilder.setG1(l.getG1());
                lBuilder.setG2(l.getG2());

                writeNodeOrBus(1, l.getTerminal1(), lBuilder);
                writeNodeOrBus(2, l.getTerminal2(), lBuilder);

                if (!Double.isNaN(l.getTerminal1().getP())) {
                    lBuilder.setP1(l.getTerminal1().getP());
                }
                if (!Double.isNaN(l.getTerminal1().getQ())) {
                    lBuilder.setQ1(l.getTerminal1().getQ());
                }

                if (!Double.isNaN(l.getTerminal2().getP())) {
                    lBuilder.setP2(l.getTerminal2().getP());
                }
                if (!Double.isNaN(l.getTerminal2().getQ())) {
                    lBuilder.setQ2(l.getTerminal2().getQ());
                }

                if (l.getCurrentLimits1() != null) {
                    writeCurrentLimits(1, l.getCurrentLimits1(), lBuilder);
                }
                if (l.getCurrentLimits2() != null) {
                    writeCurrentLimits(2, l.getCurrentLimits2(), lBuilder);
                }
                networkBuilder.addLine(lBuilder);
            }
        }
    }

    private static void writeNode(Integer index, Terminal t, Iidm.Line.Builder builder) {
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

    private static void writeBus(Integer index, Bus bus, Bus connectableBus, Iidm.Line.Builder builder) {
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

    private static void writeVoltageLevelId(Integer index, Terminal t, Iidm.Line.Builder builder) {
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

    private static void writeNodeOrBus(Integer index, Terminal t, Iidm.Line.Builder builder) {
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

    private static void writeNode(Integer index, Terminal t, Iidm.TwoWindingsTransformer.Builder builder) {
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

    private static void writeNode(Integer index, Terminal t, Iidm.ThreeWindingsTransformer.Builder builder) {
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

    private static void writeBus(Integer index, Bus bus, Bus connectableBus, Iidm.TwoWindingsTransformer.Builder builder) {
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

    private static void writeBus(Integer index, Bus bus, Bus connectableBus, Iidm.ThreeWindingsTransformer.Builder builder) {
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

    private static void writeVoltageLevelId(Integer index, Terminal t, Iidm.TwoWindingsTransformer.Builder builder) {
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

    private static void writeVoltageLevelId(Integer index, Terminal t, Iidm.ThreeWindingsTransformer.Builder builder) {
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

    private static void writeNodeOrBus(Integer index, Terminal t, Iidm.TwoWindingsTransformer.Builder builder) {
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

    private static void writeNodeOrBus(Integer index, Terminal t, Iidm.ThreeWindingsTransformer.Builder builder) {
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

    private static void writeTwowindingsTransformer(TwoWindingsTransformer twt, Iidm.Substation.Builder sBuilder) {
        Iidm.TwoWindingsTransformer.Builder tBuilder = Iidm.TwoWindingsTransformer.newBuilder();
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
        tBuilder.addAllProperty(writeProperties(twt));
        Iidm.TwoWindingsTransformer pTwt = tBuilder.build();
        sBuilder.addTwoWindingsTransformer(pTwt);
    }

    private static void writeThreeWindingsTransformer(ThreeWindingsTransformer twt, Iidm.Substation.Builder sBuilder) {
        Iidm.ThreeWindingsTransformer.Builder tBuilder = Iidm.ThreeWindingsTransformer.newBuilder();
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

        tBuilder.addAllProperty(writeProperties(twt));
        sBuilder.addThreeWindingsTransformer(tBuilder.build());
    }

    private static void writeCurrentLimits(int i, CurrentLimits limits, Iidm.TwoWindingsTransformer.Builder tBuilder) {
        Iidm.CurrentLimit.Builder limitBuilder = Iidm.CurrentLimit.newBuilder();
        if (!Double.isNaN(limits.getPermanentLimit())
                || !limits.getTemporaryLimits().isEmpty()) {
            limitBuilder.setPermanentLimit(limits.getPermanentLimit());
            for (CurrentLimits.TemporaryLimit tl : limits.getTemporaryLimits()) {
                Iidm.TemporaryLimitType.Builder tempBuilder = Iidm.TemporaryLimitType.newBuilder();
                tempBuilder.setName(tl.getName());
                tempBuilder.setAcceptableDuration(tl.getAcceptableDuration());
                tempBuilder.setValue(tl.getValue());
                tempBuilder.setFictitious(tl.isFictitious());
                limitBuilder.addTemporaryLimit(tempBuilder);
            }
        }
        switch (i) {
            case 1:
                tBuilder.setCurrentLimits1(limitBuilder);
                break;
            case 2:
                tBuilder.setCurrentLimits2(limitBuilder);
                break;
            default:
                throw new AssertionError("Unexpected value: " + i);
        }
    }

    private static void writeCurrentLimits(int i, CurrentLimits limits, Iidm.ThreeWindingsTransformer.Builder tBuilder) {
        Iidm.CurrentLimit.Builder limitBuilder = Iidm.CurrentLimit.newBuilder();
        if (!Double.isNaN(limits.getPermanentLimit())
                || !limits.getTemporaryLimits().isEmpty()) {
            limitBuilder.setPermanentLimit(limits.getPermanentLimit());
            for (CurrentLimits.TemporaryLimit tl : limits.getTemporaryLimits()) {
                Iidm.TemporaryLimitType.Builder tempBuilder = Iidm.TemporaryLimitType.newBuilder();
                tempBuilder.setName(tl.getName());
                tempBuilder.setAcceptableDuration(tl.getAcceptableDuration());
                tempBuilder.setValue(tl.getValue());
                tempBuilder.setFictitious(tl.isFictitious());
                limitBuilder.addTemporaryLimit(tempBuilder);
            }
        }
        switch (i) {
            case 1:
                tBuilder.setCurrentLimits1(limitBuilder);
                break;
            case 2:
                tBuilder.setCurrentLimits2(limitBuilder);
                break;
            case 3:
                tBuilder.setCurrentLimits3(limitBuilder);
                break;
            default:
                throw new AssertionError("Unexpected value: " + i);
        }
    }

    private static void writeCurrentLimits(int i, CurrentLimits limits, Iidm.Line.Builder builder) {
        Iidm.CurrentLimit.Builder limitBuilder = Iidm.CurrentLimit.newBuilder();
        if (!Double.isNaN(limits.getPermanentLimit())
                || !limits.getTemporaryLimits().isEmpty()) {
            limitBuilder.setPermanentLimit(limits.getPermanentLimit());
            for (CurrentLimits.TemporaryLimit tl : limits.getTemporaryLimits()) {
                Iidm.TemporaryLimitType.Builder tempBuilder = Iidm.TemporaryLimitType.newBuilder();
                tempBuilder.setName(tl.getName());
                tempBuilder.setAcceptableDuration(tl.getAcceptableDuration());
                tempBuilder.setValue(tl.getValue());
                tempBuilder.setFictitious(tl.isFictitious());
                limitBuilder.addTemporaryLimit(tempBuilder);
            }
        }
        switch (i) {
            case 1:
                builder.setCurrentLimits1(limitBuilder);
                break;
            case 2:
                builder.setCurrentLimits2(limitBuilder);
                break;
            default:
                throw new AssertionError("Unexpected value: " + i);
        }
    }

    private static void writeCurrentLimits(CurrentLimits limits, Iidm.DanglingLine.Builder builder) {
        Iidm.CurrentLimit.Builder limitBuilder = Iidm.CurrentLimit.newBuilder();
        if (!Double.isNaN(limits.getPermanentLimit())
                || !limits.getTemporaryLimits().isEmpty()) {
            limitBuilder.setPermanentLimit(limits.getPermanentLimit());
            for (CurrentLimits.TemporaryLimit tl : limits.getTemporaryLimits()) {
                Iidm.TemporaryLimitType.Builder tempBuilder = Iidm.TemporaryLimitType.newBuilder();
                tempBuilder.setName(tl.getName());
                tempBuilder.setAcceptableDuration(tl.getAcceptableDuration());
                tempBuilder.setValue(tl.getValue());
                tempBuilder.setFictitious(tl.isFictitious());
                limitBuilder.addTemporaryLimit(tempBuilder);
            }
        }
        builder.setCurrentLimits(limitBuilder);
    }

    private static void writeRatioTapChanger(RatioTapChanger rtc, Iidm.TwoWindingsTransformer.Builder tBuilder) {
        Iidm.RatioTapChanger.Builder rtcBuilder = Iidm.RatioTapChanger.newBuilder();

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

        List<Iidm.RatioTapChangerStep> ptrcSteps = new ArrayList<>();
        for (int p = rtc.getLowTapPosition(); p <= rtc.getHighTapPosition(); p++) {
            RatioTapChangerStep rtcs = rtc.getStep(p);
            Iidm.RatioTapChangerStep.Builder stepBuilder = Iidm.RatioTapChangerStep.newBuilder();
            stepBuilder.setB(rtcs.getB());
            stepBuilder.setG(rtcs.getG());
            stepBuilder.setR(rtcs.getR());
            stepBuilder.setRho(rtcs.getRho());
            stepBuilder.setX(rtcs.getX());
            ptrcSteps.add(stepBuilder.build());
        }
        rtcBuilder.addAllStep(ptrcSteps);
        tBuilder.setRatioTapChanger(rtcBuilder.build());
    }

    private static Iidm.RatioTapChanger writeRatioTapChanger(RatioTapChanger rtc, Iidm.ThreeWindingsTransformer.Builder tBuilder) {
        Iidm.RatioTapChanger.Builder rtcBuilder = Iidm.RatioTapChanger.newBuilder();

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

        List<Iidm.RatioTapChangerStep> ptrcSteps = new ArrayList<>();
        for (int p = rtc.getLowTapPosition(); p <= rtc.getHighTapPosition(); p++) {
            RatioTapChangerStep rtcs = rtc.getStep(p);
            Iidm.RatioTapChangerStep.Builder stepBuilder = Iidm.RatioTapChangerStep.newBuilder();
            stepBuilder.setB(rtcs.getB());
            stepBuilder.setG(rtcs.getG());
            stepBuilder.setR(rtcs.getR());
            stepBuilder.setRho(rtcs.getRho());
            stepBuilder.setX(rtcs.getX());
            ptrcSteps.add(stepBuilder.build());
        }
        rtcBuilder.addAllStep(ptrcSteps);
        return rtcBuilder.build();
    }

    private static void writePhaseTapChanger(PhaseTapChanger ptc, Iidm.TwoWindingsTransformer.Builder tBuilder) {
        Iidm.PhaseTapChanger.Builder ptcBuilder = Iidm.PhaseTapChanger.newBuilder();

        ptcBuilder.setTapPosition(ptc.getTapPosition());
        ptcBuilder.setLowTapPosition(ptc.getLowTapPosition());
        ptcBuilder.setRegulationMode(iidmToProtoPhaseRegulationMode(ptc.getRegulationMode()));

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

        List<Iidm.PhaseTapChangerStep> pSteps = new ArrayList<>();
        for (int p = ptc.getLowTapPosition(); p <= ptc.getHighTapPosition(); p++) {
            PhaseTapChangerStep ptcs = ptc.getStep(p);
            Iidm.PhaseTapChangerStep.Builder pstepBuilder = Iidm.PhaseTapChangerStep.newBuilder();
            pstepBuilder.setR(ptcs.getR());
            pstepBuilder.setX(ptcs.getX());
            pstepBuilder.setG(ptcs.getG());
            pstepBuilder.setB(ptcs.getB());
            pstepBuilder.setRho(ptcs.getRho());
            pstepBuilder.setAlpha(ptcs.getAlpha());
            pSteps.add(pstepBuilder.build());
        }
        ptcBuilder.addAllStep(pSteps);
        tBuilder.setPhaseTapChanger(ptcBuilder.build());
    }

    private static Iidm.TerminalRef writeTerminalRef(Terminal terminal) {
        Iidm.TerminalRef.Builder termBuilder = Iidm.TerminalRef.newBuilder();
        Connectable c = terminal.getConnectable();
        termBuilder.setId(c.getId());
        if (c.getTerminals().size() > 1) {
            if (c instanceof Injection) {
                // nothing to do
            } else if (c instanceof Branch) {
                Branch branch = (Branch) c;
                termBuilder.setSide(iidmToProtoBranchSide(branch.getSide(terminal)));
            } else if (c instanceof ThreeWindingsTransformer) {
                ThreeWindingsTransformer twt = (ThreeWindingsTransformer) c;
                termBuilder.setSide(iidmToProtoThreeWindingTransformerSide(twt.getSide(terminal)));
            } else {
                throw new AssertionError("Unexpected Connectable instance: " + c.getClass());
            }
        }
        return termBuilder.build();

    }

    private static void writeGenerators(VoltageLevel vl, TopologyKind topologyKind, Iidm.VoltageLevel.Builder vBuilder) {
        vl.getGeneratorStream().forEach(gen -> {
            Iidm.Generator.Builder builder = Iidm.Generator.newBuilder();
            writeGenerator(builder, gen);
            addNodeOrBus(builder, topologyKind, vl, gen);
            builder.addAllProperty(writeProperties(gen));
            if (!Double.isNaN(gen.getTerminal().getP())) {
                builder.setP(gen.getTerminal().getP());
            }
            if (!Double.isNaN(gen.getTerminal().getQ())) {
                builder.setQ(gen.getTerminal().getQ());
            }

            vBuilder.addGenerator(builder);
        });
    }

    private static void writeLoads(VoltageLevel vl, TopologyKind topologyKind, Iidm.VoltageLevel.Builder vBuilder) {
        vl.getLoadStream().forEach(load -> {
            Iidm.Load.Builder loadBuilder = Iidm.Load.newBuilder();
            loadBuilder.setId(load.getId());
            if (load.getName() != null) {
                loadBuilder.setName(load.getName());
            }
            loadBuilder.setP0(load.getP0());
            loadBuilder.setQ0(load.getQ0());
            loadBuilder.setLoadType(iidmtoProtoLoadType(load.getLoadType()));
            addNodeOrBus(loadBuilder, topologyKind, vl, load);
            if (!Double.isNaN(load.getTerminal().getP())) {
                loadBuilder.setP(load.getTerminal().getP());
            }
            if (!Double.isNaN(load.getTerminal().getQ())) {
                loadBuilder.setQ(load.getTerminal().getQ());
            }
            loadBuilder.addAllProperty(writeProperties(load));

            vBuilder.addLoad(loadBuilder);
        });
    }

    private static void writeBatteries(VoltageLevel vl, TopologyKind topologyKind, Iidm.VoltageLevel.Builder vBuilder) {
        vl.getBatteryStream().forEach(b -> {
            Iidm.Battery.Builder builder = Iidm.Battery.newBuilder();
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
                    builder.setReactiveCapabilityCurve(ReactiveLimitsProto.INSTANCE.writeReactiveCababilityCurve(b));
                    break;
                case MIN_MAX:
                    builder.setMinMaxReactiveLimits(ReactiveLimitsProto.INSTANCE.writeMixMaxReactiveLimits(b));
                    break;
                default:
                    throw new AssertionError();
            }

            addNodeOrBus(builder, topologyKind, vl, b);

            if (!Double.isNaN(b.getTerminal().getP())) {
                builder.setP(b.getTerminal().getP());
            }
            if (!Double.isNaN(b.getTerminal().getQ())) {
                builder.setQ(b.getTerminal().getQ());
            }
            builder.addAllProperty(writeProperties(b));

            vBuilder.addBattery(builder);
        });
    }

    private static void writeShunts(VoltageLevel vl, TopologyKind topologyKind, Iidm.VoltageLevel.Builder vBuilder) {
        vl.getShuntCompensatorStream().forEach(s -> {
            Iidm.ShuntCompensator.Builder shBuilder = Iidm.ShuntCompensator.newBuilder();
            shBuilder.setId(s.getId());
            if (s.getName() != null) {
                shBuilder.setName(s.getName());
            }
            shBuilder.setBPerSection(s.getbPerSection());
            shBuilder.setCurrentSectionCount(s.getCurrentSectionCount());
            shBuilder.setMaximumSectionCount(s.getMaximumSectionCount());

            addNodeOrBus(shBuilder, topologyKind, vl, s);

            if (!Double.isNaN(s.getTerminal().getP())) {
                shBuilder.setP(s.getTerminal().getP());
            }
            if (!Double.isNaN(s.getTerminal().getQ())) {
                shBuilder.setQ(s.getTerminal().getQ());
            }

            shBuilder.addAllProperty(writeProperties(s));
            vBuilder.addShuntCompensator(shBuilder);
        });
    }

    private static void writeDanglingLines(VoltageLevel vl, TopologyKind topologyKind, Iidm.VoltageLevel.Builder vBuilder) {
        vl.getDanglingLineStream().forEach(dl -> {
            Iidm.DanglingLine.Builder dlBuilder = Iidm.DanglingLine.newBuilder();
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

            addNodeOrBus(dlBuilder, topologyKind, vl, dl);

            if (!Double.isNaN(dl.getTerminal().getP())) {
                dlBuilder.setP(dl.getTerminal().getP());
            }
            if (!Double.isNaN(dl.getTerminal().getQ())) {
                dlBuilder.setQ(dl.getTerminal().getQ());
            }

            dlBuilder.addAllProperty(writeProperties(dl));
            vBuilder.addDanglingLine(dlBuilder);
        });
    }

    private static void writeStaticVarCompensators(VoltageLevel vl, TopologyKind topologyKind, Iidm.VoltageLevel.Builder vBuilder) {
        vl.getStaticVarCompensatorStream().forEach(svc -> {
            Iidm.StaticVarCompensator.Builder pSvcBuilder = Iidm.StaticVarCompensator.newBuilder();
            pSvcBuilder.setId(svc.getId());
            if (svc.getName() != null) {
                pSvcBuilder.setName(svc.getName());
            }
            pSvcBuilder.setRegulationMode(iidmToProtoStaticVarRegulationMode(svc.getRegulationMode()));
            pSvcBuilder.setBMin(svc.getBmin());
            pSvcBuilder.setBMax(svc.getBmax());
            if (!Double.isNaN(pSvcBuilder.getReactivePowerSetPoint())) {
                pSvcBuilder.setReactivePowerSetPoint(svc.getReactivePowerSetPoint());
            }
            if (!Double.isNaN(pSvcBuilder.getVoltageSetPoint())) {
                pSvcBuilder.setVoltageSetPoint(svc.getVoltageSetPoint());
            }

            addNodeOrBus(pSvcBuilder, topologyKind, vl, svc);

            if (!Double.isNaN(svc.getTerminal().getP())) {
                pSvcBuilder.setP(svc.getTerminal().getP());
            }
            if (!Double.isNaN(svc.getTerminal().getQ())) {
                pSvcBuilder.setQ(svc.getTerminal().getQ());
            }

            pSvcBuilder.addAllProperty(writeProperties(svc));
            vBuilder.addStaticVarCompensator(pSvcBuilder);
        });
    }

    private static void writeVscConverterStations(VoltageLevel vl, TopologyKind topologyKind, Iidm.VoltageLevel.Builder vBuilder) {
        vl.getVscConverterStationStream().forEach(vsc -> {
            Iidm.VscConverterStation.Builder builder = Iidm.VscConverterStation.newBuilder();
            builder.setId(vsc.getId());
            if (vsc.getName() != null) {
                builder.setName(vsc.getName());
            }
            builder.setLossFactor(vsc.getLossFactor());
            builder.setVoltageRegulatorOn(vsc.isVoltageRegulatorOn());

            switch (vsc.getReactiveLimits().getKind()) {
                case CURVE:
                    builder.setReactiveCapabilityCurve(ReactiveLimitsProto.INSTANCE.writeReactiveCababilityCurve(vsc));
                    break;
                case MIN_MAX:
                    builder.setMinMaxReactiveLimits(ReactiveLimitsProto.INSTANCE.writeMixMaxReactiveLimits(vsc));
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

            addNodeOrBus(builder, topologyKind, vl, vsc);

            if (!Double.isNaN(vsc.getTerminal().getP())) {
                builder.setP(vsc.getTerminal().getP());
            }
            if (!Double.isNaN(vsc.getTerminal().getQ())) {
                builder.setQ(vsc.getTerminal().getQ());
            }

            builder.addAllProperty(writeProperties(vsc));

            vBuilder.addVscConverterStation(builder);
        });
    }

    private static void writeLccConverterStations(VoltageLevel vl, TopologyKind topologyKind, Iidm.VoltageLevel.Builder vBuilder) {
        vl.getLccConverterStationStream().forEach(lcc -> {
            Iidm.LccConverterStation.Builder builder = Iidm.LccConverterStation.newBuilder();
            builder.setId(lcc.getId());
            if (lcc.getName() != null) {
                builder.setName(lcc.getName());
            }
            builder.setLossFactor(lcc.getLossFactor());
            builder.setPowerFactor(lcc.getPowerFactor());

            addNodeOrBus(builder, topologyKind, vl, lcc);

            if (!Double.isNaN(lcc.getTerminal().getP())) {
                builder.setP(lcc.getTerminal().getP());
            }
            if (!Double.isNaN(lcc.getTerminal().getQ())) {
                builder.setQ(lcc.getTerminal().getQ());
            }

            builder.addAllProperty(writeProperties(lcc));

            vBuilder.addLccConverterStation(builder);

        });
    }

    private static List<Iidm.Property> writeProperties(Identifiable<?> identifiable) {
        List<Iidm.Property> pList = new ArrayList<>();
        if (identifiable.hasProperty()) {
            for (String name : identifiable.getPropertyNames()) {
                String value = identifiable.getProperty(name);
                Iidm.Property.Builder propertyBuilder = Iidm.Property.newBuilder();
                propertyBuilder.setName(name);
                propertyBuilder.setValue(value);
                pList.add(propertyBuilder.build());
            }
        }
        return pList;
    }

    private static void writeGenerator(Iidm.Generator.Builder genBuilder, Generator generator) {
        genBuilder.setId(generator.getId());
        if (generator.getName() != null) {
            genBuilder.setName(generator.getName());
        }
        genBuilder.setEnergySource(iidmToProtoEnergySource(generator.getEnergySource()));
        genBuilder.setMinP(generator.getMinP());
        genBuilder.setMaxP(generator.getMaxP());
        genBuilder.setTargetP(generator.getTargetP());
        genBuilder.setVoltageRegulatorOn(generator.isVoltageRegulatorOn());

        if (!Double.isNaN(generator.getRatedS())) {
            genBuilder.setRatedS(generator.getRatedS());
        }

        if (!Double.isNaN(generator.getTargetQ())) {
            genBuilder.setTargetQ(generator.getTargetQ());
        }

        if (!Double.isNaN(generator.getTargetV())) {
            genBuilder.setTargetV(generator.getTargetV());
        }

        if (!Double.isNaN(generator.getTerminal().getP())) {
            genBuilder.setP(generator.getTerminal().getP());
        }

        if (!Double.isNaN(generator.getTerminal().getQ())) {
            genBuilder.setQ(generator.getTerminal().getQ());
        }

        switch (generator.getReactiveLimits().getKind()) {
            case CURVE:
                genBuilder.setReactiveCapabilityCurve(ReactiveLimitsProto.INSTANCE.writeReactiveCababilityCurve(generator));
                break;

            case MIN_MAX:
                genBuilder.setMinMaxReactiveLimits(ReactiveLimitsProto.INSTANCE.writeMixMaxReactiveLimits(generator));
                break;

            default:
                throw new AssertionError();
        }
    }

    protected static void addNodeOrBus(Iidm.Load.Builder loadBuilder, TopologyKind topologyKind, VoltageLevel vl, Load load) {
        switch (topologyKind) {
            case NODE_BREAKER:
                loadBuilder.setNode(load.getTerminal().getNodeBreakerView().getNode());
                break;
            case BUS_BREAKER:
                loadBuilder.setBus(load.getTerminal().getBusBreakerView().getBus().getId());
                if (load.getTerminal().getBusBreakerView().getConnectableBus() != null) {
                    loadBuilder.setConnectableBus(load.getTerminal().getBusBreakerView().getConnectableBus().getId());
                }
                break;
            default:
                throw new AssertionError("Unexpected TopologyKind value: " + topologyKind);
        }
    }

    private static void addNodeOrBus(Iidm.ShuntCompensator.Builder shBuilder, TopologyKind topologyKind, VoltageLevel vl, ShuntCompensator s) {
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

    private static void addNodeOrBus(Iidm.DanglingLine.Builder dlBuilder, TopologyKind topologyKind, VoltageLevel vl, DanglingLine dl) {
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

    private static void addNodeOrBus(Iidm.StaticVarCompensator.Builder builder, TopologyKind topologyKind, VoltageLevel vl, StaticVarCompensator svc) {
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

    private static void addNodeOrBus(Iidm.VscConverterStation.Builder builder, TopologyKind topologyKind, VoltageLevel vl, VscConverterStation vsc) {
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

    private static void addNodeOrBus(Iidm.Battery.Builder builder, TopologyKind topologyKind, VoltageLevel vl, Battery b) {
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

    protected static void addNodeOrBus(Iidm.Generator.Builder generatorBuilder, TopologyKind topologyKind, VoltageLevel vl, Generator generator) {
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

    private static void addNodeOrBus(Iidm.LccConverterStation.Builder builder, TopologyKind topologyKind, VoltageLevel vl, LccConverterStation lcc) {
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

    public static Network read(ReadOnlyDataSource dataSource, NetworkFactory networkFactory, ImportOptions options, String dataSourceExt) throws IOException {
        Objects.requireNonNull(dataSource);
        Network network;
        try (InputStream isb = dataSource.newInputStream(null, dataSourceExt)) {
            Iidm.Network pNetwork = Iidm.Network.parseFrom(isb);

            network = networkFactory.createNetwork(pNetwork.getId(), pNetwork.getSourceFormat());
            network.setCaseDate(DateTime.parse(pNetwork.getCaseDate()));
            network.setForecastDistance(pNetwork.getForecastDistance());

            List<Iidm.Substation> pSubs = pNetwork.getSubstationList();
            pSubs.stream().forEach(psub -> {
                SubstationAdder subsAdder = network.newSubstation();
                Country country = Optional.ofNullable(Country.valueOf(psub.getCountry())).orElse(null);
                subsAdder.setId(psub.getId());
                subsAdder.setName(psub.getName());
                if (psub.hasTso()) {
                    subsAdder.setTso(psub.getTso());
                }
                subsAdder.setGeographicalTags(psub.getGeographicalTagsList().stream().toArray(String[]::new));
                if (country != null) {
                    subsAdder.setCountry(country);
                }
                Substation sub = subsAdder.add();
                readProperties(sub, psub.getPropertyList());

                List<Iidm.VoltageLevel> pvls = psub.getVoltageLevelList();
                pvls.forEach(pvl -> {
                    VoltageLevelAdder vlAdder = sub.newVoltageLevel();
                    vlAdder.setId(pvl.getId());
                    vlAdder.setName(pvl.getName());
                    vlAdder.setNominalV(pvl.getNominalV());
                    if (pvl.hasHighVoltageLimit()) {
                        vlAdder.setHighVoltageLimit(pvl.getHighVoltageLimit());
                    }
                    if (pvl.hasLowVoltageLimit()) {
                        vlAdder.setLowVoltageLimit(pvl.getLowVoltageLimit());
                    }
                    TopologyKind topologyKind = protoToIidmTopologyKind(pvl.getTopologyKind());

                    vlAdder.setTopologyKind(topologyKind);

                    VoltageLevel newVoltageLevel = vlAdder.add();
                    switch (topologyKind) {
                        case NODE_BREAKER:
                            break;

                        case BUS_BREAKER:
                            List<Iidm.Bus> bList = pvl.getBusBreakerTopology().getBusList();
                            bList.forEach(b -> {
                                BusAdder busAdder = newVoltageLevel.getBusBreakerView().newBus();
                                busAdder.setId(b.getId());
                                if (b.hasName()) {
                                    busAdder.setName(b.getName());
                                }
                                Bus newBus = busAdder.add();
                                if (b.hasAngle()) {
                                    newBus.setAngle(b.getAngle());
                                }
                                if (b.hasV()) {
                                    newBus.setV(b.getV());
                                }

                            });
                            List<Iidm.SwitchBus> swList = pvl.getBusBreakerTopology().getSwitchList();
                            swList.forEach(psw -> {
                                VoltageLevel.BusBreakerView.SwitchAdder bswAdder = newVoltageLevel.getBusBreakerView().newSwitch();
                                bswAdder.setId(psw.getId());
                                if (psw.hasName()) {
                                    bswAdder.setName(psw.getName());
                                }
                                bswAdder.setOpen(psw.getOpen());
                                bswAdder.setBus1(psw.getBus1());
                                bswAdder.setBus2(psw.getBus2());
                                if (psw.hasFictitious()) {
                                    bswAdder.setFictitious(psw.getFictitious());
                                }
                                Switch newSwitch = bswAdder.add();
                                readProperties(newSwitch, psw.getPropertyList());
                            });
                            break;
                        default:
                            throw new AssertionError("Unexpected topologyKind value: " + topologyKind);
                    }

                    readBatteries(newVoltageLevel, pvl, topologyKind);
                    readLoads(newVoltageLevel, pvl, topologyKind);
                    readGenerators(newVoltageLevel, pvl);
                    readShunts(newVoltageLevel, pvl, topologyKind);
                    readDanglingLines(newVoltageLevel, pvl, topologyKind);
                    readStaticVarCompensators(newVoltageLevel, pvl, topologyKind);
                    readVscConverterStations(newVoltageLevel, pvl, topologyKind);
                    readLccConverterStations(newVoltageLevel, pvl, topologyKind);

                    readProperties(newVoltageLevel, pvl.getPropertyList());
                });

                readTwoWindingTransformers(psub, sub, network);
                readThreeWindingTransformers(psub, sub, network);

            });

            readLines(pNetwork, network);
            readHvdcLines(pNetwork, network);

            return network;
        }
    }

    private static void readProperties(Identifiable<?> identifiable, List<Iidm.Property> properties) {
        properties.forEach(property -> {
            identifiable.setProperty(property.getName(), property.getValue());
        });
    }

    private static void readLines(Iidm.Network pNetwork, Network network) {
        List<Iidm.Line> lines = pNetwork.getLineList();
        lines.forEach(pl -> {
            LineAdder lAdder = network.newLine();
            lAdder.setId(pl.getId());
            if (pl.hasName()) {
                lAdder.setName(pl.getName());
            }
            lAdder.setR(pl.getR());
            lAdder.setX(pl.getX());
            lAdder.setB1(pl.getB1());
            lAdder.setB2(pl.getB2());
            lAdder.setG1(pl.getG1());
            lAdder.setG2(pl.getG2());

            readNodeOrBus(lAdder, pl);

            Line line = lAdder.add();
            if (pl.hasP1()) {
                line.getTerminal1().setP(pl.getP1());
            }

            if (pl.hasQ1()) {
                line.getTerminal1().setQ(pl.getQ1());
            }

            if (pl.hasP2()) {
                line.getTerminal2().setP(pl.getP2());
            }
            if (pl.hasQ2()) {
                line.getTerminal2().setQ(pl.getQ2());
            }

            if (pl.hasCurrentLimits1()) {
                readCurrentLimits(line::newCurrentLimits1, pl.getCurrentLimits1());
            }
            if (pl.hasCurrentLimits2()) {
                readCurrentLimits(line::newCurrentLimits2, pl.getCurrentLimits2());
            }
        });
    }

    private static void readHvdcLines(Iidm.Network pNetwork, Network network) {
        List<Iidm.HvdcLine> lines = pNetwork.getHvdcLineList();
        lines.forEach(pline -> {
            HvdcLineAdder adder = network.newHvdcLine();
            adder.setId(pline.getId());
            if (pline.hasName()) {
                adder.setName(pline.getName());
            }
            adder.setConvertersMode(protoToIidmConvertersMode(pline.getConvertersMode()));
            adder.setNominalV(pline.getNominalV());
            adder.setActivePowerSetpoint(pline.getActivePowerSetpoint());
            adder.setMaxP(pline.getMaxP());
            adder.setR(pline.getR());
            adder.setConverterStationId1(pline.getConverterStation1());
            adder.setConverterStationId2(pline.getConverterStation2());

            HvdcLine hvdcLine = adder.add();
            readProperties(hvdcLine, pline.getPropertyList());
        });
    }

    private static void readNodeOrBus(LineAdder adder, Iidm.Line pLine) {
        if (pLine.hasBus1()) {
            adder.setBus1(pLine.getBus1());
        }
        if (pLine.hasConnectableBus1()) {
            adder.setConnectableBus1(pLine.getConnectableBus1());
        }
        if (pLine.hasNode1()) {
            adder.setNode1(pLine.getNode1());
        }
        adder.setVoltageLevel1(pLine.getVoltageLevelId1());
        if (pLine.hasBus2()) {
            adder.setBus2(pLine.getBus2());
        }
        if (pLine.hasConnectableBus2()) {
            adder.setConnectableBus2(pLine.getConnectableBus2());
        }
        if (pLine.hasNode2()) {
            adder.setNode2(pLine.getNode2());
        }
        adder.setVoltageLevel2(pLine.getVoltageLevelId2());
    }

    private static void readTwoWindingTransformers(Iidm.Substation psub, Substation sub, Network network) {
        List<Iidm.TwoWindingsTransformer> ptList = psub.getTwoWindingsTransformerList();
        ptList.forEach(pTwt -> {
            TwoWindingsTransformerAdder tAdder = sub.newTwoWindingsTransformer();
            tAdder.setId(pTwt.getId());
            if (pTwt.hasName()) {
                tAdder.setName(pTwt.getName());
            }
            tAdder.setR(pTwt.getR());
            tAdder.setX(pTwt.getX());
            tAdder.setG(pTwt.getG());
            tAdder.setB(pTwt.getB());
            tAdder.setRatedU1(pTwt.getRatedU1());
            tAdder.setRatedU2(pTwt.getRatedU2());

            readNodeOrBus(tAdder, pTwt);
            TwoWindingsTransformer twt = tAdder.add();
            if (pTwt.hasRatioTapChanger()) {
                readRatioTapChanger(twt, pTwt.getRatioTapChanger(), network);
            }

            if (pTwt.hasPhaseTapChanger()) {
                readPhaseTapChanger(twt, pTwt.getPhaseTapChanger(), network);
            }

            if (pTwt.hasP1()) {
                twt.getTerminal1().setP(pTwt.getP1());
            }

            if (pTwt.hasQ1()) {
                twt.getTerminal1().setQ(pTwt.getQ1());
            }

            if (pTwt.hasP2()) {
                twt.getTerminal2().setP(pTwt.getP2());
            }
            if (pTwt.hasQ2()) {
                twt.getTerminal2().setQ(pTwt.getQ2());
            }

            if (pTwt.hasCurrentLimits1()) {
                readCurrentLimits(twt::newCurrentLimits1, pTwt.getCurrentLimits1());
            }
            if (pTwt.hasCurrentLimits2()) {
                readCurrentLimits(twt::newCurrentLimits2, pTwt.getCurrentLimits2());
            }

            readProperties(twt, pTwt.getPropertyList());
        });
    }

    protected static void readNodeOrBus(ThreeWindingsTransformerAdder.LegAdder adder1, ThreeWindingsTransformerAdder.LegAdder adder2, ThreeWindingsTransformerAdder.LegAdder adder3, Iidm.ThreeWindingsTransformer pTwt) {
        if (pTwt.hasBus1()) {
            adder1.setBus(pTwt.getBus1());
        }
        if (pTwt.hasConnectableBus1()) {
            adder1.setConnectableBus(pTwt.getConnectableBus1());
        }
        if (pTwt.hasNode1()) {
            adder1.setNode(pTwt.getNode1());
        }
        adder1.setVoltageLevel(pTwt.getVoltageLevelId1());

        if (pTwt.hasBus2()) {
            adder2.setBus(pTwt.getBus2());
        }
        if (pTwt.hasConnectableBus2()) {
            adder2.setConnectableBus(pTwt.getConnectableBus2());
        }
        if (pTwt.hasNode2()) {
            adder2.setNode(pTwt.getNode2());
        }
        adder2.setVoltageLevel(pTwt.getVoltageLevelId2());

        if (pTwt.hasBus3()) {
            adder3.setBus(pTwt.getBus3());
        }
        if (pTwt.hasConnectableBus3()) {
            adder3.setConnectableBus(pTwt.getConnectableBus3());
        }
        if (pTwt.hasNode3()) {
            adder3.setNode(pTwt.getNode3());
        }
        adder3.setVoltageLevel(pTwt.getVoltageLevelId3());
    }

    private static void readThreeWindingTransformers(Iidm.Substation psub, Substation sub, Network network) {
        List<Iidm.ThreeWindingsTransformer> ptList = psub.getThreeWindingsTransformerList();
        ptList.forEach(pTwt -> {
            ThreeWindingsTransformerAdder adder = sub.newThreeWindingsTransformer();
            adder.setId(pTwt.getId());
            if (pTwt.hasName()) {
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
            if (pTwt.hasP1()) {
                twt.getLeg1().getTerminal().setP(pTwt.getP1());
            }

            if (pTwt.hasQ1()) {
                twt.getLeg1().getTerminal().setQ(pTwt.getQ1());
            }

            if (pTwt.hasP2()) {
                twt.getLeg2().getTerminal().setP(pTwt.getP2());
            }

            if (pTwt.hasQ2()) {
                twt.getLeg2().getTerminal().setQ(pTwt.getQ2());
            }

            if (pTwt.hasRatioTapChanger2()) {
                readRatioTapChanger(twt.getLeg2(), pTwt.getRatioTapChanger2(), network);

            }
            if (pTwt.hasRatioTapChanger3()) {
                readRatioTapChanger(twt.getLeg3(), pTwt.getRatioTapChanger3(), network);

            }
            if (pTwt.hasCurrentLimits1()) {
                readCurrentLimits(twt.getLeg1()::newCurrentLimits, pTwt.getCurrentLimits1());
            }
            if (pTwt.hasCurrentLimits2()) {
                readCurrentLimits(twt.getLeg2()::newCurrentLimits, pTwt.getCurrentLimits2());
            }
            readProperties(twt, pTwt.getPropertyList());
        });
    }

    public static void readCurrentLimits(Supplier<CurrentLimitsAdder> currentLimitOwner, Iidm.CurrentLimit plimit) {
        CurrentLimitsAdder adder = currentLimitOwner.get();
        if (plimit.hasPermanentLimit()) {
            adder.setPermanentLimit(plimit.getPermanentLimit());
        }

        List<Iidm.TemporaryLimitType> tempLimits = plimit.getTemporaryLimitList();
        tempLimits.forEach(ptl -> {
            adder.beginTemporaryLimit()
                    .setName(ptl.getName())
                    .setAcceptableDuration(ptl.hasAcceptableDuration() ? ptl.getAcceptableDuration() : Integer.MAX_VALUE)
                    .setValue(ptl.hasValue() ? ptl.getValue() : Double.MAX_VALUE)
                    .setFictitious(ptl.hasFictitious() ? ptl.getFictitious() : false)
                    .endTemporaryLimit();
        });
        adder.add();
    }

    private static void readRatioTapChanger(TwoWindingsTransformer twt, Iidm.RatioTapChanger prtc, Network network) {
        RatioTapChangerAdder rtcAdder = twt.newRatioTapChanger();
        rtcAdder.setLowTapPosition(prtc.getLowTapPosition());
        rtcAdder.setTapPosition(prtc.getTapPosition());
        rtcAdder.setLoadTapChangingCapabilities(prtc.getLoadTapChangingCapabilities());
        if (prtc.hasTargetDeadband()) {
            rtcAdder.setTargetDeadband(prtc.getTargetDeadband());
        }
        if (prtc.hasTargetV()) {
            rtcAdder.setTargetV(prtc.getTargetV());
        }
        if (prtc.getLoadTapChangingCapabilities()) {
            rtcAdder.setRegulating(prtc.getRegulating());
        }

        if (prtc.hasTerminalRef()) {
            rtcAdder.setRegulationTerminal(readTerminalRef(prtc, network));
        }

        List<Iidm.RatioTapChangerStep> psteps = prtc.getStepList();
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

    private static void readRatioTapChanger(ThreeWindingsTransformer.Leg2or3 twl, Iidm.RatioTapChanger prtc, Network network) {
        RatioTapChangerAdder rtcAdder = twl.newRatioTapChanger();
        rtcAdder.setLowTapPosition(prtc.getLowTapPosition());
        rtcAdder.setTapPosition(prtc.getTapPosition());
        rtcAdder.setLoadTapChangingCapabilities(prtc.getLoadTapChangingCapabilities());
        if (prtc.hasTargetDeadband()) {
            rtcAdder.setTargetDeadband(prtc.getTargetDeadband());
        }
        if (prtc.hasTargetV()) {
            rtcAdder.setTargetV(prtc.getTargetV());
        }
        if (prtc.getLoadTapChangingCapabilities()) {
            rtcAdder.setRegulating(prtc.getRegulating());
        }

        if (prtc.hasTerminalRef()) {
            rtcAdder.setRegulationTerminal(readTerminalRef(prtc, network));
        }

        List<Iidm.RatioTapChangerStep> psteps = prtc.getStepList();
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

    private static void readPhaseTapChanger(TwoWindingsTransformer twt, Iidm.PhaseTapChanger ptc, Network network) {
        PhaseTapChangerAdder ptcAdder = twt.newPhaseTapChanger();
        ptcAdder.setTapPosition(ptc.getTapPosition());
        ptcAdder.setLowTapPosition(ptc.getLowTapPosition());
        ptcAdder.setRegulationMode(protoToIidmPhaseRegulationMode(ptc.getRegulationMode()));
        if (ptc.hasRegulationValue()) {
            ptcAdder.setRegulationValue(ptc.getRegulationValue());
        }
        if (ptc.hasTargetDeadband()) {
            ptcAdder.setTargetDeadband(ptc.getTargetDeadband());
        }
        if (ptc.hasRegulating()) {
            ptcAdder.setRegulating(ptc.getRegulating());
        } else {
            ptcAdder.setRegulating(false);
        }
        if (ptc.hasTerminalRef()) {
            ptcAdder.setRegulationTerminal(readTerminalRef(ptc, network));
        }

        List<Iidm.PhaseTapChangerStep> psteps = ptc.getStepList();
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

    private static Terminal readTerminalRef(Iidm.RatioTapChanger prtc, Network network) {
        String id = prtc.getTerminalRef().getId();
        Iidm.Side pSide = prtc.getTerminalRef().getSide();
        Identifiable identifiable = network.getIdentifiable(id);
        if (identifiable instanceof Injection) {
            return ((Injection) identifiable).getTerminal();
        } else if (identifiable instanceof Branch) {
            return pSide.equals(Iidm.Side.Side_ONE) ? ((Branch) identifiable).getTerminal1()
                    : ((Branch) identifiable).getTerminal2();
        } else if (identifiable instanceof ThreeWindingsTransformer) {
            ThreeWindingsTransformer twt = (ThreeWindingsTransformer) identifiable;
            return twt.getTerminal(protoToIidmThreeWindingTransformerSide(pSide));
        } else {
            throw new AssertionError("Unexpected Identifiable instance: " + identifiable.getClass());
        }
    }

    private static Terminal readTerminalRef(Iidm.PhaseTapChanger ptc, Network network) {
        String id = ptc.getTerminalRef().getId();
        Iidm.Side pSide = ptc.getTerminalRef().getSide();
        Identifiable identifiable = network.getIdentifiable(id);
        if (identifiable instanceof Injection) {
            return ((Injection) identifiable).getTerminal();
        } else if (identifiable instanceof Branch) {
            return pSide.equals(Iidm.Side.Side_ONE) ? ((Branch) identifiable).getTerminal1()
                    : ((Branch) identifiable).getTerminal2();
        } else if (identifiable instanceof ThreeWindingsTransformer) {
            ThreeWindingsTransformer twt = (ThreeWindingsTransformer) identifiable;
            return twt.getTerminal(protoToIidmThreeWindingTransformerSide(pSide));
        } else {
            throw new AssertionError("Unexpected Identifiable instance: " + identifiable.getClass());
        }
    }

    private static void readNodeOrBus(TwoWindingsTransformerAdder tAdder, Iidm.TwoWindingsTransformer pTwt) {
        if (pTwt.hasBus1()) {
            tAdder.setBus1(pTwt.getBus1());
        }
        if (pTwt.hasConnectableBus1()) {
            tAdder.setConnectableBus1(pTwt.getConnectableBus1());
        }
        if (pTwt.hasNode1()) {
            tAdder.setNode1(pTwt.getNode1());
        }
        tAdder.setVoltageLevel1(pTwt.getVoltageLevelId1());
        if (pTwt.hasBus2()) {
            tAdder.setBus2(pTwt.getBus2());
        }
        if (pTwt.hasConnectableBus2()) {
            tAdder.setConnectableBus2(pTwt.getConnectableBus2());
        }
        if (pTwt.hasNode2()) {
            tAdder.setNode2(pTwt.getNode2());
        }
        tAdder.setVoltageLevel2(pTwt.getVoltageLevelId2());
    }

    private static void readGenerators(VoltageLevel vl, Iidm.VoltageLevel pvl) {
        List<Iidm.Generator> pgenerators = pvl.getGeneratorList();

        pgenerators.forEach(pgen -> {
            GeneratorAdder genAdder = vl.newGenerator();
            genAdder.setId(pgen.getId());
            if (pgen.hasName()) {
                genAdder.setName(pgen.getName());
            }
            genAdder.setEnergySource(protoToIidmEnergySource(pgen.getEnergySource()));
            genAdder.setMinP(pgen.getMinP());
            genAdder.setMaxP(pgen.getMaxP());
            genAdder.setTargetP(pgen.getTargetP());
            genAdder.setVoltageRegulatorOn(pgen.getVoltageRegulatorOn());

            if (pgen.hasBus()) {
                genAdder.setBus(pgen.getBus());
            }

            if (pgen.hasConnectableBus()) {
                genAdder.setConnectableBus(pgen.getConnectableBus());
            }

            if (pgen.hasNode()) {
                genAdder.setNode(pgen.getNode());
            }

            if (pgen.hasTargetV()) {
                genAdder.setTargetV(pgen.getTargetV());
            }

            if (pgen.hasTargetQ()) {
                genAdder.setTargetQ(pgen.getTargetQ());
            }

            Generator newGen = genAdder.add();
            if (pgen.hasMinMaxReactiveLimits()) {
                ReactiveLimitsProto.INSTANCE.readMinMaxReactiveLimits(newGen, pgen.getMinMaxReactiveLimits());
            }
            if (pgen.hasReactiveCapabilityCurve()) {
                ReactiveLimitsProto.INSTANCE.readReactiveCapabilityCurve(newGen, pgen.getReactiveCapabilityCurve());
            }

            readProperties(newGen, pgen.getPropertyList());
            if (pgen.hasP()) {
                newGen.getTerminal().setP(pgen.getP());
            }
            if (pgen.hasQ()) {
                newGen.getTerminal().setQ(pgen.getQ());
            }
        });
    }

    private static void readLoads(VoltageLevel vl, Iidm.VoltageLevel pvl, TopologyKind topologyKind) {
        List<Iidm.Load> ploadLists = pvl.getLoadList();

        ploadLists.forEach(pl -> {
            LoadAdder loadAdder = vl.newLoad();
            loadAdder.setId(pl.getId());
            if (pl.hasName()) {
                loadAdder.setName(pl.getName());
            }
            loadAdder.setP0(pl.getP0());
            loadAdder.setQ0(pl.getQ0());
            if (pl.hasLoadType()) {
                loadAdder.setLoadType(protoToIidmLoadType(pl.getLoadType()));
            }
            if (pl.hasBus()) {
                loadAdder.setBus(pl.getBus());
            }

            if (pl.hasConnectableBus()) {
                loadAdder.setConnectableBus(pl.getConnectableBus());
            }

            if (pl.hasNode()) {
                loadAdder.setNode(pl.getNode());
            }

            Load newLoad = loadAdder.add();

            if (pl.hasP()) {
                newLoad.getTerminal().setP(pl.getP());
            }
            if (pl.hasQ()) {
                newLoad.getTerminal().setQ(pl.getQ());
            }

            readProperties(newLoad, pl.getPropertyList());
        });
    }

    private static void readBatteries(VoltageLevel vl, Iidm.VoltageLevel pvl, TopologyKind topologyKind) {
        List<Iidm.Battery> pbatteries = pvl.getBatteryList();

        pbatteries.forEach(pb -> {
            BatteryAdder adder = vl.newBattery();
            adder.setId(pb.getId());
            if (pb.hasName()) {
                adder.setName(pb.getName());
            }

            adder.setMinP(pb.getMinP());
            adder.setMaxP(pb.getMaxP());
            adder.setP0(pb.getP0());
            adder.setQ0(pb.getQ0());

            if (pb.hasBus()) {
                adder.setBus(pb.getBus());
            }

            if (pb.hasConnectableBus()) {
                adder.setConnectableBus(pb.getConnectableBus());
            }

            if (pb.hasNode()) {
                adder.setNode(pb.getNode());
            }
            Battery newBattery = adder.add();

            if (pb.hasMinMaxReactiveLimits()) {
                ReactiveLimitsProto.INSTANCE.readMinMaxReactiveLimits(newBattery, pb.getMinMaxReactiveLimits());
            }
            if (pb.hasReactiveCapabilityCurve()) {
                ReactiveLimitsProto.INSTANCE.readReactiveCapabilityCurve(newBattery, pb.getReactiveCapabilityCurve());
            }

            if (pb.hasP()) {
                newBattery.getTerminal().setP(pb.getP());
            }
            if (pb.hasQ()) {
                newBattery.getTerminal().setQ(pb.getQ());
            }

            readProperties(newBattery, pb.getPropertyList());
        });

    }

    private static void readShunts(VoltageLevel newVoltageLevel, Iidm.VoltageLevel pvl, TopologyKind topologyKind) {
        List<Iidm.ShuntCompensator> pShuntsList = pvl.getShuntCompensatorList();
        pShuntsList.forEach(pshunt -> {
            ShuntCompensatorAdder adder = newVoltageLevel.newShuntCompensator();
            adder.setId(pshunt.getId());
            if (pshunt.hasName()) {
                adder.setName(pshunt.getName());
            }
            adder.setbPerSection(pshunt.getBPerSection());
            adder.setCurrentSectionCount(pshunt.getCurrentSectionCount());
            adder.setMaximumSectionCount(pshunt.getMaximumSectionCount());

            if (pshunt.hasBus()) {
                adder.setBus(pshunt.getBus());
            }

            if (pshunt.hasConnectableBus()) {
                adder.setConnectableBus(pshunt.getConnectableBus());
            }

            if (pshunt.hasNode()) {
                adder.setNode(pshunt.getNode());
            }

            ShuntCompensator newShunt = adder.add();
            if (pshunt.hasP()) {
                newShunt.getTerminal().setP(pshunt.getP());
            }
            if (pshunt.hasQ()) {
                newShunt.getTerminal().setQ(pshunt.getQ());
            }
            readProperties(newShunt, pshunt.getPropertyList());
        });
    }

    private static void readDanglingLines(VoltageLevel newVoltageLevel, Iidm.VoltageLevel pvl, TopologyKind topologyKind) {
        List<Iidm.DanglingLine> pDanglingLines = pvl.getDanglingLineList();
        pDanglingLines.forEach(pdl -> {
            DanglingLineAdder adder = newVoltageLevel.newDanglingLine();
            adder.setId(pdl.getId());
            if (pdl.hasName()) {
                adder.setName(pdl.getName());
            }

            adder.setB(pdl.getB());
            adder.setG(pdl.getG());
            adder.setR(pdl.getR());
            adder.setP0(pdl.getP0());
            adder.setQ0(pdl.getQ0());
            adder.setX(pdl.getX());
            if (pdl.hasUcteXnodeCode()) {
                adder.setUcteXnodeCode(pdl.getUcteXnodeCode());
            }

            if (pdl.hasBus()) {
                adder.setBus(pdl.getBus());
            }

            if (pdl.hasConnectableBus()) {
                adder.setConnectableBus(pdl.getConnectableBus());
            }

            if (pdl.hasNode()) {
                adder.setNode(pdl.getNode());
            }

            DanglingLine newDanglingLine = adder.add();

            if (pdl.hasCurrentLimits()) {
                readCurrentLimits(newDanglingLine::newCurrentLimits, pdl.getCurrentLimits());
            }

            if (pdl.hasP()) {
                newDanglingLine.getTerminal().setP(pdl.getP());
            }
            if (pdl.hasQ()) {
                newDanglingLine.getTerminal().setQ(pdl.getQ());
            }
            readProperties(newDanglingLine, pdl.getPropertyList());
        });
    }

    private static void readStaticVarCompensators(VoltageLevel newVoltageLevel, Iidm.VoltageLevel pvl, TopologyKind topologyKind) {
        List<Iidm.StaticVarCompensator> psvcList = pvl.getStaticVarCompensatorList();
        psvcList.forEach(psvc -> {
            StaticVarCompensatorAdder adder = newVoltageLevel.newStaticVarCompensator();
            adder.setId(psvc.getId());
            if (psvc.hasName()) {
                adder.setName(psvc.getName());
            }

            adder.setRegulationMode(protoToIidmStaticVarCompensatorRegulationMode(psvc.getRegulationMode()));
            adder.setBmin(psvc.getBMin());
            adder.setBmax(psvc.getBMax());
            if (psvc.hasReactivePowerSetPoint()) {
                adder.setReactivePowerSetPoint(psvc.getReactivePowerSetPoint());
            }
            if (psvc.hasVoltageSetPoint()) {
                adder.setVoltageSetPoint(psvc.getVoltageSetPoint());
            }

            if (psvc.hasBus()) {
                adder.setBus(psvc.getBus());
            }

            if (psvc.hasConnectableBus()) {
                adder.setConnectableBus(psvc.getConnectableBus());
            }

            if (psvc.hasNode()) {
                adder.setNode(psvc.getNode());
            }

            StaticVarCompensator svc = adder.add();
            if (psvc.hasP()) {
                svc.getTerminal().setP(psvc.getP());
            }
            if (psvc.hasQ()) {
                svc.getTerminal().setQ(psvc.getQ());
            }
            readProperties(svc, psvc.getPropertyList());
        });
    }

    private static void readVscConverterStations(VoltageLevel newVoltageLevel, Iidm.VoltageLevel pvl, TopologyKind topologyKind) {
        List<Iidm.VscConverterStation> pvscList = pvl.getVscConverterStationList();
        pvscList.forEach(pvsc -> {
            VscConverterStationAdder adder = newVoltageLevel.newVscConverterStation();
            adder.setId(pvsc.getId());
            if (pvsc.hasName()) {
                adder.setName(pvsc.getName());
            }
            adder.setLossFactor(pvsc.getLossFactor());
            adder.setVoltageRegulatorOn(pvsc.getVoltageRegulatorOn());

            if (pvsc.hasReactivePowerSetpoint()) {
                adder.setReactivePowerSetpoint(pvsc.getReactivePowerSetpoint());
            }

            if (pvsc.hasVoltageSetpoint()) {
                adder.setVoltageSetpoint(pvsc.getVoltageSetpoint());
            }

            if (pvsc.hasBus()) {
                adder.setBus(pvsc.getBus());
            }

            if (pvsc.hasConnectableBus()) {
                adder.setConnectableBus(pvsc.getConnectableBus());
            }

            if (pvsc.hasNode()) {
                adder.setNode(pvsc.getNode());
            }

            VscConverterStation vsc = adder.add();

            if (pvsc.hasMinMaxReactiveLimits()) {
                ReactiveLimitsProto.INSTANCE.readMinMaxReactiveLimits(vsc, pvsc.getMinMaxReactiveLimits());
            }
            if (pvsc.hasReactiveCapabilityCurve()) {
                ReactiveLimitsProto.INSTANCE.readReactiveCapabilityCurve(vsc, pvsc.getReactiveCapabilityCurve());
            }

            if (pvsc.hasP()) {
                vsc.getTerminal().setP(pvsc.getP());
            }
            if (pvsc.hasQ()) {
                vsc.getTerminal().setQ(pvsc.getQ());
            }
            readProperties(vsc, pvsc.getPropertyList());
        });
    }

    private static void readLccConverterStations(VoltageLevel newVoltageLevel, Iidm.VoltageLevel pvl, TopologyKind topologyKind) {
        List<Iidm.LccConverterStation> plccList = pvl.getLccConverterStationList();
        plccList.forEach(plcc -> {
            LccConverterStationAdder adder = newVoltageLevel.newLccConverterStation();
            adder.setId(plcc.getId());
            if (plcc.hasName()) {
                adder.setName(plcc.getName());
            }
            adder.setLossFactor(plcc.getLossFactor());
            adder.setPowerFactor(plcc.getPowerFactor());

            if (plcc.hasBus()) {
                adder.setBus(plcc.getBus());
            }

            if (plcc.hasConnectableBus()) {
                adder.setConnectableBus(plcc.getConnectableBus());
            }

            if (plcc.hasNode()) {
                adder.setNode(plcc.getNode());
            }

            LccConverterStation lcc = adder.add();

            if (plcc.hasP()) {
                lcc.getTerminal().setP(plcc.getP());
            }
            if (plcc.hasQ()) {
                lcc.getTerminal().setQ(plcc.getQ());
            }
            readProperties(lcc, plcc.getPropertyList());
        });
    }
}
