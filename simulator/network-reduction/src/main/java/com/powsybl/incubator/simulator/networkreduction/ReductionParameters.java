/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.networkreduction;

import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.math.matrix.MatrixFactory;
import com.powsybl.openloadflow.network.util.PreviousValueVoltageInitializer;
import com.powsybl.openloadflow.network.util.VoltageInitializer;

import java.util.List;
import java.util.Objects;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class ReductionParameters {

    private final LoadFlowParameters loadFlowParameters;

    private final MatrixFactory matrixFactory;

    private VoltageInitializer voltageInitializer = new PreviousValueVoltageInitializer(); //TODO: check why previous does not work

    private final List<String> externalVoltageLevels;

    private final ReductionEngine.ReductionType reductionType;

    public ReductionParameters(LoadFlowParameters loadFlowParameters, MatrixFactory matrixFactory, List<String> externalVoltageLevels, ReductionEngine.ReductionType reductionType) {
        this.loadFlowParameters = Objects.requireNonNull(loadFlowParameters);
        this.matrixFactory = Objects.requireNonNull(matrixFactory);
        this.externalVoltageLevels = Objects.requireNonNull(externalVoltageLevels);
        this.reductionType = Objects.requireNonNull(reductionType);
    }

    public LoadFlowParameters getLoadFlowParameters() {
        return loadFlowParameters;
    }

    public MatrixFactory getMatrixFactory() {
        return matrixFactory;
    }

    public VoltageInitializer getVoltageInitializer() {
        return voltageInitializer;
    }

    public List<String> getExternalVoltageLevels() {
        return externalVoltageLevels;
    }

    public ReductionEngine.ReductionType getReductionType() {
        return reductionType;
    }
}
