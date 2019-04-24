/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.interpretation.model.cgmes;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
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
