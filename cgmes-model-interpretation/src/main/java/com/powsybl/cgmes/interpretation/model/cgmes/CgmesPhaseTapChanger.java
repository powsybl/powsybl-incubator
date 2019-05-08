/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.interpretation.model.cgmes;

import org.apache.commons.math3.complex.Complex;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.CgmesNames1;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class CgmesPhaseTapChanger extends CgmesTapChanger {
    public static final CgmesPhaseTapChanger EMPTY = new CgmesPhaseTapChanger();

    public enum Type {
        TABULAR, SYMMETRICAL, ASYMMETRICAL
    }

    final Type type;
    final double xStepMin;
    final double xStepMax;
    final double stepVoltageIncrement;
    final double stepPhaseShiftIncrement;
    final double windingConnectionAngle;

    final boolean asymmetricalDifferentRatios;

    private CgmesPhaseTapChanger() {
        stepVoltageIncrement = 0;
        stepPhaseShiftIncrement = 0;
        windingConnectionAngle = 0;
        type = Type.SYMMETRICAL;
        xStepMin = 0;
        xStepMax = 0;
        asymmetricalDifferentRatios = false;
    }

    CgmesPhaseTapChanger(PropertyBag ptcp, CgmesModel cgmes) {
        super(ptcp, "PhaseTapChangerTable", cgmes);
        this.stepVoltageIncrement = ptcp.asDouble("voltageStepIncrement", 0);
        this.stepPhaseShiftIncrement = ptcp.asDouble("stepPhaseShiftIncrement", 0);
        this.windingConnectionAngle = ptcp.asDouble("windingConnectionAngle", 0);
        this.type = detectType(ptcp.getLocal("phaseTapChangerType").toLowerCase(), tableId, table);
        if (ptcp.containsKey(CgmesNames1.PHASE_TAP_CHANGER_X_STEP_MIN)
            && ptcp.containsKey(CgmesNames1.PHASE_TAP_CHANGER_X_STEP_MAX)) {
            double xStepMinp = ptcp.asDouble(CgmesNames1.PHASE_TAP_CHANGER_X_STEP_MIN);
            double xStepMaxp = ptcp.asDouble(CgmesNames1.PHASE_TAP_CHANGER_X_STEP_MAX);
            if (isXStepRangeValid(xStepMinp, xStepMaxp)) {
                this.xStepMin = xStepMinp;
                this.xStepMax = xStepMaxp;
            } else {
                this.xStepMin = 0;
                this.xStepMax = 0;
            }
        } else if (ptcp.containsKey("xMin") && ptcp.containsKey("xMax")) {
            double xStepMinp = ptcp.asDouble("xMin");
            double xStepMaxp = ptcp.asDouble("xMax");
            if (isXStepRangeValid(xStepMinp, xStepMaxp)) {
                this.xStepMin = xStepMinp;
                this.xStepMax = xStepMaxp;
            } else {
                this.xStepMin = 0;
                this.xStepMax = 0;
            }
        } else {
            this.xStepMin = 0;
            this.xStepMax = 0;
        }
        this.asymmetricalDifferentRatios = type == Type.ASYMMETRICAL
            && stepVoltageIncrement != 0.0 && lowStep != highStep;
    }

    public CgmesTapChangerStatus status() {
        CgmesTapChangerStatus status;
        if (type == Type.TABULAR) {
            status = statusFromTable();
        } else {
            status = new CgmesTapChangerStatus();
            Complex a = nonTabularRatio(step);
            status.ratio = a.abs();
            status.angle = Math.toDegrees(a.getArgument());
        }
        return status;
    }

    public double overrideX(double x, double angle) {
        double x1 = x;
        if (xStepMax > 0) {
            double alphaMax = alphaMax();
            if (type == Type.ASYMMETRICAL) {
                x1 = asymmetricalX(angle, alphaMax);
            } else if (type == Type.SYMMETRICAL) {
                x1 = symmetricalX(angle, alphaMax);
            } else {
                // Nothing to do for tabular
            }
        }
        return x1;
    }

    public boolean hasDifferentRatiosAngles() {
        if (stepVoltageIncrement != 0 && lowStep != highStep) {
            return true;
        }
        if (stepPhaseShiftIncrement != 0 && lowStep != highStep) {
            return true;
        }
        return tabularDifferentRatios || asymmetricalDifferentRatios || tabularDifferentAngles;
    }

    private boolean isXStepRangeValid(double xmin, double xmax) {
        return xmin >= 0
            && xmax > 0
            && xmin < xmax;
    }

    private Type detectType(String stype, String tableId, PropertyBags table) {
        if (stype != null) {
            if (stype.endsWith("tabular") && tableId != null && table != null) {
                return Type.TABULAR;
            } else if (stype.endsWith("asymmetrical")) {
                return Type.ASYMMETRICAL;
            } else if (stype.endsWith("symmetrical")) {
                return Type.SYMMETRICAL;
            }
        }
        return Type.SYMMETRICAL;
    }

    private double alphaMax() {
        double amax = 0.0;
        for (int step = lowStep; step <= highStep; step++) {
            double a = nonTabularRatio(step).getArgument();
            if (a > amax) {
                amax = a;
            }
        }
        return Math.toDegrees(amax);
    }

    private Complex nonTabularRatio(double step) {
        if (type == Type.ASYMMETRICAL) {
            return asymmetricalRatio(step);
        } else if (type == Type.SYMMETRICAL) {
            return symmetricalRatio(step);
        } else {
            return symmetricalRatio(step);
        }
    }

    private Complex symmetricalRatio(double step) {
        double a;
        if (stepPhaseShiftIncrement != 0.0) {
            a = Math.toRadians((step - neutralStep) * stepPhaseShiftIncrement);
        } else {
            double dy = (step - neutralStep) * (stepVoltageIncrement / 100.0);
            a = 2 * Math.asin(dy / 2);
        }
        return new Complex(1.0, a);
    }

    private Complex asymmetricalRatio(double step) {
        double wca = Math.toRadians(windingConnectionAngle);
        double dv = (step - neutralStep) * (stepVoltageIncrement / 100.0);
        double dx = 1.0 + dv * Math.cos(wca);
        double dy = dv * Math.sin(wca);
        return new Complex(dx, dy);
    }

    private double symmetricalX(double alpha0, double alphaMax0) {
        double alpha = Math.toRadians(alpha0);
        double alphaMax = Math.toRadians(alphaMax0);
        return xStepMin + (xStepMax - xStepMin) * Math.pow(Math.sin(alpha / 2) / Math.sin(alphaMax / 2), 2);
    }

    private double asymmetricalX(double alpha0, double alphaMax0) {
        double alpha = Math.toRadians(alpha0);
        double alphaMax = Math.toRadians(alphaMax0);
        double wca = Math.toRadians(windingConnectionAngle);
        double numer = Math.sin(wca) - Math.tan(alphaMax) * Math.cos(wca);
        double denom = Math.sin(wca) - Math.tan(alpha) * Math.cos(wca);
        return xStepMin + (xStepMax - xStepMin) * Math.pow(Math.tan(alpha) / Math.tan(alphaMax) * numer / denom, 2);
    }

    private CgmesTapChangerStatus statusFromTable() {
        CgmesTapChangerStatus status = new CgmesTapChangerStatus();
        PropertyBag point = point(step);
        if (point != null) {
            status.ratio = pointValue(point, CgmesNames1.RATIO, 1.0);
            status.angle = pointValue(point, "angle", 0.0);
            status.xc = pointValue(point, "x", 0.0);
            status.rc = pointValue(point, "r", 0.0);
            status.bc = pointValue(point, "b", 0.0);
            status.gc = pointValue(point, "g", 0.0);
        }
        return status;
    }
}
