/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.iidm.extensions.gl;

import static org.junit.Assert.assertEquals;

import org.joda.time.DateTime;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.NetworkFactory;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.VoltageLevel;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public final class GLTestUtils {

    public static final double SUBSTATION_1_X = 0.5492960214614868;
    public static final double SUBSTATION_1_Y = 51.380348205566406;
    public static final double SUBSTATION_2_X = 0.30759671330451965;
    public static final double SUBSTATION_2_Y = 52.00010299682617;
    public static final double LINE_1_X = 0.5132722854614258;
    public static final double LINE_1_Y = 51.529258728027344;
    public static final double LINE_2_X = 0.4120868146419525;
    public static final double LINE_2_Y = 51.944923400878906;

    private GLTestUtils() {
    }

    public static Network getNetwork() {
        Network network = NetworkFactory.create("Network", "test");
        network.setCaseDate(DateTime.parse("2018-01-01T00:30:00.000+01:00"));
        Substation substation1 = network.newSubstation()
                .setId("Substation1")
                .setCountry(Country.FR)
                .add();
        VoltageLevel voltageLevel1 = substation1.newVoltageLevel()
                .setId("VoltageLevel1")
                .setNominalV(400)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        voltageLevel1.getBusBreakerView().newBus()
                .setId("Bus1")
                .add();
        Substation substation2 = network.newSubstation()
                .setId("Substation2")
                .setCountry(Country.FR)
                .add();
        VoltageLevel voltageLevel2 = substation2.newVoltageLevel()
                .setId("VoltageLevel2")
                .setNominalV(400)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        voltageLevel2.getBusBreakerView().newBus()
                .setId("Bus2")
                .add();
        network.newLine()
                .setId("Line")
                .setVoltageLevel1(voltageLevel1.getId())
                .setBus1("Bus1")
                .setConnectableBus1("Bus1")
                .setVoltageLevel2(voltageLevel2.getId())
                .setBus2("Bus2")
                .setConnectableBus2("Bus2")
                .setR(3.0)
                .setX(33.0)
                .setG1(0.0)
                .setB1(386E-6 / 2)
                .setG2(0.0)
                .setB2(386E-6 / 2)
                .add();
        return network;
    }

    public static void checkNetwork(Network network) {
        Substation networkSubstation1 = network.getSubstation("Substation1");
        SubstationPosition<Substation> networkSubstationPosition1 = networkSubstation1.getExtension(SubstationPosition.class);
        checkPoint(networkSubstationPosition1.getPoint(), SUBSTATION_1_X, SUBSTATION_1_Y, 0);

        Substation networkSubstation2 = network.getSubstation("Substation2");
        SubstationPosition<Substation> networkSubstationPosition2 = networkSubstation2.getExtension(SubstationPosition.class);
        checkPoint(networkSubstationPosition2.getPoint(), SUBSTATION_2_X, SUBSTATION_2_Y, 0);

        Line networkLine = network.getLine("Line");
        LinePosition<Line> networkLinePosition = networkLine.getExtension(LinePosition.class);
        checkPoint(networkLinePosition.getPoints().get(0), SUBSTATION_1_X, SUBSTATION_1_Y, 1);
        checkPoint(networkLinePosition.getPoints().get(1), LINE_1_X, LINE_1_Y, 2);
        checkPoint(networkLinePosition.getPoints().get(2), LINE_2_X, LINE_2_Y, 3);
        checkPoint(networkLinePosition.getPoints().get(3), SUBSTATION_2_X, SUBSTATION_2_Y, 4);
    }

    private static void checkPoint(PositionPoint point, double x, double y, int seq) {
        assertEquals(x, point.getX(), 0);
        assertEquals(y, point.getY(), 0);
        assertEquals(seq, point.getSeq(), 0);
    }

}
