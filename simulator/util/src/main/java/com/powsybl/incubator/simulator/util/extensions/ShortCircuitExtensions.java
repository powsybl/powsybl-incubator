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

import static com.powsybl.incubator.simulator.util.extensions.iidm.ShortCircuitConstants.*;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public final class ShortCircuitExtensions {

    public static final String PROPERTY_SHORT_CIRCUIT = "ShortCircuit";
    public static final String PROPERTY_HOMOPOLAR_MODEL = "HomopolarModel";

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

        ScTransfo3W.Leg leg1 = new ScTransfo3W.Leg(leg1ConnectionType, leg1CoeffRo, leg1CoeffXo, leg1FreeFluxes); // TODO : check if default connection acceptable
        ScTransfo3W.Leg leg2 = new ScTransfo3W.Leg(leg2ConnectionType, leg2CoeffRo, leg2CoeffXo, leg2FreeFluxes); // TODO : check if default connection acceptable
        ScTransfo3W.Leg leg3 = new ScTransfo3W.Leg(leg3ConnectionType, leg3CoeffRo, leg3CoeffXo, leg3FreeFluxes); // TODO : check if default connection acceptable

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
        var extensions = twt.getExtension(TwoWindingsTransformerShortCircuit.class);
        if (extensions != null) {
            coeffRo = extensions.getCoeffRo();
            coeffXo = extensions.getCoeffXo();
            freeFluxes = extensions.isFreeFluxes();
            leg1ConnectionType = extensions.getLeg1ConnectionType();
            leg2ConnectionType = extensions.getLeg2ConnectionType();
        }

        lfBranch.setProperty(PROPERTY_SHORT_CIRCUIT, new ScTransfo2W(leg1ConnectionType, leg2ConnectionType, coeffRo, coeffXo, freeFluxes));
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

        double transRd = DEFAULT_TRANS_RD;
        double subTransRd = DEFAULT_SUB_TRANS_RD;
        boolean toGround = DEFAULT_TO_GROUND;
        double coeffRo = DEFAULT_COEFF_RO;
        double coeffXo = DEFAULT_COEFF_XO;
        GeneratorShortCircuit2 extensions2 = generator.getExtension(GeneratorShortCircuit2.class);
        if (extensions2 != null) {
            transRd = extensions2.getTransRd();
            subTransRd = extensions2.getSubTransRd();
            toGround = extensions2.isToGround();
            coeffRo = extensions2.getCoeffRo();
            coeffXo = extensions2.getCoeffXo();
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
                                                                        coeffXo)); // TODO: set the right type when info available
    }
}
