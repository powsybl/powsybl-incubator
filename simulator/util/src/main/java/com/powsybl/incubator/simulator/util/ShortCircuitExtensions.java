/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.GeneratorShortCircuit;
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
                    addGeneratorExtension(network, additionalDataInfo, lfGenerator);
                }
            }

            for (LfBranch lfBranch : lfNetwork.getBranches()) {
                switch (lfBranch.getBranchType()) {
                    case LINE:
                        addLineExtension(additionalDataInfo, lfBranch);
                        break;

                    case TRANSFO_2:
                        addTransfo2Extension(additionalDataInfo, lfBranch);
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

        boolean isLeg1FreeFluxes = getParameterValue(additionalDataInfo.getTfo3wIdLeg1ToFreeFluxes(), t3wId, false);
        boolean isLeg2FreeFluxes = getParameterValue(additionalDataInfo.getTfo3wIdLeg2ToFreeFluxes(), t3wId, false);
        boolean isLeg3FreeFluxes = getParameterValue(additionalDataInfo.getTfo3wIdLeg3ToFreeFluxes(), t3wId, false);

        ShortCircuitNetworkTransformerLeg leg1 = new ShortCircuitNetworkTransformerLeg(ShortCircuitNetworkTransformerLeg.LegConnectionType.DELTA, coeffLeg1Ro, coeffLeg1Xo, isLeg1FreeFluxes); // TODO : check if default connection acceptable
        ShortCircuitNetworkTransformerLeg leg2 = new ShortCircuitNetworkTransformerLeg(ShortCircuitNetworkTransformerLeg.LegConnectionType.Y_GROUNDED, coeffLeg2Ro, coeffLeg2Xo, isLeg2FreeFluxes); // TODO : check if default connection acceptable
        ShortCircuitNetworkTransformerLeg leg3 = new ShortCircuitNetworkTransformerLeg(ShortCircuitNetworkTransformerLeg.LegConnectionType.DELTA, coeffLeg3Ro, coeffLeg3Xo, isLeg3FreeFluxes);

        AdditionalDataInfo.LegType leg1Type = getParameterValue(additionalDataInfo.getLeg1type(), t3wId, null);
        if (leg1Type == AdditionalDataInfo.LegType.Y) {
            leg1.setLegConnectionType(ShortCircuitNetworkTransformerLeg.LegConnectionType.Y);
        } else if (leg1Type == AdditionalDataInfo.LegType.Y_GROUNDED) {
            leg1.setLegConnectionType(ShortCircuitNetworkTransformerLeg.LegConnectionType.Y_GROUNDED);
        }

        AdditionalDataInfo.LegType leg2Type = getParameterValue(additionalDataInfo.getLeg2type(), t3wId, null);
        if (leg2Type == AdditionalDataInfo.LegType.Y) {
            leg2.setLegConnectionType(ShortCircuitNetworkTransformerLeg.LegConnectionType.Y);
        } else if (leg2Type == AdditionalDataInfo.LegType.DELTA) {
            leg2.setLegConnectionType(ShortCircuitNetworkTransformerLeg.LegConnectionType.DELTA);
        }

        AdditionalDataInfo.LegType leg3Type = getParameterValue(additionalDataInfo.getLeg3type(), t3wId, null);
        if (leg3Type == AdditionalDataInfo.LegType.Y) {
            leg3.setLegConnectionType(ShortCircuitNetworkTransformerLeg.LegConnectionType.Y);
        } else if (leg3Type == AdditionalDataInfo.LegType.Y_GROUNDED) {
            leg3.setLegConnectionType(ShortCircuitNetworkTransformerLeg.LegConnectionType.Y_GROUNDED);
        }

        lfBranch.setProperty(PROPERTY_NAME, new ShortCircuitNetworkT3W(leg1, leg2, leg3));
    }

    private static void addLineExtension(AdditionalDataInfo additionalDataInfo, LfBranch lfBranch) {
        String lineId = lfBranch.getOriginalIds().get(0);

        double coeffRo = getParameterValue(additionalDataInfo.getLineIdToCoeffRo(), lineId, 1.0);
        double coeffXo = getParameterValue(additionalDataInfo.getLineIdToCoeffXo(), lineId, 1.0);

        lfBranch.setProperty(PROPERTY_NAME, new ShortCircuitNetworkLine(coeffRo, coeffXo));
    }

    private static void addTransfo2Extension(AdditionalDataInfo additionalDataInfo, LfBranch lfBranch) {
        String t2wId = lfBranch.getOriginalIds().get(0);

        double coeffRo = getParameterValue(additionalDataInfo.getTfo2wIdToCoeffRo(), t2wId, 1.0);
        double coeffXo = getParameterValue(additionalDataInfo.getTfo2wIdToCoeffXo(), t2wId, 1.0);

        ShortCircuitNetworkTransformerLeg leg1 = new ShortCircuitNetworkTransformerLeg(ShortCircuitNetworkTransformerLeg.LegConnectionType.DELTA); // TODO : check if default connection acceptable
        ShortCircuitNetworkTransformerLeg leg2 = new ShortCircuitNetworkTransformerLeg(ShortCircuitNetworkTransformerLeg.LegConnectionType.Y_GROUNDED); // TODO : check if default connection acceptable

        AdditionalDataInfo.LegType leg1Type = getParameterValue(additionalDataInfo.getLeg1type(), t2wId, null);
        if (leg1Type == AdditionalDataInfo.LegType.Y) {
            leg1.setLegConnectionType(ShortCircuitNetworkTransformerLeg.LegConnectionType.Y);
        } else if (leg1Type == AdditionalDataInfo.LegType.Y_GROUNDED) {
            leg1.setLegConnectionType(ShortCircuitNetworkTransformerLeg.LegConnectionType.Y_GROUNDED);
        }

        AdditionalDataInfo.LegType leg2Type = getParameterValue(additionalDataInfo.getLeg2type(), t2wId, null);
        if (leg2Type == AdditionalDataInfo.LegType.Y) {
            leg2.setLegConnectionType(ShortCircuitNetworkTransformerLeg.LegConnectionType.Y);
        } else if (leg2Type == AdditionalDataInfo.LegType.DELTA) {
            leg2.setLegConnectionType(ShortCircuitNetworkTransformerLeg.LegConnectionType.DELTA);
        }

        boolean freeFluxes = getParameterValue(additionalDataInfo.getTfo2wIdToFreeFluxes(), t2wId, false);

        lfBranch.setProperty(PROPERTY_NAME, new ShortCircuitNetworkT2W(leg1, leg2, coeffRo, coeffXo, freeFluxes));
    }

    private static void addGeneratorExtension(Network network, AdditionalDataInfo additionalDataInfo, LfGenerator lfGenerator) {
        Generator generator = network.getGenerator(lfGenerator.getOriginalId());

        GeneratorShortCircuit extension = generator.getExtension(GeneratorShortCircuit.class);
        if (extension == null) { // TODO: use a default value if extension is missing
            throw new PowsyblException("Short circuit extension not found for generator '" + generator.getId() + "'");
        }
        double transX = extension.getDirectTransX();
        double subtransX = extension.getDirectSubtransX();
        double stepUpTfoX = extension.getStepUpTransformerX();

        double transRd = getParameterValue(additionalDataInfo.getMachineIdToTransRd(), generator.getId(), 0.);
        double subTransRd = getParameterValue(additionalDataInfo.getMachineIdToSubTransRd(), generator.getId(), 0.);
        boolean toGround = getParameterValue(additionalDataInfo.getMachineToGround(), generator.getId(), false);
        double coeffRo = getParameterValue(additionalDataInfo.getMachineCoeffRo(), generator.getId(), 1.);
        double coeffXo = getParameterValue(additionalDataInfo.getMachineCoeffXo(), generator.getId(), 1.);

        lfGenerator.setProperty(PROPERTY_NAME, new ShortCircuitNetworkMachineInfo(transX,
                                                                                  stepUpTfoX,
                                                                                  ShortCircuitNetworkMachineInfo.MachineType.SYNCHRONOUS_GEN,
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
