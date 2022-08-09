/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util.extensions;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.GeneratorShortCircuit;
import com.powsybl.incubator.simulator.util.extensions.iidm.*;
import com.powsybl.openloadflow.network.LfBranch;
import com.powsybl.openloadflow.network.LfBus;
import com.powsybl.openloadflow.network.LfGenerator;
import com.powsybl.openloadflow.network.LfNetwork;

import java.util.List;
import java.util.Objects;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public final class ShortCircuitExtensions {

    public static final String PROPERTY_NAME = "ShortCircuit";

    private ShortCircuitExtensions() {
    }

    public static void add(Network network, List<LfNetwork> lfNetworks) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(lfNetworks);
        for (LfNetwork lfNetwork : lfNetworks) {
            for (LfBus lfBus : lfNetwork.getBuses()) {
                for (LfGenerator lfGenerator : lfBus.getGenerators()) {
                    addGeneratorExtension(network, lfGenerator);
                }
            }

            for (LfBranch lfBranch : lfNetwork.getBranches()) {
                switch (lfBranch.getBranchType()) {
                    case LINE:
                        addLineExtension(network, lfBranch);
                        break;

                    case TRANSFO_2:
                        addTransfo2Extension(network, lfBranch);
                        break;

                    case TRANSFO_3_LEG_1:
                    case TRANSFO_3_LEG_2:
                    case TRANSFO_3_LEG_3:
                        addTransfo3Extension(network, lfBranch);
                        break;

                    case DANGLING_LINE:
                        // FIXME something to do?
                        break;
                }
            }
        }
    }

    private static void addTransfo3Extension(Network network, LfBranch lfBranch) {
        String t3wId = lfBranch.getOriginalIds().get(0);
        ThreeWindingsTransformer twt = network.getThreeWindingsTransformer(t3wId);

        double leg1CoeffRo = ThreeWindingsTransformerShortCircuitAdder.DEFAULT_COEFF_RO;
        double leg2CoeffRo = ThreeWindingsTransformerShortCircuitAdder.DEFAULT_COEFF_RO;
        double leg3CoeffRo = ThreeWindingsTransformerShortCircuitAdder.DEFAULT_COEFF_RO;
        double leg1CoeffXo = ThreeWindingsTransformerShortCircuitAdder.DEFAULT_COEFF_XO;
        double leg2CoeffXo = ThreeWindingsTransformerShortCircuitAdder.DEFAULT_COEFF_XO;
        double leg3CoeffXo = ThreeWindingsTransformerShortCircuitAdder.DEFAULT_COEFF_XO;
        boolean leg1FreeFluxes = ThreeWindingsTransformerShortCircuitAdder.DEFAULT_FREE_FLUXES;
        boolean leg2FreeFluxes = ThreeWindingsTransformerShortCircuitAdder.DEFAULT_FREE_FLUXES;
        boolean leg3FreeFluxes = ThreeWindingsTransformerShortCircuitAdder.DEFAULT_FREE_FLUXES;
        LegConnectionType leg1ConnectionType = ThreeWindingsTransformerShortCircuitAdder.DEFAULT_LEG1_CONNECTION_TYPE;
        LegConnectionType leg2ConnectionType = ThreeWindingsTransformerShortCircuitAdder.DEFAULT_LEG2_CONNECTION_TYPE;
        LegConnectionType leg3ConnectionType = ThreeWindingsTransformerShortCircuitAdder.DEFAULT_LEG3_CONNECTION_TYPE;
        var extensions = twt.getExtension(ThreeWindingsTransformerShortCircuit.class);
        if (extensions != null) {
            leg1CoeffRo = extensions.getLeg1CoeffRo();
            leg2CoeffRo = extensions.getLeg2CoeffRo();
            leg3CoeffRo = extensions.getLeg3CoeffRo();
            leg1CoeffXo = extensions.getLeg1CoeffXo();
            leg2CoeffXo = extensions.getLeg2CoeffXo();
            leg3CoeffXo = extensions.getLeg3CoeffXo();
            leg1FreeFluxes = extensions.isLeg1FreeFluxes();
            leg2FreeFluxes = extensions.isLeg2FreeFluxes();
            leg3FreeFluxes = extensions.isLeg3FreeFluxes();
            leg1ConnectionType = extensions.getLeg1ConnectionType();
            leg2ConnectionType = extensions.getLeg2ConnectionType();
            leg3ConnectionType = extensions.getLeg3ConnectionType();
        }

        ShortCircuitTransformerLeg leg1 = new ShortCircuitTransformerLeg(leg1ConnectionType, leg1CoeffRo, leg1CoeffXo, leg1FreeFluxes); // TODO : check if default connection acceptable
        ShortCircuitTransformerLeg leg2 = new ShortCircuitTransformerLeg(leg2ConnectionType, leg2CoeffRo, leg2CoeffXo, leg2FreeFluxes); // TODO : check if default connection acceptable
        ShortCircuitTransformerLeg leg3 = new ShortCircuitTransformerLeg(leg3ConnectionType, leg3CoeffRo, leg3CoeffXo, leg3FreeFluxes); // TODO : check if default connection acceptable

        lfBranch.setProperty(PROPERTY_NAME, new ShortCircuitT3W(leg1, leg2, leg3));
    }

    private static void addLineExtension(Network network, LfBranch lfBranch) {
        String lineId = lfBranch.getOriginalIds().get(0);
        Line line = network.getLine(lineId);

        double coeffRo = LineShortCircuitAdder.DEFAULT_COEFF_RO;
        double coeffXo = LineShortCircuitAdder.DEFAULT_COEFF_XO;
        LineShortCircuit extensions = line.getExtension(LineShortCircuit.class);
        if (extensions != null) {
            coeffRo = extensions.getCoeffRo();
            coeffXo = extensions.getCoeffXo();
        }

        lfBranch.setProperty(PROPERTY_NAME, new ShortCircuitLine(coeffRo, coeffXo));
    }

    private static void addTransfo2Extension(Network network, LfBranch lfBranch) {
        String t2wId = lfBranch.getOriginalIds().get(0);
        TwoWindingsTransformer twt = network.getTwoWindingsTransformer(t2wId);

        double coeffRo = TwoWindingsTransformerShortCircuitAdder.DEFAULT_COEFF_RO;
        double coeffXo = TwoWindingsTransformerShortCircuitAdder.DEFAULT_COEFF_XO;
        boolean freeFluxes = TwoWindingsTransformerShortCircuitAdder.DEFAULT_FREE_FLUXES;
        LegConnectionType leg1ConnectionType = TwoWindingsTransformerShortCircuitAdder.DEFAULT_LEG1_CONNECTION_TYPE;
        LegConnectionType leg2ConnectionType = TwoWindingsTransformerShortCircuitAdder.DEFAULT_LEG2_CONNECTION_TYPE;
        var extensions = twt.getExtension(TwoWindingsTransformerShortCircuit.class);
        if (extensions != null) {
            coeffRo = extensions.getCoeffRo();
            coeffXo = extensions.getCoeffXo();
            freeFluxes = extensions.isFreeFluxes();
            leg1ConnectionType = extensions.getLeg1ConnectionType();
            leg2ConnectionType = extensions.getLeg2ConnectionType();
        }

        lfBranch.setProperty(PROPERTY_NAME, new ShortCircuitT2W(leg1ConnectionType, leg2ConnectionType, coeffRo, coeffXo, freeFluxes));
    }

    private static void addGeneratorExtension(Network network, LfGenerator lfGenerator) {
        Generator generator = network.getGenerator(lfGenerator.getOriginalId());

        GeneratorShortCircuit extension = generator.getExtension(GeneratorShortCircuit.class);
        if (extension == null) { // TODO: use a default value if extension is missing
            throw new PowsyblException("Short circuit extension not found for generator '" + generator.getId() + "'");
        }
        double transX = extension.getDirectTransX();
        double subtransX = extension.getDirectSubtransX();
        double stepUpTfoX = extension.getStepUpTransformerX();

        double transRd = GeneratorShortCircuitAdder2.DEFAULT_TRANS_RD;
        double subTransRd = GeneratorShortCircuitAdder2.DEFAULT_SUB_TRANS_RD;
        boolean toGround = GeneratorShortCircuitAdder2.DEFAULT_TO_GROUND;
        double coeffRo = GeneratorShortCircuitAdder2.DEFAULT_COEFF_RO;
        double coeffXo = GeneratorShortCircuitAdder2.DEFAULT_COEFF_XO;
        GeneratorShortCircuit2 extensions2 = generator.getExtension(GeneratorShortCircuit2.class);
        if (extensions2 != null) {
            transRd = extensions2.getTransRd();
            subTransRd = extensions2.getSubTransRd();
            toGround = extensions2.isToGround();
            coeffRo = extensions2.getCoeffRo();
            coeffXo = extensions2.getCoeffXo();
        }

        lfGenerator.setProperty(PROPERTY_NAME, new ShortCircuitGenerator(transX,
                                                                         stepUpTfoX,
                                                                         ShortCircuitGenerator.MachineType.SYNCHRONOUS_GEN,
                                                                         transRd,
                                                                         0.,
                                                                         subTransRd,
                                                                         subtransX,
                                                                         toGround,
                                                                         0.,
                                                                         0.,
                                                                         coeffRo,
                                                                         coeffXo)); // TODO: set the right type when info available
    }
}
