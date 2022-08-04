/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util;

import com.powsybl.incubator.simulator.util.extensions.ShortCircuitExtensions;
import com.powsybl.incubator.simulator.util.extensions.ShortCircuitGenerator;
import com.powsybl.math.matrix.MatrixFactory;
import com.powsybl.openloadflow.ac.outerloop.AcLoadFlowContext;
import com.powsybl.openloadflow.ac.outerloop.AcLoadFlowParameters;
import com.powsybl.openloadflow.ac.outerloop.AcloadFlowEngine;
import com.powsybl.openloadflow.equations.EquationSystem;
import com.powsybl.openloadflow.equations.VariableSet;
import com.powsybl.openloadflow.network.*;
import net.jafama.FastMath;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public final class AdmittanceEquationSystem {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdmittanceEquationSystem.class);

    private static final double SB = 100.;

    private static final double EPSILON = 0.00000001;

    private AdmittanceEquationSystem() {
    }

    //Equations are created based on the branches connections
    private static void createImpedantBranch(VariableSet<VariableType> variableSet, EquationSystem<VariableType, EquationType> equationSystem,
                                             LfBranch branch, LfBus bus1, LfBus bus2, AdmittanceType admittanceType, MatrixFactory mf) {
        if (bus1 != null && bus2 != null) { //TODO: check case when one bus is OK
            // Equation system Y*V = I (expressed in cartesian coordinates x,y)
            equationSystem.createEquation(bus1.getNum(), EquationType.BUS_YR)
                    .addTerm(new AdmittanceEquationTermX1(branch, bus1, bus2, variableSet, admittanceType, mf));

            equationSystem.createEquation(bus1.getNum(), EquationType.BUS_YI)
                    .addTerm(new AdmittanceEquationTermY1(branch, bus1, bus2, variableSet, admittanceType, mf));

            equationSystem.createEquation(bus2.getNum(), EquationType.BUS_YR)
                    .addTerm(new AdmittanceEquationTermX2(branch, bus1, bus2, variableSet, admittanceType, mf));

            equationSystem.createEquation(bus2.getNum(), EquationType.BUS_YI)
                    .addTerm(new AdmittanceEquationTermY2(branch, bus1, bus2, variableSet, admittanceType, mf));
        }
    }

    public enum AdmittanceVoltageProfileType {
        CALCULATED, // use the computed values at nodes to compute Y elements
        NOMINAL; // use the nominal voltage values at nodes to get Y elements
    }

    public enum AdmittanceType {
        //TODO : adapt for the different kinds of admittance matrices
        ADM_INJ, // all external nodal injections that does not come from branches are considered as current injectors (including shunts elements)
        ADM_SHUNT, // all external  nodal injections that does not come from branches are considered as current injectors (but not shunt elements)
        ADM_ADMIT, // all external  nodal injections are transformed into passive shunt elements included in the Y matrix (then [Ie] should be [0])
        ADM_THEVENIN, // used to compute the Zth Thevenin Equivalent: shunts remain shunts, synchronous machines are transformed into X" equivalent shunts, remaining injections are transformed into passive shunt elements included in the Y matrix
        ADM_THEVENIN_HOMOPOLAR; // used to compute the homopolar admittance matrix for unbalanced short circuits
    }

    public enum AdmittancePeriodType {
        ADM_SUB_TRANSIENT, // all external nodal injections that does not come from branches are considered as current injectors (including shunts elements)
        ADM_TRANSIENT, // all external  nodal injections that does not come from branches are considered as current injectors (but not shunt elements)
        ADM_STEADY_STATE, // all external  nodal injections are transformed into passive shunt elements included in the Y matrix (then [Ie] should be [0])
    }

    private static void createBranches(LfNetwork network, VariableSet<VariableType> variableSet, EquationSystem<VariableType, EquationType> equationSystem, AdmittanceType admittanceType, MatrixFactory mf) {
        for (LfBranch branch : network.getBranches()) {
            LfBus bus1 = branch.getBus1();
            LfBus bus2 = branch.getBus2();
            PiModel piModel = branch.getPiModel();
            if (FastMath.abs(piModel.getX()) < LfBranch.LOW_IMPEDANCE_THRESHOLD) {
                if (bus1 != null && bus2 != null) {
                    LOGGER.warn("Warning: Branch = {} : Non impedant lines not supported in the current version of the reduction method",
                            branch.getId());
                }
            } else {
                //System.out.println("X(" + branch.getId() + ")= " + piModel.getX());
                createImpedantBranch(variableSet, equationSystem, branch, bus1, bus2, admittanceType, mf);
            }
        }
    }

    private static double getBfromShunt(LfBus bus) {
        List<ShortCircuitEquationSystemFeeder> feederList = new ArrayList<>();
        return getBfromShunt(bus, feederList);
    }

    private static double getBfromShunt(LfBus bus, List<ShortCircuitEquationSystemFeeder> feederList) {
        LfShunt shunt = bus.getShunt().orElse(null);
        double tmpB = 0.;
        if (shunt != null) {
            tmpB += shunt.getB();
            ShortCircuitEquationSystemFeeder shuntFeeder = new ShortCircuitEquationSystemFeeder(shunt.getB(), 0., shunt.getId(), ShortCircuitEquationSystemFeeder.FeederType.SHUNT);
            feederList.add(shuntFeeder);
            //check if g will be implemented
        }
        LfShunt controllerShunt = bus.getControllerShunt().orElse(null);
        if (controllerShunt != null) {
            tmpB += controllerShunt.getB();
            ShortCircuitEquationSystemFeeder shuntFeeder = new ShortCircuitEquationSystemFeeder(shunt.getB(), 0., shunt.getId(), ShortCircuitEquationSystemFeeder.FeederType.CONTROLLED_SHUNT);
            feederList.add(shuntFeeder);
            //check if g will be implemented
        }

        return tmpB;
    }

    private static Pair<Double, Double> getYtransfromRdXd(LfBus bus, AdmittancePeriodType admittancePeriodType, List<ShortCircuitEquationSystemFeeder> feederList, AdmittanceType admittanceType) {
        double vnomVl = bus.getNominalV();

        double tmpG = 0.;
        double tmpB = 0.;
        for (LfGenerator lfgen : bus.getGenerators()) { //compute R'd or R"d from generators at bus
            ShortCircuitGenerator scGen = (ShortCircuitGenerator) lfgen.getProperty(ShortCircuitExtensions.PROPERTY_NAME);
            double rd = scGen.getTransRd() + scGen.getStepUpTfoR();
            double xd = scGen.getTransXd() + scGen.getStepUpTfoX();
            if (admittancePeriodType == AdmittancePeriodType.ADM_SUB_TRANSIENT) {
                xd = scGen.getSubTransXd() + scGen.getStepUpTfoX();
                rd = scGen.getSubTransRd() + scGen.getStepUpTfoR();
            }

            double coeffR = 1.0; // coeff used to deduce Ro from Rd. It is equal to 1.0 if we are looking for direct values. If the machine is not grounded, homopolar values are zero, then we set coeffs to 0.
            double coeffX = 1.0;
            if (admittanceType == AdmittanceType.ADM_THEVENIN_HOMOPOLAR) {
                coeffR = 0.;
                coeffX = 0.;
                if (scGen.isGrounded()) {
                    coeffR = scGen.getCoeffRo();
                    coeffX = scGen.getCoeffXo();
                }
            }

            double epsilon = 0.0000001;

            rd = rd * coeffR;
            xd = xd * coeffX;

            if (Math.abs(rd) > epsilon || Math.abs(xd) > epsilon) {
                double gGen = (vnomVl * vnomVl / SB) * rd / (rd * rd + xd * xd);
                double bGen = -(vnomVl * vnomVl / SB) * xd / (rd * rd + xd * xd);
                tmpG = tmpG + gGen;
                tmpB = tmpB + bGen; // TODO: check: for now X'd = 0 not allowed
                ShortCircuitEquationSystemFeeder shuntFeeder = new ShortCircuitEquationSystemFeeder(bGen, gGen, lfgen.getId(), ShortCircuitEquationSystemFeeder.FeederType.GENERATOR);
                feederList.add(shuntFeeder);
            }
        }

        Pair<Double, Double> result = new Pair<>(tmpG, tmpB);
        return result;
    }

    private static void createShunts(LfNetwork network, VariableSet<VariableType> variableSet, EquationSystem<VariableType, EquationType> equationSystem, AdmittanceType admittanceType,
                                     AdmittanceVoltageProfileType admittanceVoltageProfileType, AdmittancePeriodType admittancePeriodType,
                                     boolean isShuntsIgnore, ShortCircuitEquationSystemFeeders feeders) {
        for (LfBus bus : network.getBuses()) {

            //total shunt at bus to be integrated in the admittance matrix
            double g = 0.;
            double b = 0.;

            //shunts created to represent the equivalence of loads and to be integrated in the total admittance matrix shunt at bus
            double gLoadEq = 0.;
            double bLoadEq = 0.;

            //shunts created to represent the equivalence of generating units sand to be integrated in the total admittance matrix shunt at bus
            double gGenEq = 0.;
            double bGenEq = 0.;

            //choice of vbase to be used to transform power injections into equivalent shunts
            double vr = bus.getV() * Math.cos(bus.getAngle());
            double vi = bus.getV() * Math.sin(bus.getAngle());
            if (admittanceVoltageProfileType == AdmittanceVoltageProfileType.NOMINAL) {
                vr = 1.0; //TODO: check if Vnom or Vnom/Vbase
                vi = 0.;
            }
            boolean isBusPv = bus.isVoltageControlled();

            if (admittanceType == AdmittanceType.ADM_SHUNT) {
                if (!isShuntsIgnore) {
                    b = getBfromShunt(bus); // Handling shunts that physically exist
                }
            } else if (admittanceType == AdmittanceType.ADM_ADMIT) {
                if (!isShuntsIgnore) {
                    b = getBfromShunt(bus); // Handling shunts that physically exist
                }
                gLoadEq = bus.getLoadTargetP() / (vr * vr + vi * vi); // Handling transformation of bus loads into equivalent shunts
                bLoadEq = -bus.getLoadTargetQ() / (vr * vr + vi * vi);
                // Handling transformation of generators into equivalent shunts
                // Warning !!! : evaluation of power injections mandatory
                gGenEq = -(bus.getP().eval() + bus.getLoadTargetP()) / (vr * vr + vi * vi); // full nodal P injection without the load

                if (isBusPv) {
                    bGenEq = (bus.getQ().eval() + bus.getLoadTargetQ()) / (vr * vr + vi * vi); // full nodal Q injection without the load
                } else {
                    bGenEq = bus.getGenerationTargetQ() / (vr * vr + vi * vi);
                }
            } else if (admittanceType == AdmittanceType.ADM_THEVENIN) {

                List<ShortCircuitEquationSystemFeeder> feederList = new ArrayList<>();

                if (!isShuntsIgnore) {
                    // Handling shunts that physically exist
                    b = getBfromShunt(bus, feederList); // ! updates feederList
                }
                gLoadEq = bus.getLoadTargetP() / (vr * vr + vi * vi); // Handling transformation of bus loads into equivalent shunts
                bLoadEq = -bus.getLoadTargetQ() / (vr * vr + vi * vi);

                ShortCircuitEquationSystemFeeder shuntFeeder = new ShortCircuitEquationSystemFeeder(bLoadEq, gLoadEq, bus.getId(), ShortCircuitEquationSystemFeeder.FeederType.LOAD);
                feederList.add(shuntFeeder);

                Pair<Double, Double> bAndG = getYtransfromRdXd(bus, admittancePeriodType, feederList, admittanceType); // ! updates feederList
                bGenEq = bAndG.getValue(); //TODO : check how the verify that the generators are operating
                gGenEq = bAndG.getKey();

                //shortCircuitNetwork.busToFeeder.put(bus, feederList);
                ShortCircuitEquationSystemBusFeeders shortCircuitEquationSystemBusFeeders = new ShortCircuitEquationSystemBusFeeders(feederList, bus);
                feeders.busToFeeders.put(bus, shortCircuitEquationSystemBusFeeders);

            } else if (admittanceType == AdmittanceType.ADM_THEVENIN_HOMOPOLAR) {

                List<ShortCircuitEquationSystemFeeder> feederList = new ArrayList<>(); // not used yet in homopolar

                Pair<Double, Double> bAndG = getYtransfromRdXd(bus, admittancePeriodType, feederList, admittanceType); // ! updates feederList
                bGenEq = bAndG.getValue(); //TODO : check how the verify that the generators are operating
                gGenEq = bAndG.getKey();

            }

            g = g + gLoadEq + gGenEq;
            b = b + bLoadEq + bGenEq;

            //System.out.println("result = g(" + bus.getId() + ")= " + g);
            //System.out.println("result = b(" + bus.getId() + ")= " + b);

            if (Math.abs(g) > EPSILON || Math.abs(b) > EPSILON) {
                equationSystem.createEquation(bus.getNum(), EquationType.BUS_YR)
                        .addTerm(new AdmittanceShuntEquationTerm(g, b, bus, variableSet, true));
                equationSystem.createEquation(bus.getNum(), EquationType.BUS_YI)
                        .addTerm(new AdmittanceShuntEquationTerm(g, b, bus, variableSet, false));
            }
        }
    }

    public static EquationSystem<VariableType, EquationType> create(LfNetwork network, MatrixFactory mf, VariableSet<VariableType> variableSet,
                                                                    AdmittanceType admittanceType, AdmittanceVoltageProfileType admittanceVoltageProfileType,
                                                                    AcLoadFlowParameters acLoadFlowParameters) {

        // Following data Not needed for reduction methods
        AdmittanceEquationSystem.AdmittancePeriodType admittancePeriodType = AdmittanceEquationSystem.AdmittancePeriodType.ADM_TRANSIENT; //TODO: not relevant for reduction: see how to improve that
        ShortCircuitEquationSystemFeeders equationsSystemFeeders = new ShortCircuitEquationSystemFeeders();
        boolean isShuntsIgnore = false;

        return create(network, mf, variableSet,
                admittanceType, admittanceVoltageProfileType, admittancePeriodType, isShuntsIgnore,
                equationsSystemFeeders, acLoadFlowParameters);
    }

    public static EquationSystem<VariableType, EquationType> create(LfNetwork network, MatrixFactory mf, VariableSet<VariableType> variableSet,
                                                                    AdmittanceType admittanceType, AdmittanceVoltageProfileType admittanceVoltageProfileType,
                                                                    AdmittancePeriodType admittancePeriodType, boolean isShuntsIgnore, ShortCircuitEquationSystemFeeders feeders,
                                                                    AcLoadFlowParameters acLoadFlowParameters) {

        EquationSystem<VariableType, EquationType> equationSystem = new EquationSystem<>();

        if (admittanceType == AdmittanceType.ADM_ADMIT) {
            try (AcLoadFlowContext context = new AcLoadFlowContext(network, acLoadFlowParameters)) {
                new AcloadFlowEngine(context)
                        .run();
            }
        }

        createBranches(network, variableSet, equationSystem, admittanceType, mf);
        if (admittanceType != AdmittanceType.ADM_INJ) { //shunts created in the admittance matrix are only those that really exist in the network
            createShunts(network, variableSet, equationSystem, admittanceType, admittanceVoltageProfileType, admittancePeriodType, isShuntsIgnore, feeders); // TODO : shuntIgnore was set at false
        }

        return equationSystem;
    }

}
