/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowFactory;
import com.powsybl.math.matrix.MatrixFactory;
import com.powsybl.math.matrix.SparseMatrixFactory;

import java.util.Objects;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public class SimpleAcLoadFlowFactory implements LoadFlowFactory {

    private final MatrixFactory matrixFactory;

    public SimpleAcLoadFlowFactory() {
        this(new SparseMatrixFactory());
    }

    public SimpleAcLoadFlowFactory(MatrixFactory matrixFactory) {
        this.matrixFactory = Objects.requireNonNull(matrixFactory);
    }

    @Override
    public LoadFlow create(Network network, ComputationManager computationManager, int i) {
        return new SimpleAcLoadFlow(network, matrixFactory);
    }
}
