/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.interpretation;

import java.util.Objects;

import org.apache.commons.math3.complex.Complex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.interpretation.model.cgmes.CgmesLine;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesModelForInterpretation;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesNode;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesTransformer;
import com.powsybl.cgmes.interpretation.model.interpreted.BranchAdmittanceMatrix;
import com.powsybl.cgmes.interpretation.model.interpreted.DetectedEquipmentModel;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretedLine;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretedTransformer2;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretedTransformer3;
import com.powsybl.commons.PowsyblException;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public final class InterpretedComputedFlow {

    private InterpretedComputedFlow() {
        this.p = 0.0;
        this.q = 0.0;
        this.calculated = false;
        this.badVoltage = false;
        this.isLine = false;
        this.isTransformer2 = false;
        this.isTransformer3 = false;
    }

    public double p() {
        return p;
    }

    public double q() {
        return q;
    }

    public boolean isCalculated() {
        return calculated;
    }

    public boolean isBadVoltage() {
        return badVoltage;
    }

    public DetectedEquipmentModel detectedEquipmentModel() {
        return equipmentModel;
    }

    public boolean isLine() {
        return isLine;
    }

    public boolean isTransformer2() {
        return isTransformer2;
    }

    public boolean isTransformer3() {
        return isTransformer3;
    }

    public static InterpretedComputedFlow forEquipment(String equipmentId, String atNode,
            InterpretationAlternative alternative, CgmesModelForInterpretation cgmes) {
        InterpretedComputedFlow flow;
        CgmesLine line = cgmes.getLine(equipmentId);
        if (line != null) {
            CgmesNode node1 = cgmes.getNode(line.nodeId1());
            CgmesNode node2 = cgmes.getNode(line.nodeId2());
            flow = InterpretedComputedFlow.forLine(line, atNode, node1, node2, alternative);
        } else {
            CgmesTransformer transformer = cgmes.getTransformer(equipmentId);
            if (transformer != null) {
                CgmesNode node1 = cgmes.getNode(transformer.end1().nodeId());
                CgmesNode node2 = cgmes.getNode(transformer.end2().nodeId());
                if (transformer.numEnds() == 2) {
                    flow = InterpretedComputedFlow.forTransformer2(transformer, atNode, node1, node2, alternative);
                } else {
                    CgmesNode node3 = cgmes.getNode(transformer.end3().nodeId());
                    flow = InterpretedComputedFlow.forTransformer3(transformer, atNode, node1, node2, node3,
                            alternative);
                }
            } else {
                throw new PowsyblException("Equipment not found as Line or Transformer " + equipmentId);
            }
        }
        return flow;
    }

    public static InterpretedComputedFlow forLine(CgmesLine line, String atNode, CgmesNode node1, CgmesNode node2,
            InterpretationAlternative config) {
        // A node may be null if line is not connected at that end

        InterpretedComputedFlow f = new InterpretedComputedFlow();
        f.isLine = true;

        InterpretedLine b = new InterpretedLine(line, node1, node2, config);
        f.equipmentModel = new DetectedEquipmentModel(b.getBranchModel());

        if (line.connected1() && line.connected2()) {
            if (node1 != null && node2 != null) {
                f.calculateBothEndsFlow(
                        atNode,
                        line.nodeId1(), line.nodeId2(),
                        node1.v(), node1.angle(),
                        node2.v(), node2.angle(),
                        b.getAdmittanceMatrix());
            }
        } else if (line.connected1()) {
            if (node1 != null) {
                f.calculateEndFromFlow(
                        atNode,
                        line.nodeId1(),
                        node1.v(), node1.angle(),
                        b.getAdmittanceMatrix());
            }
        } else if (line.connected2()) {
            if (node2 != null) {
                f.calculateEndToFlow(
                        atNode,
                        line.nodeId2(),
                        node2.v(), node2.angle(),
                        b.getAdmittanceMatrix());
            }
        } else {
            throw new PowsyblException("Line disconnected at both ends " + line.id());
        }
        return f;
    }

    public static InterpretedComputedFlow forTransformer2(CgmesTransformer transformer, String atNode,
            CgmesNode node1, CgmesNode node2, InterpretationAlternative config) {

        Objects.requireNonNull(node1);
        Objects.requireNonNull(node2);
        InterpretedComputedFlow f = new InterpretedComputedFlow();
        f.isTransformer2 = true;

        InterpretedTransformer2 b = new InterpretedTransformer2(transformer, config);
        f.equipmentModel = new DetectedEquipmentModel(b.getBranchModel());

        String nodeId1 = transformer.end1().nodeId();
        String nodeId2 = transformer.end2().nodeId();
        boolean connected1 = transformer.end1().connected();
        boolean connected2 = transformer.end2().connected();
        if (connected1 && connected2) {
            f.calculateBothEndsFlow(
                    atNode,
                    nodeId1, nodeId2,
                    node1.v(), node1.angle(),
                    node2.v(), node2.angle(),
                    b.getAdmittanceMatrix());
        } else if (connected1) {
            f.calculateEndFromFlow(
                    atNode,
                    nodeId1,
                    node1.v(), node1.angle(),
                    b.getAdmittanceMatrix());
        } else if (connected2) {
            f.calculateEndToFlow(
                    atNode,
                    nodeId2,
                    node2.v(), node2.angle(),
                    b.getAdmittanceMatrix());
        }
        return f;
    }

    public static InterpretedComputedFlow forTransformer3(CgmesTransformer transformer, String n,
            CgmesNode node1, CgmesNode node2, CgmesNode node3,
            InterpretationAlternative config) {
        Objects.requireNonNull(node1);
        Objects.requireNonNull(node2);
        Objects.requireNonNull(node3);

        InterpretedComputedFlow f = new InterpretedComputedFlow();
        f.isTransformer3 = true;

        Boolean connected1 = transformer.end1().connected();
        Boolean connected2 = transformer.end2().connected();
        Boolean connected3 = transformer.end3().connected();

        InterpretedTransformer3 t3 = new InterpretedTransformer3(transformer, config);
        f.equipmentModel = new DetectedEquipmentModel(t3.getBranchModelEnd1(), t3.getBranchModelEnd2(),
                t3.getBranchModelEnd3());

        if (f.isValidTransformer3(transformer)) {
            String nodeId1 = transformer.end1().nodeId();
            String nodeId2 = transformer.end2().nodeId();
            String nodeId3 = transformer.end3().nodeId();
            if (connected1 && connected2 && connected3) {
                f.calculateThreeConnectedEndsFlow(
                        n,
                        nodeId1, nodeId2, nodeId3,
                        node1.v(), node1.angle(),
                        node2.v(), node2.angle(),
                        node3.v(), node3.angle(),
                        t3.getAdmittanceMatrixEnd1(),
                        t3.getAdmittanceMatrixEnd2(),
                        t3.getAdmittanceMatrixEnd3());
            } else if (connected1 && connected2) {
                BranchAdmittanceMatrix admittanceMatrixEnd1 = t3.getAdmittanceMatrixEnd1();
                BranchAdmittanceMatrix admittanceMatrixEnd2 = t3.getAdmittanceMatrixEnd2();
                BranchAdmittanceMatrix admittanceMatrixOpenEnd = t3.getAdmittanceMatrixEnd3();
                f.calculateTwoConnectedEndsFlow(
                        n,
                        nodeId1, nodeId2,
                        node1.v(), node1.angle(),
                        node2.v(), node2.angle(),
                        admittanceMatrixEnd1,
                        admittanceMatrixEnd2,
                        admittanceMatrixOpenEnd);
            } else if (connected1 && connected3) {
                BranchAdmittanceMatrix admittanceMatrixEnd1 = t3.getAdmittanceMatrixEnd1();
                BranchAdmittanceMatrix admittanceMatrixEnd3 = t3.getAdmittanceMatrixEnd3();
                BranchAdmittanceMatrix admittanceMatrixOpenEnd = t3.getAdmittanceMatrixEnd2();
                f.calculateTwoConnectedEndsFlow(
                        n,
                        nodeId1, nodeId3,
                        node1.v(), node1.angle(),
                        node3.v(), node3.angle(),
                        admittanceMatrixEnd1,
                        admittanceMatrixEnd3,
                        admittanceMatrixOpenEnd);
            } else if (connected2 && connected3) {
                BranchAdmittanceMatrix admittanceMatrixEnd2 = t3.getAdmittanceMatrixEnd2();
                BranchAdmittanceMatrix admittanceMatrixEnd3 = t3.getAdmittanceMatrixEnd3();
                BranchAdmittanceMatrix admittanceMatrixOpenEnd = t3.getAdmittanceMatrixEnd1();
                f.calculateTwoConnectedEndsFlow(
                        n,
                        nodeId2, nodeId3,
                        node2.v(), node2.angle(),
                        node3.v(), node3.angle(),
                        admittanceMatrixEnd2,
                        admittanceMatrixEnd3,
                        admittanceMatrixOpenEnd);
            } else if (connected1) {
                BranchAdmittanceMatrix admittanceMatrixEnd1 = t3.getAdmittanceMatrixEnd1();
                BranchAdmittanceMatrix admittanceMatrixFirstOpenEnd = t3.getAdmittanceMatrixEnd2();
                BranchAdmittanceMatrix admittanceMatrixSecondOpenEnd = t3.getAdmittanceMatrixEnd3();
                f.calculateOneConnectedEndFlow(
                        n,
                        nodeId1,
                        node1.v(), node1.angle(),
                        admittanceMatrixEnd1,
                        admittanceMatrixFirstOpenEnd,
                        admittanceMatrixSecondOpenEnd);
            } else if (connected2) {
                BranchAdmittanceMatrix admittanceMatrixEnd2 = t3.getAdmittanceMatrixEnd2();
                BranchAdmittanceMatrix admittanceMatrixFirstOpenEnd = t3.getAdmittanceMatrixEnd1();
                BranchAdmittanceMatrix admittanceMatrixSecondOpenEnd = t3.getAdmittanceMatrixEnd3();
                f.calculateOneConnectedEndFlow(
                        n,
                        nodeId2,
                        node2.v(), node2.angle(),
                        admittanceMatrixEnd2,
                        admittanceMatrixFirstOpenEnd,
                        admittanceMatrixSecondOpenEnd);
            } else if (connected3) {
                BranchAdmittanceMatrix admittanceMatrixEnd3 = t3.getAdmittanceMatrixEnd3();
                BranchAdmittanceMatrix admittanceMatrixFirstOpenEnd = t3.getAdmittanceMatrixEnd1();
                BranchAdmittanceMatrix admittanceMatrixSecondOpenEnd = t3.getAdmittanceMatrixEnd2();
                f.calculateOneConnectedEndFlow(
                        n,
                        nodeId3,
                        node3.v(), node3.angle(),
                        admittanceMatrixEnd3,
                        admittanceMatrixFirstOpenEnd,
                        admittanceMatrixSecondOpenEnd);
            }
        }
        return f;
    }

    // Line and Xfmr2 flow calculations
    private void calculateEndFromFlow(String n, String nEnd1, double v1, double angleDegrees1,
            BranchAdmittanceMatrix admittanceMatrix) {
        calculateEndFlow(n, nEnd1, v1, angleDegrees1, admittanceMatrix, false);
    }

    private void calculateEndToFlow(String n, String nEnd2, double v2, double angleDegrees2,
            BranchAdmittanceMatrix admittanceMatrix) {
        calculateEndFlow(n, nEnd2, v2, angleDegrees2, admittanceMatrix, true);
    }

    private void calculateEndFlow(String n, String nEnd, double v, double angleDegrees,
            BranchAdmittanceMatrix admittanceMatrix, boolean isOpenFrom) {
        if (v == 0.0) {
            return;
        }
        double angle = Math.toRadians(angleDegrees);
        Complex a = new Complex(v * Math.cos(angle), v * Math.sin(angle));

        if (nEnd.equals(n)) {
            Complex ysh = kronAntenna(admittanceMatrix.y11(), admittanceMatrix.y12(), admittanceMatrix.y21(),
                    admittanceMatrix.y22(), isOpenFrom);
            p = ysh.getReal() * a.abs() * a.abs();
            q = -ysh.getImaginary() * a.abs() * a.abs();
        } else {
            LOG.warn("calculateEndToFlow. Unexpected node");
        }
        calculated = true;
        badVoltage = !anglesAreOk(angleDegrees);
    }

    private void calculateBothEndsFlow(String n, String nEnd1, String nEnd2, double v1, double angleDegrees1, double v2,
            double angleDegrees2, BranchAdmittanceMatrix admittanceMatrix) {
        if (v1 == 0.0 || v2 == 0.0) {
            return;
        }
        double angle1 = Math.toRadians(angleDegrees1);
        double angle2 = Math.toRadians(angleDegrees2);
        Complex vf = new Complex(v1 * Math.cos(angle1), v1 * Math.sin(angle1));
        Complex vt = new Complex(v2 * Math.cos(angle2), v2 * Math.sin(angle2));

        flowBothEnds(admittanceMatrix.y11(), admittanceMatrix.y12(),
                admittanceMatrix.y21(), admittanceMatrix.y22(), vf, vt);

        if (nEnd1.equals(n)) {
            p = sft.getReal();
            q = sft.getImaginary();
        } else if (nEnd2.equals(n)) {
            p = stf.getReal();
            q = stf.getImaginary();
        } else {
            LOG.warn("calculateBothEndsFlow. Unexpected node");
        }
        calculated = true;
        badVoltage = !anglesAreOk(angleDegrees1, angleDegrees2);
    }

    // Xfmr3 flow calculations
    private void calculateOneConnectedEndFlow(String n, String nEnd, double v, double angleDegrees,
            BranchAdmittanceMatrix admittanceMatrixEnd, BranchAdmittanceMatrix admittanceMatrixOpenEnd1,
            BranchAdmittanceMatrix admittanceMatrixOpenEnd2) {
        if (v == 0.0) {
            return;
        }
        double angle = Math.toRadians(angleDegrees);
        Complex vf = new Complex(v * Math.cos(angle), v * Math.sin(angle));

        Complex ysh = calculateEndShunt(
                // TODO We should pass directly the admittance matrixes instead of each element
                admittanceMatrixEnd.y11(), admittanceMatrixEnd.y12(), admittanceMatrixEnd.y21(), admittanceMatrixEnd.y22(),
                admittanceMatrixOpenEnd1.y11(), admittanceMatrixOpenEnd1.y12(), admittanceMatrixOpenEnd1.y21(),
                admittanceMatrixOpenEnd1.y22(),
                admittanceMatrixOpenEnd2.y11(), admittanceMatrixOpenEnd2.y12(), admittanceMatrixOpenEnd2.y21(),
                admittanceMatrixOpenEnd2.y22());

        if (nEnd.equals(n)) {
            p = ysh.getReal() * vf.abs() * vf.abs();
            q = ysh.getImaginary() * vf.abs() * vf.abs();
        } else {
            LOG.warn("calculateEnd1Flow. Unexpected node");
        }
        calculated = true;
        badVoltage = !anglesAreOk(angleDegrees);
    }

    private void calculateTwoConnectedEndsFlow(String n, String nEnd1, String nEnd2, double v1, double angleDegrees1,
            double v2,
            double angleDegrees2, BranchAdmittanceMatrix admittanceMatrixEnd1,
            BranchAdmittanceMatrix admittanceMatrixEnd2, BranchAdmittanceMatrix admittanceMatrixOpenEnd) {
        if (v1 == 0.0 || v2 == 0.0) {
            return;
        }
        double angle1 = Math.toRadians(angleDegrees1);
        double angle2 = Math.toRadians(angleDegrees2);
        Complex vf1 = new Complex(v1 * Math.cos(angle1), v1 * Math.sin(angle1));
        Complex vf2 = new Complex(v2 * Math.cos(angle2), v2 * Math.sin(angle2));

        BranchAdmittanceMatrix admittance = calculateTwoConnectedEndsAdmittance(
                admittanceMatrixEnd1.y11(), admittanceMatrixEnd1.y12(), admittanceMatrixEnd1.y21(),
                admittanceMatrixEnd1.y22(),
                admittanceMatrixEnd2.y11(), admittanceMatrixEnd2.y12(), admittanceMatrixEnd2.y21(),
                admittanceMatrixEnd2.y22(),
                admittanceMatrixOpenEnd.y11(), admittanceMatrixOpenEnd.y12(), admittanceMatrixOpenEnd.y21(),
                admittanceMatrixOpenEnd.y22());

        flowBothEnds(admittance.y11(), admittance.y12(), admittance.y21(), admittance.y22(), vf1, vf2);

        if (nEnd1.equals(n)) {
            p = sft.getReal();
            q = sft.getImaginary();
        } else if (nEnd2.equals(n)) {
            p = stf.getReal();
            q = stf.getImaginary();
        } else {
            LOG.warn("calculateEnd1End2Flow. Unexpected node");
        }
        calculated = true;
        badVoltage = !anglesAreOk(angleDegrees1, angleDegrees2);
    }

    private void calculateThreeConnectedEndsFlow(String n, String nEnd1, String nEnd2, String nEnd3, double v1,
            double angleDegrees1,
            double v2, double angleDegrees2, double v3, double angleDegrees3,
            BranchAdmittanceMatrix admittanceMatrixEnd1, BranchAdmittanceMatrix admittanceMatrixEnd2,
            BranchAdmittanceMatrix admittanceMatrixEnd3) {
        if (v1 == 0.0 || v2 == 0.0 || v3 == 0.0) {
            return;
        }

        double angle1 = Math.toRadians(angleDegrees1);
        double angle2 = Math.toRadians(angleDegrees2);
        double angle3 = Math.toRadians(angleDegrees3);
        Complex vf1 = new Complex(v1 * Math.cos(angle1), v1 * Math.sin(angle1));
        Complex vf2 = new Complex(v2 * Math.cos(angle2), v2 * Math.sin(angle2));
        Complex vf3 = new Complex(v3 * Math.cos(angle3), v3 * Math.sin(angle3));

        Complex v0 = admittanceMatrixEnd1.y21().multiply(vf1).add(admittanceMatrixEnd2.y21().multiply(vf2))
                .add(admittanceMatrixEnd3.y21().multiply(vf3)).negate()
                .divide(admittanceMatrixEnd1.y22().add(admittanceMatrixEnd2.y22()).add(admittanceMatrixEnd3.y22()));

        if (nEnd1.equals(n)) {
            flowBothEnds(admittanceMatrixEnd1.y11(), admittanceMatrixEnd1.y12(), admittanceMatrixEnd1.y21(),
                    admittanceMatrixEnd1.y22(), vf1, v0);
            p = sft.getReal();
            q = sft.getImaginary();
        } else if (nEnd2.equals(n)) {
            flowBothEnds(admittanceMatrixEnd2.y11(), admittanceMatrixEnd2.y12(), admittanceMatrixEnd2.y21(),
                    admittanceMatrixEnd2.y22(), vf2, v0);
            p = sft.getReal();
            q = sft.getImaginary();
        } else if (nEnd3.equals(n)) {
            flowBothEnds(admittanceMatrixEnd3.y11(), admittanceMatrixEnd3.y12(), admittanceMatrixEnd3.y21(),
                    admittanceMatrixEnd3.y22(), vf3, v0);
            p = sft.getReal();
            q = sft.getImaginary();
        } else {
            LOG.warn("calculate3EndsFlow. Unexpected node");
        }
        calculated = true;
        badVoltage = !anglesAreOk(angleDegrees1, angleDegrees2, angleDegrees3);
    }

    private Complex calculateEndShunt(Complex y11, Complex y12, Complex y21, Complex y22, Complex yFirstOpen11,
            Complex yFirstOpen12, Complex yFirstOpen21, Complex yFirstOpen22, Complex ySecondOpen11,
            Complex ySecondOpen12, Complex ySecondOpen21, Complex ySecondOpen22) {
        Complex ysh1 = kronAntenna(yFirstOpen11, yFirstOpen12, yFirstOpen21, yFirstOpen22, true);
        Complex ysh2 = kronAntenna(ySecondOpen11, ySecondOpen12, ySecondOpen21, ySecondOpen22, true);
        y22.add(ysh1).add(ysh2);

        return kronAntenna(y11, y12, y21, y22, false);
    }

    private BranchAdmittanceMatrix calculateTwoConnectedEndsAdmittance(Complex yFirstConnected11,
            Complex yFirstConnected12,
            Complex yFirstConnected21, Complex yFirstConnected22, Complex ySecondConnected11,
            Complex ySecondConnected12,
            Complex ySecondConnected21, Complex ySecondConnected22, Complex yOpen11, Complex yOpen12, Complex yOpen21,
            Complex yOpen22) {
        Complex ysh = kronAntenna(yOpen11, yOpen12, yOpen21, yOpen22, true);
        ySecondConnected22.add(ysh);

        return kronChain(yFirstConnected11, yFirstConnected12, yFirstConnected21, yFirstConnected22, ySecondConnected11,
                ySecondConnected12, ySecondConnected21, ySecondConnected22);
    }

    private Complex kronAntenna(Complex y11, Complex y12, Complex y21, Complex y22, boolean isOpenFrom) {
        Complex ysh;
        if (isOpenFrom) {
            ysh = y22.subtract(y21.multiply(y12).divide(y11));
        } else {
            ysh = y11.subtract(y12.multiply(y21).divide(y22));
        }
        return ysh;
    }

    private BranchAdmittanceMatrix kronChain(Complex yFirstConnected11, Complex yFirstConnected12,
            Complex yFirstConnected21, Complex yFirstConnected22, Complex ySecondConnected11,
            Complex ySecondConnected12, Complex ySecondConnected21, Complex ySecondConnected22) {

        Complex y11 = yFirstConnected11.subtract(yFirstConnected21.multiply(yFirstConnected12)
                .divide(yFirstConnected22.add(ySecondConnected22)));
        Complex y12 = ySecondConnected21.multiply(yFirstConnected12)
                .divide(yFirstConnected22.add(ySecondConnected22)).negate();
        Complex y21 = yFirstConnected21.multiply(ySecondConnected12)
                .divide(yFirstConnected22.add(ySecondConnected22)).negate();
        Complex y22 = ySecondConnected11.subtract(
                ySecondConnected21.multiply(ySecondConnected12).divide(yFirstConnected22.add(ySecondConnected22)));

        return new BranchAdmittanceMatrix(y11, y12, y21, y22);
    }

    private void flowBothEnds(Complex y11, Complex y12, Complex y21, Complex y22, Complex v1, Complex v2) {
        Complex ift = y12.multiply(v2).add(y11.multiply(v1));
        sft = ift.conjugate().multiply(v1);

        Complex itf = y21.multiply(v1).add(y22.multiply(v2));
        stf = itf.conjugate().multiply(v2);
    }

    private boolean isValidTransformer3(CgmesTransformer t3) {
        double r1 = t3.end1().r();
        double x1 = t3.end1().x();
        double r2 = t3.end2().r();
        double x2 = t3.end2().x();
        double r3 = t3.end3().r();
        double x3 = t3.end3().x();
        return !(r1 == 0.0 && x1 == 0.0 || r2 == 0.0 && x2 == 0.0 || r3 == 0.0 && x3 == 0.0);
    }

    private boolean anglesAreOk(double... angleDegrees) {
        for (double angleDegree : angleDegrees) {
            if (angleDegree == 0.0) {
                return false;
            }
        }
        return true;
    }

    private boolean isLine;
    private boolean isTransformer2;
    private boolean isTransformer3;

    private double p;
    private double q;

    private boolean calculated;
    private boolean badVoltage;

    private DetectedEquipmentModel equipmentModel;

    private Complex sft;
    private Complex stf;

    private static final Logger LOG = LoggerFactory.getLogger(InterpretedComputedFlow.class);
}
