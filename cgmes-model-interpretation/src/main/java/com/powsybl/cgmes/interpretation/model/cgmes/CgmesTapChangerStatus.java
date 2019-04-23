package com.powsybl.cgmes.interpretation.model.cgmes;

public class CgmesTapChangerStatus {
    double ratio = 1.0;
    double angle = 0.0;
    double rc = 0.0;
    double xc = 0.0;
    double bc = 0.0;
    double gc = 0.0;

    public double ratio() {
        return ratio;
    }

    public double angle() {
        return angle;
    }

    public double rc() {
        return rc;
    }

    public double xc() {
        return xc;
    }

    public double bc() {
        return bc;
    }

    public double gc() {
        return gc;
    }
}
