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
import com.powsybl.openloadflow.network.*;
import org.apache.commons.math3.util.Pair;

import java.util.List;
import java.util.Objects;

import static com.powsybl.incubator.simulator.util.extensions.iidm.ShortCircuitConstants.*;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public final class ShortCircuitExtensions {

    public static final String PROPERTY_SHORT_CIRCUIT = "ShortCircuit";
    public static final String PROPERTY_HOMOPOLAR_MODEL = "HomopolarModel";
    public static final String PROPERTY_SHORT_CIRCUIT_NORM = "ShortCircuitNorm";

    private static final double SB = 100.;

    private static final double EPSILON = 0.00000001;

    private ShortCircuitExtensions() {
    }

    public static void add(Network network, List<LfNetwork> lfNetworks) {
        add(network, lfNetworks, new ShortCircuitNormExtensions());
    }

    public static void add(Network network, List<LfNetwork> lfNetworks, ShortCircuitNormExtensions shortCircuitNormExtensions) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(lfNetworks);
        for (LfNetwork lfNetwork : lfNetworks) {
            for (LfBus lfBus : lfNetwork.getBuses()) {
                for (LfGenerator lfGenerator : lfBus.getGenerators()) {
                    addGeneratorExtension(network, lfGenerator, shortCircuitNormExtensions);
                }
                addLoadExtension(network, lfBus);
            }

            for (LfBranch lfBranch : lfNetwork.getBranches()) {
                switch (lfBranch.getBranchType()) {
                    case LINE:
                        addLineExtension(network, lfBranch);
                        break;

                    case TRANSFO_2:
                        addTransfo2Extension(network, lfBranch, shortCircuitNormExtensions);
                        break;

                    case TRANSFO_3_LEG_1:
                    case TRANSFO_3_LEG_2:
                    case TRANSFO_3_LEG_3:
                        addTransfo3Extension(network, lfBranch, shortCircuitNormExtensions);
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

    private static void addTransfo3Extension(Network network, LfBranch lfBranch, ShortCircuitNormExtensions shortCircuitNormExtensions) {
        String t3wId = lfBranch.getOriginalIds().get(0);
        ThreeWindingsTransformer twt = network.getThreeWindingsTransformer(t3wId);

        double leg1Ro = twt.getLeg1().getR();
        double leg2Ro = twt.getLeg2().getR();
        double leg3Ro = twt.getLeg3().getR();
        double leg1Xo = twt.getLeg1().getX();
        double leg2Xo = twt.getLeg2().getX();
        double leg3Xo = twt.getLeg3().getX();
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

            leg1Ro = extensions.getLeg1().getLegRo();
            leg2Ro = extensions.getLeg2().getLegRo();
            leg3Ro = extensions.getLeg3().getLegRo();
            leg1Xo = extensions.getLeg1().getLegXo();
            leg2Xo = extensions.getLeg2().getLegXo();
            leg3Xo = extensions.getLeg3().getLegXo();

            leg1FreeFluxes = extensions.getLeg1().isLegFreeFluxes();
            leg2FreeFluxes = extensions.getLeg2().isLegFreeFluxes();
            leg3FreeFluxes = extensions.getLeg3().isLegFreeFluxes();
            leg1ConnectionType = extensions.getLeg1().getLegConnectionType();
            leg2ConnectionType = extensions.getLeg2().getLegConnectionType();
            leg3ConnectionType = extensions.getLeg3().getLegConnectionType();
        }

        ThreeWindingsTransformerNorm t3wExtensionNorm = shortCircuitNormExtensions.getNormExtension(twt);
        if (t3wExtensionNorm != null) {
            if (t3wExtensionNorm.isOverloadHomopolarCoefs()) {

                leg1Ro = twt.getLeg1().getR() * t3wExtensionNorm.getLeg1().getLegCoeffRoOverload();
                leg2Ro = twt.getLeg2().getR() * t3wExtensionNorm.getLeg2().getLegCoeffRoOverload();
                leg3Ro = twt.getLeg3().getR() * t3wExtensionNorm.getLeg3().getLegCoeffRoOverload();
                leg1Xo = twt.getLeg1().getX() * t3wExtensionNorm.getLeg1().getLegCoeffXoOverload();
                leg2Xo = twt.getLeg2().getX() * t3wExtensionNorm.getLeg2().getLegCoeffXoOverload();
                leg3Xo = twt.getLeg3().getX() * t3wExtensionNorm.getLeg3().getLegCoeffXoOverload();
            }

            kT1R = t3wExtensionNorm.getLeg1().getKtR();
            kT1X = t3wExtensionNorm.getLeg1().getKtX();
            kT2R = t3wExtensionNorm.getLeg2().getKtR();
            kT2X = t3wExtensionNorm.getLeg2().getKtX();
            kT3R = t3wExtensionNorm.getLeg3().getKtR();
            kT3X = t3wExtensionNorm.getLeg3().getKtX();
            kT1R0 = t3wExtensionNorm.getLeg1().getKtRo();
            kT1X0 = t3wExtensionNorm.getLeg1().getKtXo();
            kT2R0 = t3wExtensionNorm.getLeg2().getKtRo();
            kT2X0 = t3wExtensionNorm.getLeg2().getKtXo();
            kT3R0 = t3wExtensionNorm.getLeg3().getKtRo();
            kT3X0 = t3wExtensionNorm.getLeg3().getKtXo();
        }

        // building of pu base supposing that impedances are on star bus side (ratedU0)
        double zBase = twt.getRatedU0() * twt.getRatedU0() / SB;

        ScTransfo3W.Leg leg1 = new ScTransfo3W.Leg(leg1ConnectionType, leg1Ro / zBase, leg1Xo / zBase, leg1FreeFluxes);
        ScTransfo3W.Leg leg2 = new ScTransfo3W.Leg(leg2ConnectionType, leg2Ro / zBase, leg2Xo / zBase, leg2FreeFluxes);
        ScTransfo3W.Leg leg3 = new ScTransfo3W.Leg(leg3ConnectionType, leg3Ro / zBase, leg3Xo / zBase, leg3FreeFluxes);

        ScTransfo3wKt.Leg leg1kT = new ScTransfo3wKt.Leg(kT1R, kT1X, kT1R0, kT1X0);
        ScTransfo3wKt.Leg leg2kT = new ScTransfo3wKt.Leg(kT2R, kT2X, kT2R0, kT2X0);
        ScTransfo3wKt.Leg leg3kT = new ScTransfo3wKt.Leg(kT3R, kT3X, kT3R0, kT3X0);

        lfBranch.setProperty(PROPERTY_SHORT_CIRCUIT, new ScTransfo3W(leg1, leg2, leg3));
        lfBranch.setProperty(PROPERTY_SHORT_CIRCUIT_NORM, new ScTransfo3wKt(leg1kT, leg2kT, leg3kT)); // set in a separate extension because is does not depend only on iidm in input but also on the type of norm
    }

    private static void addLineExtension(Network network, LfBranch lfBranch) {
        String lineId = lfBranch.getOriginalIds().get(0);
        Line line = network.getLine(lineId);

        double vNom2 = line.getTerminal2().getVoltageLevel().getNominalV();
        double zBase = vNom2 * vNom2 / SB;
        double ro = line.getR() / zBase;
        double xo = line.getX() / zBase;
        LineShortCircuit extensions = line.getExtension(LineShortCircuit.class);
        if (extensions != null) {
            ro = extensions.getRo() / zBase;
            xo = extensions.getXo() / zBase;
        }

        lfBranch.setProperty(PROPERTY_SHORT_CIRCUIT, new ScLine(ro, xo));
    }

    private static void addTransfo2Extension(Network network, LfBranch lfBranch, ShortCircuitNormExtensions shortCircuitNormExtensions) {
        String t2wId = lfBranch.getOriginalIds().get(0);
        TwoWindingsTransformer twt = network.getTwoWindingsTransformer(t2wId);

        // per unitizing of grounding Z for LfNetwork
        double u2Nom = twt.getTerminal2().getVoltageLevel().getNominalV();
        double zBase = u2Nom * u2Nom / SB;

        double coeffRo = DEFAULT_COEFF_RO;
        double coeffXo = DEFAULT_COEFF_XO;
        double ro = DEFAULT_COEFF_RO * lfBranch.getPiModel().getR();
        double xo = DEFAULT_COEFF_XO * lfBranch.getPiModel().getX();
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
            ro = extensions.getRo() / zBase;
            xo = extensions.getXo() / zBase;
            freeFluxes = extensions.isFreeFluxes();
            leg1ConnectionType = extensions.getLeg1ConnectionType();
            leg2ConnectionType = extensions.getLeg2ConnectionType();

            r1Ground = extensions.getR1Ground() / zBase;
            x1Ground = extensions.getX1Ground() / zBase;
            r2Ground = extensions.getR2Ground() / zBase;
            x2Ground = extensions.getX2Ground() / zBase;
        }

        TwoWindingsTransformerNorm t2wNormExtension = shortCircuitNormExtensions.getNormExtension(twt);
        if (t2wNormExtension != null) {
            kT = t2wNormExtension.getkNorm();
        }

        lfBranch.setProperty(PROPERTY_SHORT_CIRCUIT, new ScTransfo2W(leg1ConnectionType, leg2ConnectionType, ro, xo, freeFluxes, r1Ground, x1Ground, r2Ground, x2Ground));
        lfBranch.setProperty(PROPERTY_SHORT_CIRCUIT_NORM, kT); // set in a separate extension because is does not depend only on iidm in input but also on the type of norm
    }

    private static void addGeneratorExtension(Network network, LfGenerator lfGenerator, ShortCircuitNormExtensions shortCircuitNormExtensions) {
        Generator generator = network.getGenerator(lfGenerator.getOriginalId());

        double transX = DEFAULT_TRANS_XD;
        double subtransX = DEFAULT_SUB_TRANS_XD;
        double stepUpTfoX = DEFAULT_STEP_UP_XD;

        GeneratorShortCircuit extension = generator.getExtension(GeneratorShortCircuit.class);
        if (extension != null) {
            transX = extension.getDirectTransX();
            subtransX = extension.getDirectSubtransX();
            stepUpTfoX = extension.getStepUpTransformerX();
        }

        double transRd = DEFAULT_TRANS_RD;
        double subTransRd = DEFAULT_SUB_TRANS_RD;
        boolean toGround = DEFAULT_TO_GROUND;
        double ro = subTransRd;
        double xo = subtransX;
        double kG = 1.0;

        GeneratorShortCircuit2 extensions2 = generator.getExtension(GeneratorShortCircuit2.class);
        if (extensions2 != null) {
            transRd = extensions2.getTransRd();
            subTransRd = extensions2.getSubTransRd();
        }

        GeneratorFortescue extensionFortescue = generator.getExtension(GeneratorFortescue.class);
        if (extensionFortescue != null) {
            toGround = extensionFortescue.isToGround();
            ro = extensionFortescue.getRo();
            xo = extensionFortescue.getXo();
        }

        GeneratorNorm extensionGenNorm = shortCircuitNormExtensions.getNormExtension(generator);
        if (extensionGenNorm != null) {
            kG = extensionGenNorm.getkG();
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
                ro,
                xo));
        lfGenerator.setProperty(PROPERTY_SHORT_CIRCUIT_NORM, kG); // set in a separate extension because is does not depend only on iidm in input but also on the type of norm
    }

    private static void addLoadExtension(Network network, LfBus lfBus) {
        double bLoads = 0.;
        double gLoads = 0.;
        for (String loadId : lfBus.getAggregatedLoads().getOriginalIds()) {
            Load load = network.getLoad(loadId);
            double uNom = load.getTerminal().getVoltageLevel().getNominalV();
            LoadShortCircuit extension = load.getExtension(LoadShortCircuit.class);
            double xd = 0.;
            double rd = 0.;
            if (extension != null) {
                Pair<Double, Double> rnxn = extension.getZeqLoad();
                rd = rnxn.getFirst();
                xd = rnxn.getSecond();
            } else {
                // No info available in extension we use the default formula with P and Q
                double p = load.getP0();
                double q = load.getQ0();
                double s2 = p * p + q * q;

                if (s2 > EPSILON) {
                    xd = q / s2 * uNom * uNom;
                    rd = p / s2 * uNom * uNom;
                }
            }
            if (Math.abs(rd) > EPSILON || Math.abs(xd) > EPSILON) {
                double tmpG = (uNom * uNom / SB) * rd / (rd * rd + xd * xd);
                double tmpB = -(uNom * uNom / SB) * xd / (rd * rd + xd * xd);
                gLoads = gLoads + tmpG; // yLoads represents the equivalent admittance of the loads connected at bus at Vnom voltage
                bLoads = bLoads + tmpB;
            }

        }

        lfBus.setProperty(PROPERTY_SHORT_CIRCUIT, new ScLoad(gLoads, bLoads)); // for now load extension is attached to the bus
    }

}
