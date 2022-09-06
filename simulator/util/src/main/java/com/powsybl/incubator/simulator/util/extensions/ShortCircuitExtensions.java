/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util.extensions;

import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.GeneratorShortCircuit;
import com.powsybl.incubator.simulator.util.extensions.iidm.*;
import com.powsybl.openloadflow.network.LfBranch;
import com.powsybl.openloadflow.network.LfBus;
import com.powsybl.openloadflow.network.LfGenerator;
import com.powsybl.openloadflow.network.LfNetwork;

import java.util.List;
import java.util.Objects;

import static com.powsybl.incubator.simulator.util.extensions.iidm.ShortCircuitConstants.*;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public final class ShortCircuitExtensions {

    public static final String PROPERTY_SHORT_CIRCUIT = "ShortCircuit";
    public static final String PROPERTY_HOMOPOLAR_MODEL = "HomopolarModel";

    private static final double SB = 100.;

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

                    default:
                        break;
                }

                // build homopolar model
                HomopolarModel homopolarModel = HomopolarModel.build(lfBranch);
                lfBranch.setProperty(PROPERTY_HOMOPOLAR_MODEL, homopolarModel);
            }
        }
    }

    private static void addTransfo3Extension(Network network, LfBranch lfBranch) {
        String t3wId = lfBranch.getOriginalIds().get(0);
        ThreeWindingsTransformer twt = network.getThreeWindingsTransformer(t3wId);

        double leg1CoeffRo = DEFAULT_COEFF_RO;
        double leg2CoeffRo = DEFAULT_COEFF_RO;
        double leg3CoeffRo = DEFAULT_COEFF_RO;
        double leg1CoeffXo = DEFAULT_COEFF_XO;
        double leg2CoeffXo = DEFAULT_COEFF_XO;
        double leg3CoeffXo = DEFAULT_COEFF_XO;
        boolean leg1FreeFluxes = DEFAULT_FREE_FLUXES;
        boolean leg2FreeFluxes = DEFAULT_FREE_FLUXES;
        boolean leg3FreeFluxes = DEFAULT_FREE_FLUXES;
        LegConnectionType leg1ConnectionType = DEFAULT_LEG1_CONNECTION_TYPE;
        LegConnectionType leg2ConnectionType = DEFAULT_LEG2_CONNECTION_TYPE;
        LegConnectionType leg3ConnectionType = DEFAULT_LEG3_CONNECTION_TYPE;
        double kT1R = DEFAULT_COEFF_K;
        double kT1X = DEFAULT_COEFF_K;
        double kT2R = DEFAULT_COEFF_K;
        double kT2X = DEFAULT_COEFF_K;
        double kT3R = DEFAULT_COEFF_K;
        double kT3X = DEFAULT_COEFF_K;
        double kT1R0 = DEFAULT_COEFF_K;
        double kT1X0 = DEFAULT_COEFF_K;
        double kT2R0 = DEFAULT_COEFF_K;
        double kT2X0 = DEFAULT_COEFF_K;
        double kT3R0 = DEFAULT_COEFF_K;
        double kT3X0 = DEFAULT_COEFF_K;

        var extensions = twt.getExtension(ThreeWindingsTransformerShortCircuit.class);
        if (extensions != null) {
            leg1CoeffRo = extensions.getLeg1().getLegCoeffRo();
            leg2CoeffRo = extensions.getLeg2().getLegCoeffRo();
            leg3CoeffRo = extensions.getLeg3().getLegCoeffRo();
            leg1CoeffXo = extensions.getLeg1().getLegCoeffXo();
            leg2CoeffXo = extensions.getLeg2().getLegCoeffXo();
            leg3CoeffXo = extensions.getLeg3().getLegCoeffXo();
            leg1FreeFluxes = extensions.getLeg1().isLegFreeFluxes();
            leg2FreeFluxes = extensions.getLeg2().isLegFreeFluxes();
            leg3FreeFluxes = extensions.getLeg3().isLegFreeFluxes();
            leg1ConnectionType = extensions.getLeg1().getLegConnectionType();
            leg2ConnectionType = extensions.getLeg2().getLegConnectionType();
            leg3ConnectionType = extensions.getLeg3().getLegConnectionType();
            kT1R = extensions.getLeg1().getKtR();
            kT1X = extensions.getLeg1().getKtX();
            kT2R = extensions.getLeg2().getKtR();
            kT2X = extensions.getLeg2().getKtX();
            kT3R = extensions.getLeg3().getKtR();
            kT3X = extensions.getLeg3().getKtX();
            kT1R0 = extensions.getLeg1().getKtRo();
            kT1X0 = extensions.getLeg1().getKtXo();
            kT2R0 = extensions.getLeg2().getKtRo();
            kT2X0 = extensions.getLeg2().getKtXo();
            kT3R0 = extensions.getLeg3().getKtRo();
            kT3X0 = extensions.getLeg3().getKtXo();
        }

        ScTransfo3W.Leg leg1 = new ScTransfo3W.Leg(leg1ConnectionType, leg1CoeffRo, leg1CoeffXo, kT1R, kT1X, kT1R0, kT1X0, leg1FreeFluxes); // TODO : check if default connection acceptable
        ScTransfo3W.Leg leg2 = new ScTransfo3W.Leg(leg2ConnectionType, leg2CoeffRo, leg2CoeffXo, kT2R, kT2X, kT2R0, kT2X0, leg2FreeFluxes); // TODO : check if default connection acceptable
        ScTransfo3W.Leg leg3 = new ScTransfo3W.Leg(leg3ConnectionType, leg3CoeffRo, leg3CoeffXo, kT3R, kT3X, kT3R0, kT3X0, leg3FreeFluxes); // TODO : check if default connection acceptable

        lfBranch.setProperty(PROPERTY_SHORT_CIRCUIT, new ScTransfo3W(leg1, leg2, leg3));
    }

    private static void addLineExtension(Network network, LfBranch lfBranch) {
        String lineId = lfBranch.getOriginalIds().get(0);
        Line line = network.getLine(lineId);

        double coeffRo = DEFAULT_COEFF_RO;
        double coeffXo = DEFAULT_COEFF_XO;
        LineShortCircuit extensions = line.getExtension(LineShortCircuit.class);
        if (extensions != null) {
            coeffRo = extensions.getCoeffRo();
            coeffXo = extensions.getCoeffXo();
        }

        lfBranch.setProperty(PROPERTY_SHORT_CIRCUIT, new ScLine(coeffRo, coeffXo));
    }

    private static void addTransfo2Extension(Network network, LfBranch lfBranch) {
        String t2wId = lfBranch.getOriginalIds().get(0);
        TwoWindingsTransformer twt = network.getTwoWindingsTransformer(t2wId);

        double coeffRo = DEFAULT_COEFF_RO;
        double coeffXo = DEFAULT_COEFF_XO;
        boolean freeFluxes = DEFAULT_FREE_FLUXES;
        LegConnectionType leg1ConnectionType = DEFAULT_LEG1_CONNECTION_TYPE;
        LegConnectionType leg2ConnectionType = DEFAULT_LEG2_CONNECTION_TYPE;
        double kT = DEFAULT_COEFF_K;
        double r1Ground = 0.;
        double x1Ground = 0.;
        double r2Ground = 0.;
        double x2Ground = 0.;
        var extensions = twt.getExtension(TwoWindingsTransformerShortCircuit.class);
        if (extensions != null) {
            coeffRo = extensions.getCoeffRo();
            coeffXo = extensions.getCoeffXo();
            freeFluxes = extensions.isFreeFluxes();
            leg1ConnectionType = extensions.getLeg1ConnectionType();
            leg2ConnectionType = extensions.getLeg2ConnectionType();
            kT = extensions.getkNorm();

            // per unitizing of grounding Z for LfNetwork
            double u2Nom = twt.getTerminal2().getVoltageLevel().getNominalV();
            double zBase = u2Nom * u2Nom / SB;

            r1Ground = extensions.getR1Ground() / zBase;
            x1Ground = extensions.getX1Ground() / zBase;
            r2Ground = extensions.getR2Ground() / zBase;
            x2Ground = extensions.getX2Ground() / zBase;
        }

        lfBranch.setProperty(PROPERTY_SHORT_CIRCUIT, new ScTransfo2W(leg1ConnectionType, leg2ConnectionType, coeffRo, coeffXo, freeFluxes, kT, r1Ground, x1Ground, r2Ground, x2Ground));
    }

    private static void addGeneratorExtension(Network network, LfGenerator lfGenerator) {
        Generator generator = network.getGenerator(lfGenerator.getOriginalId());

        double transX = DEFAULT_TRANS_XD;
        double subtransX = DEFAULT_SUB_TRANS_XD;
        double stepUpTfoX = DEFAULT_STEP_UP_XD;

        GeneratorShortCircuit extension = generator.getExtension(GeneratorShortCircuit.class);
        if (extension != null) { // TODO: use a default value if extension is missing
            transX = extension.getDirectTransX();
            subtransX = extension.getDirectSubtransX();
            stepUpTfoX = extension.getStepUpTransformerX();
        }

        double transRd = DEFAULT_TRANS_RD;
        double subTransRd = DEFAULT_SUB_TRANS_RD;
        boolean toGround = DEFAULT_TO_GROUND;
        double coeffRo = DEFAULT_COEFF_RO;
        double coeffXo = DEFAULT_COEFF_XO;
        double kG = 1.0;
        GeneratorShortCircuit2 extensions2 = generator.getExtension(GeneratorShortCircuit2.class);
        if (extensions2 != null) {
            transRd = extensions2.getTransRd();
            subTransRd = extensions2.getSubTransRd();
            toGround = extensions2.isToGround();
            coeffRo = extensions2.getCoeffRo();
            coeffXo = extensions2.getCoeffXo();
            kG = extensions2.getkG();
        }

        lfGenerator.setProperty(PROPERTY_SHORT_CIRCUIT, new ScGenerator(transX,
                                                                        stepUpTfoX,
                                                                        ScGenerator.MachineType.SYNCHRONOUS_GEN,
                                                                        transRd,
                                                                        0.,
                                                                        subTransRd,
                                                                        subtransX,
                                                                        toGround,
                                                                        0.,
                                                                        0.,
                                                                        coeffRo,
                                                                        coeffXo, kG)); // TODO: set the right type when info available
    }
}
