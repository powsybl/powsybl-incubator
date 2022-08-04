/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util;

import com.powsybl.incubator.simulator.util.extensions.AdditionalDataInfo;
import com.powsybl.math.matrix.MatrixFactory;
import com.powsybl.openloadflow.ac.outerloop.AcLoadFlowParameters;

import java.util.List;
import java.util.Objects;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class TheveninEquivalentParameters {

    public enum TheveninVoltageProfileType {
        CALCULATED, // use the computed values at nodes to compute Zth and Eth
        NOMINAL; // use the nominal voltage values at nodes to get Zth and Eth
    }

    public enum TheveninPeriodType {
        THEVENIN_SUB_TRANSIENT, //uses subTransient parameters x"d
        THEVENIN_TRANSIENT,     //uses transient parameters x'd
        THEVENIN_STEADY_STATE;
    }

    public static final double XSUBTRANSIENT = 0.2; //default value if data not available

    private final boolean voltageUpdate;

    private final AcLoadFlowParameters acLoadFlowParameters;

    private final MatrixFactory matrixFactory;

    private final List<CalculationLocation> theveninVoltageLevelLocation;

    private AdditionalDataInfo additionalDataInfo;

    private final boolean theveninIgnoreShunts;

    private final TheveninVoltageProfileType theveninVoltageProfileType;

    private final TheveninPeriodType theveninPeriodType;

    private ShortCircuitNorm norm;

    public TheveninEquivalentParameters(AcLoadFlowParameters acLoadFlowParameters, MatrixFactory matrixFactory, List<CalculationLocation> voltageLevels, boolean voltageUpdate, TheveninVoltageProfileType theveninVoltageProfileType, TheveninPeriodType theveninPeriodType, boolean theveninIgnoreShunts, AdditionalDataInfo additionalDataInfo, ShortCircuitNorm norm) {
        this.acLoadFlowParameters = Objects.requireNonNull(acLoadFlowParameters);
        this.matrixFactory = Objects.requireNonNull(matrixFactory);
        this.theveninVoltageLevelLocation = Objects.requireNonNull(voltageLevels);
        this.voltageUpdate = voltageUpdate;
        this.theveninIgnoreShunts = theveninIgnoreShunts;
        this.theveninVoltageProfileType = theveninVoltageProfileType;
        this.additionalDataInfo = additionalDataInfo;
        this.theveninPeriodType = theveninPeriodType;
        this.norm = norm;
    }

    public AcLoadFlowParameters getAcLoadFlowParameters() {
        return acLoadFlowParameters;
    }

    public MatrixFactory getMatrixFactory() {
        return matrixFactory;
    }

    public List<CalculationLocation> getFaults() {
        return theveninVoltageLevelLocation;
    }

    public boolean isVoltageUpdate() {
        return voltageUpdate;
    }

    public boolean isTheveninIgnoreShunts() {
        return theveninIgnoreShunts;
    }

    public TheveninVoltageProfileType getTheveninVoltageProfileType() {
        return theveninVoltageProfileType;
    }

    public AdditionalDataInfo getAdditionalDataInfo() {
        return additionalDataInfo;
    }

    public TheveninPeriodType getTheveninPeriodType() {
        return theveninPeriodType;
    }

    public ShortCircuitNorm getNorm() {
        return norm;
    }
}
