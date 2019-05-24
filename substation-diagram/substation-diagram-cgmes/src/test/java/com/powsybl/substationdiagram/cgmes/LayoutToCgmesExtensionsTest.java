/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.cgmes;

import com.powsybl.cgmes.iidm.extensions.dl.*;
import com.powsybl.iidm.network.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.eu>
 */
public class LayoutToCgmesExtensionsTest {

    private Network network;

    @Before
    public void setUp() {
        createNetwork();
    }

    private void createNetwork() {
        network = NetworkFactory.create("test", "test");
        Substation substation = network.newSubstation()
                .setId("Substation")
                .setCountry(Country.FR)
                .add();
        createFirstVoltageLevel(substation);
    }

    private VoltageLevel createFirstVoltageLevel(Substation substation) {
        VoltageLevel voltageLevel1 = substation.newVoltageLevel()
                .setId("VoltageLevel1")
                .setNominalV(400)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        voltageLevel1.getBusBreakerView().newBus()
                .setId("Bus1")
                .add();
        voltageLevel1.newLoad()
                .setId("Load")
                .setBus("Bus1")
                .setConnectableBus("Bus1")
                .setP0(100)
                .setQ0(50)
                .add();
        voltageLevel1.newShuntCompensator()
                .setId("Shunt")
                .setBus("Bus1")
                .setConnectableBus("Bus1")
                .setbPerSection(1e-5)
                .setCurrentSectionCount(1)
                .setMaximumSectionCount(1)
                .add();
        voltageLevel1.newDanglingLine()
                .setId("DanglingLine")
                .setBus("Bus1")
                .setR(10.0)
                .setX(1.0)
                .setB(10e-6)
                .setG(10e-5)
                .setP0(50.0)
                .setQ0(30.0)
                .add();
        return voltageLevel1;
    }

    private void checkExtensionsSet() {
        network.getVoltageLevelStream().forEach(vl -> {
            vl.visitEquipments(new DefaultTopologyVisitor() {
                @Override
                public void visitDanglingLine(DanglingLine danglingLine) {
                    assertNotNull(danglingLine.getExtension(LineDiagramData.class));
                }

                @Override
                public void visitGenerator(Generator generator) {
                    assertNotNull(generator.getExtension(InjectionDiagramData.class));
                }

                @Override
                public void visitShuntCompensator(ShuntCompensator sc) {
                    assertNotNull(sc.getExtension(InjectionDiagramData.class));
                }

                @Override
                public void visitLoad(Load load) {
                    assertNotNull(load.getExtension(InjectionDiagramData.class));
                }

                @Override
                public void visitStaticVarCompensator(StaticVarCompensator staticVarCompensator) {
                    assertNotNull(staticVarCompensator.getExtension(InjectionDiagramData.class));
                }

                @Override
                public void visitLine(Line line, Branch.Side side) {
                    assertNotNull(line.getExtension(LineDiagramData.class));
                }

                @Override
                public void visitBusbarSection(BusbarSection busBarSection) {
                    assertNotNull(busBarSection.getExtension(NodeDiagramData.class));
                }

                @Override
                public void visitTwoWindingsTransformer(TwoWindingsTransformer transformer, Branch.Side side) {
                    assertNotNull(transformer.getExtension(CouplingDeviceDiagramData.class));
                }

                @Override
                public void visitThreeWindingsTransformer(ThreeWindingsTransformer transformer, ThreeWindingsTransformer.Side side) {
                    assertNotNull(transformer.getExtension(ThreeWindingsTransformerDiagramData.class));
                }

                @Override
                public void visitHvdcConverterStation(HvdcConverterStation<?> converterStation) {
                    assertNotNull(converterStation.getExtension(LineDiagramData.class));
                }
            });
        });
    }

    private void checkExtensionsUnset() {
        network.getVoltageLevelStream().forEach(vl -> {
            vl.visitEquipments(new DefaultTopologyVisitor() {
                @Override
                public void visitDanglingLine(DanglingLine danglingLine) {
                    assertNull(danglingLine.getExtension(LineDiagramData.class));
                }

                @Override
                public void visitGenerator(Generator generator) {
                    assertNull(generator.getExtension(InjectionDiagramData.class));
                }

                @Override
                public void visitShuntCompensator(ShuntCompensator sc) {
                    assertNull(sc.getExtension(InjectionDiagramData.class));
                }

                @Override
                public void visitLoad(Load load) {
                    assertNull(load.getExtension(InjectionDiagramData.class));
                }

                @Override
                public void visitStaticVarCompensator(StaticVarCompensator staticVarCompensator) {
                    assertNull(staticVarCompensator.getExtension(InjectionDiagramData.class));
                }

                @Override
                public void visitLine(Line line, Branch.Side side) {
                    assertNull(line.getExtension(LineDiagramData.class));
                }

                @Override
                public void visitBusbarSection(BusbarSection busBarSection) {
                    assertNull(busBarSection.getExtension(NodeDiagramData.class));
                }

                @Override
                public void visitTwoWindingsTransformer(TwoWindingsTransformer transformer, Branch.Side side) {
                    assertNull(transformer.getExtension(CouplingDeviceDiagramData.class));
                }

                @Override
                public void visitThreeWindingsTransformer(ThreeWindingsTransformer transformer, ThreeWindingsTransformer.Side side) {
                    assertNull(transformer.getExtension(ThreeWindingsTransformerDiagramData.class));
                }

                @Override
                public void visitHvdcConverterStation(HvdcConverterStation<?> converterStation) {
                    assertNull(converterStation.getExtension(LineDiagramData.class));
                }
            });
        });
    }

    @Test
    public void testCgmesDlExtensionsEmpty() {
        checkExtensionsUnset();
    }

    @Test
    public void testCgmesDlExtensionsSet() {
        LayoutToCgmesExtensionsConverter lconv = new LayoutToCgmesExtensionsConverter();
        lconv.convertLayout(network);

        checkExtensionsSet();
    }

}
