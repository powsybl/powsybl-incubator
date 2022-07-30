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
                }
            }
        }
    }

    private static void addTransfo3Extension(AdditionalDataInfo additionalDataInfo, LfBranch lfBranch) {
        String idT3w = lfBranch.getOriginalIds().get(0);

        double coeffLeg1Ro = 1.0;
        if (additionalDataInfo.getTfo3wIdToLeg1CoeffRo() != null) {
            if (additionalDataInfo.getTfo3wIdToLeg1CoeffRo().containsKey(idT3w)) {
                coeffLeg1Ro = additionalDataInfo.getTfo3wIdToLeg1CoeffRo().get(idT3w);
            }
        }
        double coeffLeg1Xo = 1.0;
        if (additionalDataInfo.getTfo3wIdToLeg1CoeffXo() != null) {
            if (additionalDataInfo.getTfo3wIdToLeg1CoeffXo().containsKey(idT3w)) {
                coeffLeg1Xo = additionalDataInfo.getTfo3wIdToLeg1CoeffXo().get(idT3w);
            }
        }

        double coeffLeg2Ro = 1.0;
        if (additionalDataInfo.getTfo3wIdToLeg2CoeffRo() != null) {
            if (additionalDataInfo.getTfo3wIdToLeg2CoeffRo().containsKey(idT3w)) {
                coeffLeg2Ro = additionalDataInfo.getTfo3wIdToLeg2CoeffRo().get(idT3w);
            }
        }
        double coeffLeg2Xo = 1.0;
        if (additionalDataInfo.getTfo3wIdToLeg2CoeffXo() != null) {
            if (additionalDataInfo.getTfo3wIdToLeg2CoeffXo().containsKey(idT3w)) {
                coeffLeg2Xo = additionalDataInfo.getTfo3wIdToLeg2CoeffXo().get(idT3w);
            }
        }

        double coeffLeg3Ro = 1.0;
        if (additionalDataInfo.getTfo3wIdToLeg3CoeffRo() != null) {
            if (additionalDataInfo.getTfo3wIdToLeg3CoeffRo().containsKey(idT3w)) {
                coeffLeg3Ro = additionalDataInfo.getTfo3wIdToLeg3CoeffRo().get(idT3w);
            }
        }
        double coeffLeg3Xo = 1.0;
        if (additionalDataInfo.getTfo3wIdToLeg3CoeffXo() != null) {
            if (additionalDataInfo.getTfo3wIdToLeg3CoeffXo().containsKey(idT3w)) {
                coeffLeg3Xo = additionalDataInfo.getTfo3wIdToLeg3CoeffXo().get(idT3w);
            }
        }

        boolean isLeg1FreeFluxes = false;
        boolean isLeg2FreeFluxes = false;
        boolean isLeg3FreeFluxes = false;
        if (additionalDataInfo.getTfo3wIdLeg1ToFreeFluxes() != null) {
            if (additionalDataInfo.getTfo3wIdLeg1ToFreeFluxes().containsKey(idT3w)) {
                isLeg1FreeFluxes = additionalDataInfo.getTfo3wIdLeg1ToFreeFluxes().get(idT3w);
            }
        }
        if (additionalDataInfo.getTfo3wIdLeg2ToFreeFluxes() != null) {
            if (additionalDataInfo.getTfo3wIdLeg2ToFreeFluxes().containsKey(idT3w)) {
                isLeg2FreeFluxes = additionalDataInfo.getTfo3wIdLeg2ToFreeFluxes().get(idT3w);
            }
        }
        if (additionalDataInfo.getTfo3wIdLeg3ToFreeFluxes() != null) {
            if (additionalDataInfo.getTfo3wIdLeg3ToFreeFluxes().containsKey(idT3w)) {
                isLeg3FreeFluxes = additionalDataInfo.getTfo3wIdLeg3ToFreeFluxes().get(idT3w);
            }
        }

        ShortCircuitNetworkTransformerLeg leg1 = new ShortCircuitNetworkTransformerLeg(ShortCircuitNetworkTransformerLeg.LegConnectionType.DELTA, coeffLeg1Ro, coeffLeg1Xo, isLeg1FreeFluxes); // TODO : check if default connection acceptable
        ShortCircuitNetworkTransformerLeg leg2 = new ShortCircuitNetworkTransformerLeg(ShortCircuitNetworkTransformerLeg.LegConnectionType.Y_GROUNDED, coeffLeg2Ro, coeffLeg2Xo, isLeg2FreeFluxes); // TODO : check if default connection acceptable
        ShortCircuitNetworkTransformerLeg leg3 = new ShortCircuitNetworkTransformerLeg(ShortCircuitNetworkTransformerLeg.LegConnectionType.DELTA, coeffLeg3Ro, coeffLeg3Xo, isLeg3FreeFluxes);

        if (additionalDataInfo.getLeg1type() != null) {
            if (additionalDataInfo.getLeg1type().containsKey(idT3w)) {
                AdditionalDataInfo.LegType legType = additionalDataInfo.getLeg1type().get(idT3w);
                if (legType == AdditionalDataInfo.LegType.Y) {
                    leg1.setLegConnectionType(ShortCircuitNetworkTransformerLeg.LegConnectionType.Y);
                } else if (legType == AdditionalDataInfo.LegType.Y_GROUNDED) {
                    leg1.setLegConnectionType(ShortCircuitNetworkTransformerLeg.LegConnectionType.Y_GROUNDED);
                }
            }
        }
        if (additionalDataInfo.getLeg2type() != null) {
            if (additionalDataInfo.getLeg2type().containsKey(idT3w)) {
                AdditionalDataInfo.LegType legType = additionalDataInfo.getLeg2type().get(idT3w);
                if (legType == AdditionalDataInfo.LegType.Y) {
                    leg2.setLegConnectionType(ShortCircuitNetworkTransformerLeg.LegConnectionType.Y);
                } else if (legType == AdditionalDataInfo.LegType.DELTA) {
                    leg2.setLegConnectionType(ShortCircuitNetworkTransformerLeg.LegConnectionType.DELTA);
                }
            }
        }
        if (additionalDataInfo.getLeg3type() != null) {
            if (additionalDataInfo.getLeg3type().containsKey(idT3w)) {
                AdditionalDataInfo.LegType legType = additionalDataInfo.getLeg3type().get(idT3w);
                if (legType == AdditionalDataInfo.LegType.Y) {
                    leg3.setLegConnectionType(ShortCircuitNetworkTransformerLeg.LegConnectionType.Y);
                } else if (legType == AdditionalDataInfo.LegType.Y_GROUNDED) {
                    leg3.setLegConnectionType(ShortCircuitNetworkTransformerLeg.LegConnectionType.Y_GROUNDED);
                }
            }
        }

        ShortCircuitNetworkT3W shortCircuitNetworkT3W = new ShortCircuitNetworkT3W(leg1, leg2, leg3);
        lfBranch.setProperty(PROPERTY_NAME, shortCircuitNetworkT3W);
    }

    private static void addLineExtension(AdditionalDataInfo additionalDataInfo, LfBranch lfBranch) {
        String idLine = lfBranch.getOriginalIds().get(0);

        double coeffRo = 1.0;
        if (additionalDataInfo.getLineIdToCoeffRo() != null) {
            if (additionalDataInfo.getLineIdToCoeffRo().containsKey(idLine)) {
                coeffRo = additionalDataInfo.getLineIdToCoeffRo().get(idLine);
            }
        }

        double coeffXo = 1.0;
        if (additionalDataInfo.getLineIdToCoeffXo() != null) {
            if (additionalDataInfo.getLineIdToCoeffXo().containsKey(idLine)) {
                coeffXo = additionalDataInfo.getLineIdToCoeffXo().get(idLine);
            }
        }
        ShortCircuitNetworkLine shortCircuitNetworkLine = new ShortCircuitNetworkLine(coeffRo, coeffXo);
        lfBranch.setProperty(PROPERTY_NAME, shortCircuitNetworkLine);
    }

    private static void addTransfo2Extension(AdditionalDataInfo additionalDataInfo, LfBranch lfBranch) {
        String idT2w = lfBranch.getOriginalIds().get(0);

        double coeffRo = 1.0;
        if (additionalDataInfo.getTfo2wIdToCoeffRo() != null) {
            if (additionalDataInfo.getTfo2wIdToCoeffRo().containsKey(idT2w)) {
                coeffRo = additionalDataInfo.getTfo2wIdToCoeffRo().get(idT2w);
            }
        }
        double coeffXo = 1.0;
        if (additionalDataInfo.getTfo2wIdToCoeffXo() != null) {
            if (additionalDataInfo.getTfo2wIdToCoeffXo().containsKey(idT2w)) {
                coeffXo = additionalDataInfo.getTfo2wIdToCoeffXo().get(idT2w);
            }
        }

        ShortCircuitNetworkTransformerLeg leg1 = new ShortCircuitNetworkTransformerLeg(ShortCircuitNetworkTransformerLeg.LegConnectionType.DELTA); // TODO : check if default connection acceptable
        ShortCircuitNetworkTransformerLeg leg2 = new ShortCircuitNetworkTransformerLeg(ShortCircuitNetworkTransformerLeg.LegConnectionType.Y_GROUNDED); // TODO : check if default connection acceptable

        if (additionalDataInfo.getLeg1type() != null) {
            if (additionalDataInfo.getLeg1type().containsKey(idT2w)) {
                AdditionalDataInfo.LegType legType = additionalDataInfo.getLeg1type().get(idT2w);
                if (legType == AdditionalDataInfo.LegType.Y) {
                    leg1.setLegConnectionType(ShortCircuitNetworkTransformerLeg.LegConnectionType.Y);
                } else if (legType == AdditionalDataInfo.LegType.Y_GROUNDED) {
                    leg1.setLegConnectionType(ShortCircuitNetworkTransformerLeg.LegConnectionType.Y_GROUNDED);
                }
            }
        }
        if (additionalDataInfo.getLeg2type() != null) {
            if (additionalDataInfo.getLeg2type().containsKey(idT2w)) {
                AdditionalDataInfo.LegType legType = additionalDataInfo.getLeg2type().get(idT2w);
                if (legType == AdditionalDataInfo.LegType.Y) {
                    leg2.setLegConnectionType(ShortCircuitNetworkTransformerLeg.LegConnectionType.Y);
                } else if (legType == AdditionalDataInfo.LegType.DELTA) {
                    leg2.setLegConnectionType(ShortCircuitNetworkTransformerLeg.LegConnectionType.DELTA);
                }
            }
        }

        boolean isFreeFluxes = false;
        if (additionalDataInfo.getTfo2wIdToFreeFluxes() != null) {
            if (additionalDataInfo.getTfo2wIdToFreeFluxes().containsKey(idT2w)) {
                isFreeFluxes = additionalDataInfo.getTfo2wIdToFreeFluxes().get(idT2w);
            }
        }

        ShortCircuitNetworkT2W shortCircuitNetworkT2W = new ShortCircuitNetworkT2W(leg1, leg2, coeffRo, coeffXo, isFreeFluxes);
        lfBranch.setProperty(PROPERTY_NAME, shortCircuitNetworkT2W);
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

        double transRd = 0.;
        if (additionalDataInfo.getMachineIdToTransRd().containsKey(generator.getId())) {
            transRd = additionalDataInfo.getMachineIdToTransRd().get(generator.getId());
        }

        double subTransRd = 0.;
        if (additionalDataInfo.getMachineIdToSubTransRd().containsKey(generator.getId())) {
            subTransRd = additionalDataInfo.getMachineIdToSubTransRd().get(generator.getId());
        }

        boolean isToGround = false;
        if (additionalDataInfo.getMachineToGround().containsKey(generator.getId())) {
            isToGround = additionalDataInfo.getMachineToGround().get(generator.getId());
        }

        double coeffRo = 1.;
        if (additionalDataInfo.getMachineCoeffRo() != null) {
            if (additionalDataInfo.getMachineCoeffRo().containsKey(generator.getId())) {
                coeffRo = additionalDataInfo.getMachineCoeffRo().get(generator.getId());
            }
        }

        double coeffXo = 1.;
        if (additionalDataInfo.getMachineCoeffXo() != null) {
            if (additionalDataInfo.getMachineCoeffXo().containsKey(generator.getId())) {
                coeffXo = additionalDataInfo.getMachineCoeffXo().get(generator.getId());
            }
        }

        ShortCircuitNetworkMachineInfo rotatingMachineInfo = new ShortCircuitNetworkMachineInfo(transX, stepUpTfoX, ShortCircuitNetworkMachineInfo.MachineType.SYNCHRONOUS_GEN, transRd, 0., subTransRd, subtransX, isToGround, 0., 0., coeffRo, coeffXo); // TODO: set the right type when info available
        lfGenerator.setProperty(PROPERTY_NAME, rotatingMachineInfo);
    }
}
