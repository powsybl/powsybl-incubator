/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class NewtonRaphsonResult {

    private int iterations;

    private NewtonRaphsonStatus status;

    private double slackBusActivePowerMismatch;

    public NewtonRaphsonResult(NewtonRaphsonStatus status, int iterations, double slackBusActivePowerMismatch) {
        if (iterations < 0) {
            throw new IllegalArgumentException("Invalid iteration count: " + iterations);
        }
        this.status = Objects.requireNonNull(status);
        this.iterations = iterations;
        this.slackBusActivePowerMismatch = slackBusActivePowerMismatch;
    }

    public NewtonRaphsonStatus getStatus() {
        return status;
    }

    public int getIterations() {
        return iterations;
    }

    public double getSlackBusActivePowerMismatch() {
        return slackBusActivePowerMismatch;
    }
}
