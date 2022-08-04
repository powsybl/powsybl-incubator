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
public class AdmittanceLinearResolutionParameters {

    /*public enum AdmittanceLinearVoltageProfileType {
        CALCULATED, // use the computed values at nodes to compute Zth and Eth
        NOMINAL; // use the nominal voltage values at nodes to get Zth and Eth
    }

    public enum AdmittanceLinearPeriodType {
        ADM_SUB_TRANSIENT, //uses subTransient parameters x"d
        ADM_TRANSIENT,     //uses transient parameters x'd
        ADM_STEADY_STATE;
    }*/

    public static final double XSUBTRANSIENT = 0.2; //default value if data not available

    private final boolean voltageUpdate;

    private final AcLoadFlowParameters acLoadFlowParameters;

    private final MatrixFactory matrixFactory;

    //private final List<String> theveninLocations; // TODO: compute Thevenin for a list of nodes in input
    private final List<CalculationLocation> calculationLocations;

    private List<CalculationLocation>  biphasedCalculationLocations;

    private AdditionalDataInfo additionalDataInfo;

    private final boolean ignoreShunts;

    private final AdmittanceEquationSystem.AdmittanceVoltageProfileType voltageProfileType;

    private final AdmittanceEquationSystem.AdmittancePeriodType periodType;

    private final AdmittanceEquationSystem.AdmittanceType admittanceType;

    public AdmittanceLinearResolutionParameters(AcLoadFlowParameters acLoadFlowParameters, MatrixFactory matrixFactory, List<CalculationLocation> calculationLocations, boolean voltageUpdate,
                                                AdmittanceEquationSystem.AdmittanceVoltageProfileType theveninVoltageProfileType, AdmittanceEquationSystem.AdmittancePeriodType theveninPeriodType, AdmittanceEquationSystem.AdmittanceType admittanceType,
                                                boolean theveninIgnoreShunts, AdditionalDataInfo additionalDataInfo) {
        this.acLoadFlowParameters = Objects.requireNonNull(acLoadFlowParameters);
        this.matrixFactory = Objects.requireNonNull(matrixFactory);
        this.calculationLocations = Objects.requireNonNull(calculationLocations);
        this.voltageUpdate = voltageUpdate;
        this.ignoreShunts = theveninIgnoreShunts;
        this.voltageProfileType = theveninVoltageProfileType;
        this.additionalDataInfo = additionalDataInfo;
        this.periodType = theveninPeriodType;
        this.admittanceType = admittanceType;
    }

    public AdmittanceLinearResolutionParameters(AcLoadFlowParameters acLoadFlowParameters, MatrixFactory matrixFactory, List<CalculationLocation> calculationLocations, boolean voltageUpdate,
                                                AdmittanceEquationSystem.AdmittanceVoltageProfileType theveninVoltageProfileType, AdmittanceEquationSystem.AdmittancePeriodType theveninPeriodType, AdmittanceEquationSystem.AdmittanceType admittanceType,
                                                boolean theveninIgnoreShunts, AdditionalDataInfo additionalDataInfo, List<CalculationLocation> biphasedVoltageLevelLocation) {
        this(acLoadFlowParameters, matrixFactory, calculationLocations, voltageUpdate, theveninVoltageProfileType, theveninPeriodType, admittanceType, theveninIgnoreShunts, additionalDataInfo);
        this.biphasedCalculationLocations = biphasedVoltageLevelLocation;

    }

    public AcLoadFlowParameters getAcLoadFlowParameters() {
        return acLoadFlowParameters;
    }

    public MatrixFactory getMatrixFactory() {
        return matrixFactory;
    }

    public List<CalculationLocation> getCalculationLocations() {
        return calculationLocations;
    }

    public boolean isVoltageUpdate() {
        return voltageUpdate;
    }

    public boolean isTheveninIgnoreShunts() {
        return ignoreShunts;
    }

    public AdmittanceEquationSystem.AdmittanceVoltageProfileType getTheveninVoltageProfileType() {
        return voltageProfileType;
    }

    public List<CalculationLocation>  getBiphasedCalculationLocations() {
        return biphasedCalculationLocations;
    }

    public AdditionalDataInfo getAdditionalDataInfo() {
        return additionalDataInfo;
    }

    public AdmittanceEquationSystem.AdmittancePeriodType getTheveninPeriodType() {
        return periodType;
    }

    public AdmittanceEquationSystem.AdmittanceType getAdmittanceType() {
        return admittanceType;
    }
}
