/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.network;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public interface LfBranch {

    String getId();

    LfBus getBus1();

    LfBus getBus2();

    void setP1(double p1);

    void setP2(double p2);

    void setQ1(double q1);

    void setQ2(double q2);

    double r();

    double x();

    double z();

    double y();

    double ksi();

    double g1();

    double g2();

    double b1();

    double b2();

    double r1();

    double r2();

    double a1();

    double a2();
}
