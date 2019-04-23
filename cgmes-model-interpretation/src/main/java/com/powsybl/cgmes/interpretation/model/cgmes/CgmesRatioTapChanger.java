package com.powsybl.cgmes.interpretation.model.cgmes;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.CgmesNames1;
import com.powsybl.triplestore.api.PropertyBag;

public class CgmesRatioTapChanger extends CgmesTapChanger {
    public static final CgmesRatioTapChanger EMPTY = new CgmesRatioTapChanger();

    private CgmesRatioTapChanger() {
        stepVoltageIncrement = 0;
    }

    CgmesRatioTapChanger(PropertyBag rtcp, CgmesModel cgmes) {
        super(rtcp, "RatioTapChangerTable", cgmes);
        this.stepVoltageIncrement = rtcp.asDouble("stepVoltageIncrement");
    }

    public boolean hasStepVoltageIncrement() {
        return stepVoltageIncrement != 0;
    }

    final double stepVoltageIncrement;

    public CgmesTapChangerStatus status() {
        CgmesTapChangerStatus status;
        if (table != null) {
            status = statusFromTable();
        } else {
            status = new CgmesTapChangerStatus();
            status.ratio = 1.0 + (step - neutralStep) * (stepVoltageIncrement / 100.0);
            status.angle = 0.0;
        }
        return status;
    }

    public boolean hasDifferentRatios() {
        if (stepVoltageIncrement != 0 && lowStep != highStep) {
            return true;
        }
        return tabularDifferentRatios;
    }

    private CgmesTapChangerStatus statusFromTable() {
        CgmesTapChangerStatus status = new CgmesTapChangerStatus();
        PropertyBag point = point(step);
        if (point != null) {
            status.ratio = pointValue(point, CgmesNames1.RATIO, 1.0);
            status.xc = pointValue(point, "x", 0.0);
            status.rc = pointValue(point, "r", 0.0);
            status.bc = pointValue(point, "b", 0.0);
            status.gc = pointValue(point, "g", 0.0);
        }
        return status;
    }
}