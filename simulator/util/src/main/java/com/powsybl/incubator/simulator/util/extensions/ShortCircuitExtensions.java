/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util.extensions;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.iidm.network.extensions.GeneratorShortCircuit;
import com.powsybl.incubator.simulator.util.extensions.iidm.*;
import com.powsybl.openloadflow.network.LfBranch;
import com.powsybl.openloadflow.network.LfBus;
import com.powsybl.openloadflow.network.LfGenerator;
import com.powsybl.openloadflow.network.LfNetwork;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public final class ShortCircuitExtensions {

    public static final String PROPERTY_NAME = "ShortCircuit";

    private ShortCircuitExtensions() {
    }

    public static void add(Network network, List<LfNetwork> lfNetworks, AdditionalDataInfo additionalDataInfo) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(lfNetworks);
        Objects.requireNonNull(additionalDataInfo);
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
                        addTransfo3Extension(additionalDataInfo, lfBranch);
                        break;

                    case DANGLING_LINE:
                        // FIXME something to do?
                        break;
                }
            }
        }
    }

    private static <T> T getParameterValue(Map<String, T> parameters, String id, T defaultValue) {
        if (parameters != null) {
            if (parameters.containsKey(id)) {
                return parameters.get(id);
            }
        }
        return defaultValue;
    }

    private static void addTransfo3Extension(AdditionalDataInfo additionalDataInfo, LfBranch lfBranch) {
        String t3wId = lfBranch.getOriginalIds().get(0);

        double coeffLeg1Ro = getParameterValue(additionalDataInfo.getTfo3wIdToLeg1CoeffRo(), t3wId, 1.0);
        double coeffLeg1Xo = getParameterValue(additionalDataInfo.getTfo3wIdToLeg1CoeffXo(), t3wId, 1.0);

        double coeffLeg2Ro = getParameterValue(additionalDataInfo.getTfo3wIdToLeg2CoeffRo(), t3wId, 1.0);
        double coeffLeg2Xo = getParameterValue(additionalDataInfo.getTfo3wIdToLeg2CoeffXo(), t3wId, 1.0);

        double coeffLeg3Ro = getParameterValue(additionalDataInfo.getTfo3wIdToLeg3CoeffRo(), t3wId, 1.0);
        double coeffLeg3Xo = getParameterValue(additionalDataInfo.getTfo3wIdToLeg3CoeffXo(), t3wId, 1.0);

        boolean leg1FreeFluxes = getParameterValue(additionalDataInfo.getTfo3wIdLeg1ToFreeFluxes(), t3wId, false);
        boolean leg2FreeFluxes = getParameterValue(additionalDataInfo.getTfo3wIdLeg2ToFreeFluxes(), t3wId, false);
        boolean leg3FreeFluxes = getParameterValue(additionalDataInfo.getTfo3wIdLeg3ToFreeFluxes(), t3wId, false);

        ShortCircuitTransformerLeg leg1 = new ShortCircuitTransformerLeg(LegConnectionType.DELTA, coeffLeg1Ro, coeffLeg1Xo, leg1FreeFluxes); // TODO : check if default connection acceptable
        ShortCircuitTransformerLeg leg2 = new ShortCircuitTransformerLeg(LegConnectionType.Y_GROUNDED, coeffLeg2Ro, coeffLeg2Xo, leg2FreeFluxes); // TODO : check if default connection acceptable
        ShortCircuitTransformerLeg leg3 = new ShortCircuitTransformerLeg(LegConnectionType.DELTA, coeffLeg3Ro, coeffLeg3Xo, leg3FreeFluxes);

        LegConnectionType leg1Type = getParameterValue(additionalDataInfo.getLeg1type(), t3wId, null);
        leg1.setLegConnectionType(leg1Type);

        LegConnectionType leg2Type = getParameterValue(additionalDataInfo.getLeg2type(), t3wId, null);
        leg2.setLegConnectionType(leg2Type);

        LegConnectionType leg3Type = getParameterValue(additionalDataInfo.getLeg3type(), t3wId, null);
        leg3.setLegConnectionType(leg3Type);

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
