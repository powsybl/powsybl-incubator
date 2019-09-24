/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ieeecdf.model;

import com.ancientprogramming.fixedformat4j.annotation.Align;
import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatDecimal;
import com.ancientprogramming.fixedformat4j.annotation.Record;

import java.util.Objects;

/**
 * Columns  1- 4   Tap bus number (I) *
 *                  For transformers or phase shifters, the side of the model
 *                  the non-unity tap is on
 * Columns  6- 9   Z bus number (I) *
 *                  For transformers and phase shifters, the side of the model
 *                  the device impedance is on.
 * Columns 11-12   Load flow area (I)
 * Columns 13-14   Loss zone (I)
 * Column  17      Circuit (I) * (Use 1 for single lines)
 * Column  19      Type (I) *
 *                  0 - Transmission line
 *                  1 - Fixed tap
 *                  2 - Variable tap for voltage control (TCUL, LTC)
 *                  3 - Variable tap (turns ratio) for MVAR control
 *                  4 - Variable phase angle for MW control (phase shifter)
 * Columns 20-29   Branch resistance R, per unit (F) *
 * Columns 30-40   Branch reactance X, per unit (F) * No zero impedance lines
 * Columns 41-50   Line charging B, per unit (F) * (total line charging, +B)
 * Columns 51-55   Line MVA rating No 1 (I) Left justify!
 * Columns 57-61   Line MVA rating No 2 (I) Left justify!
 * Columns 63-67   Line MVA rating No 3 (I) Left justify!
 * Columns 69-72   Control bus number
 * Column  74      Side (I)
 *                  0 - Controlled bus is one of the terminals
 *                  1 - Controlled bus is near the tap side
 *                  2 - Controlled bus is near the impedance side (Z bus)
 * Columns 77-82   Transformer final turns ratio (F)
 * Columns 84-90   Transformer (phase shifter) final angle (F)
 * Columns 91-97   Minimum tap or phase shift (F)
 * Columns 98-104  Maximum tap or phase shift (F)
 * Columns 106-111 Step size (F)
 * Columns 113-119 Minimum voltage, MVAR or MW limit (F)
 * Columns 120-126 Maximum voltage, MVAR or MW limit (F)
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@Record
public class IeeeCdfBranch {

    /**
     * 0 - Transmission line
     * 1 - Fixed tap
     * 2 - Variable tap for voltage control (TCUL, LTC)
     * 3 - Variable tap (turns ratio) for MVAR control
     * 4 - Variable phase angle for MW control (phase shifter)
     */
    enum Type {
        TRANSMISSION_LINE,
        FIXED_TAP,
        VARIABLE_TAP_FOR_VOLTAVE_CONTROL,
        VARIABLE_TAP_FOR_REACTIVE_POWER_CONTROL,
        VARIABLE_PHASE_ANGLE_FOR_ACTIVE_POWER_CONTROL,
    }

    enum Side {
        CONTROLLED_BUS_IS_ONE_OF_THE_TERMINALS,
        CONTROLLED_BUS_IS_NEAR_THE_TAP_SIDE,
        CONTROLLED_BUS_IS_NEAR_THE_IMPEDANCE_SIDE
    }

    private int tapBusNumber;
    private int zBusNumber;
    private int area;
    private int lossZone;
    private int circuit;
    private Type type;
    private float resistance;
    private float reactance;
    private float chargingSusceptance;
    private int rating1;
    private int rating2;
    private int rating3;
    private int controlBusNumber;
    private Side side;
    private float finalTurnsRatio;
    private float finalAngle;
    private float minTapOrPhaseShift;
    private float maxTapOrPhaseShift;
    private float stepSize;
    private float minVoltageActiveOrReactivePowerLimit;
    private float maxVoltageActiveOrReactivePowerLimit;

    /**
     * Tap bus number (I) *
     */
    @Field(offset = 1, length = 4, align = Align.RIGHT)
    public int getTapBusNumber() {
        return tapBusNumber;
    }

    public void setTapBusNumber(int tapBusNumber) {
        this.tapBusNumber = tapBusNumber;
    }

    /**
     * Z bus number (I) *
     */
    @Field(offset = 6, length = 4, align = Align.RIGHT)
    public int getzBusNumber() {
        return zBusNumber;
    }

    public void setzBusNumber(int zBusNumber) {
        this.zBusNumber = zBusNumber;
    }

    /**
     * Load flow area (I)
     */
    @Field(offset = 11, length = 2, align = Align.RIGHT)
    public int getArea() {
        return area;
    }

    public void setArea(int area) {
        this.area = area;
    }

    /**
     * Loss zone (I)
     */
    @Field(offset = 13, length = 2)
    public int getLossZone() {
        return lossZone;
    }

    public void setLossZone(int lossZone) {
        this.lossZone = lossZone;
    }

    /**
     * Circuit (I) * (Use 1 for single lines)
     */
    @Field(offset = 17, length = 1)
    public int getCircuit() {
        return circuit;
    }

    public void setCircuit(int circuit) {
        this.circuit = circuit;
    }

    /**
     * Type (I) *
     */
    @Field(offset = 19, length = 1, formatter = BranchTypeFormatter.class)
    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    /**
     * Branch resistance R, per unit (F) *
     */
    @Field(offset = 20, length = 10)
    @FixedFormatDecimal(decimals = 5, useDecimalDelimiter = true)
    public float getResistance() {
        return resistance;
    }

    public void setResistance(float resistance) {
        this.resistance = resistance;
    }

    /**
     * Branch reactance X, per unit (F) * No zero impedance lines
     */
    @Field(offset = 30, length = 11)
    @FixedFormatDecimal(decimals = 5, useDecimalDelimiter = true)
    public float getReactance() {
        return reactance;
    }

    public void setReactance(float reactance) {
        this.reactance = reactance;
    }

    /**
     * Line charging B, per unit (F) * (total line charging, +B)
     */
    @Field(offset = 41, length = 10)
    @FixedFormatDecimal(decimals = 5, useDecimalDelimiter = true)
    public float getChargingSusceptance() {
        return chargingSusceptance;
    }

    public void setChargingSusceptance(float chargingSusceptance) {
        this.chargingSusceptance = chargingSusceptance;
    }

    /**
     * Line MVA rating No 1 (I) Left justify!
     */
    @Field(offset = 51, length = 5, align = Align.RIGHT)
    public int getRating1() {
        return rating1;
    }

    public void setRating1(int rating1) {
        this.rating1 = rating1;
    }

    /**
     * Line MVA rating No 2 (I) Left justify!
     */
    @Field(offset = 57, length = 5, align = Align.RIGHT)
    public int getRating2() {
        return rating2;
    }

    public void setRating2(int rating2) {
        this.rating2 = rating2;
    }

    /**
     * Line MVA rating No 3 (I) Left justify!
     */
    @Field(offset = 63, length = 5, align = Align.RIGHT)
    public int getRating3() {
        return rating3;
    }

    public void setRating3(int rating3) {
        this.rating3 = rating3;
    }

    /**
     * Control bus number
     */
    @Field(offset = 69, length = 4, align = Align.RIGHT)
    public int getControlBusNumber() {
        return controlBusNumber;
    }

    public void setControlBusNumber(int controlBusNumber) {
        this.controlBusNumber = controlBusNumber;
    }

    /**
     * Side (I)
     */
    @Field(offset = 74, length = 1, align = Align.RIGHT, formatter = BranchSideFormatter.class)
    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }

    /**
     * Transformer final turns ratio (F)
     */
    @Field(offset = 77, length = 6)
    @FixedFormatDecimal(decimals = 3, useDecimalDelimiter = true)
    public float getFinalTurnsRatio() {
        return finalTurnsRatio;
    }

    public void setFinalTurnsRatio(float finalTurnsRatio) {
        this.finalTurnsRatio = finalTurnsRatio;
    }

    /**
     * Transformer (phase shifter) final angle (F)
     */
    @Field(offset = 84, length = 7)
    @FixedFormatDecimal(decimals = 2, useDecimalDelimiter = true)
    public float getFinalAngle() {
        return finalAngle;
    }

    public void setFinalAngle(float finalAngle) {
        this.finalAngle = finalAngle;
    }

    /**
     * Minimum tap or phase shift (F)
     */
    @Field(offset = 91, length = 7)
    @FixedFormatDecimal(decimals = 5, useDecimalDelimiter = true)
    public float getMinTapOrPhaseShift() {
        return minTapOrPhaseShift;
    }

    public void setMinTapOrPhaseShift(float minTapOrPhaseShift) {
        this.minTapOrPhaseShift = minTapOrPhaseShift;
    }

    /**
     * Maximum tap or phase shift (F)
     */
    @Field(offset = 98, length = 7)
    @FixedFormatDecimal(decimals = 5, useDecimalDelimiter = true)
    public float getMaxTapOrPhaseShift() {
        return maxTapOrPhaseShift;
    }

    public void setMaxTapOrPhaseShift(float maxTapOrPhaseShift) {
        this.maxTapOrPhaseShift = maxTapOrPhaseShift;
    }

    /**
     * Step size (F)
     */
    @Field(offset = 106, length = 6)
    @FixedFormatDecimal(decimals = 5, useDecimalDelimiter = true)
    public float getStepSize() {
        return stepSize;
    }

    public void setStepSize(float stepSize) {
        this.stepSize = stepSize;
    }

    /**
     * Minimum voltage, MVAR or MW limit (F)
     */
    @Field(offset = 113, length = 6)
    @FixedFormatDecimal(decimals = 4, useDecimalDelimiter = true)
    public float getMinVoltageActiveOrReactivePowerLimit() {
        return minVoltageActiveOrReactivePowerLimit;
    }

    public void setMinVoltageActiveOrReactivePowerLimit(float minVoltageActiveOrReactivePowerLimit) {
        this.minVoltageActiveOrReactivePowerLimit = minVoltageActiveOrReactivePowerLimit;
    }

    /**
     * Maximum voltage, MVAR or MW limit (F)
     */
    @Field(offset = 120, length = 7)
    @FixedFormatDecimal(decimals = 4, useDecimalDelimiter = true)
    public float getMaxVoltageActiveOrReactivePowerLimit() {
        return maxVoltageActiveOrReactivePowerLimit;
    }

    public void setMaxVoltageActiveOrReactivePowerLimit(float maxVoltageActiveOrReactivePowerLimit) {
        this.maxVoltageActiveOrReactivePowerLimit = maxVoltageActiveOrReactivePowerLimit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tapBusNumber, zBusNumber, area, lossZone, circuit, type, resistance, reactance,
                            chargingSusceptance, rating1, rating2, rating3, controlBusNumber, side,
                            finalTurnsRatio, finalAngle, minTapOrPhaseShift, maxTapOrPhaseShift, stepSize,
                            minVoltageActiveOrReactivePowerLimit, maxVoltageActiveOrReactivePowerLimit);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IeeeCdfBranch) {
            IeeeCdfBranch other = (IeeeCdfBranch) obj;
            return tapBusNumber == other.tapBusNumber
                    && zBusNumber == other.zBusNumber
                    && area == other.area
                    && lossZone == other.lossZone
                    && circuit == other.circuit
                    && type == other.type
                    && resistance == other.resistance
                    && reactance == other.reactance
                    && chargingSusceptance == other.chargingSusceptance
                    && rating1 == other.rating1
                    && rating2 == other.rating2
                    && rating3 == other.rating3
                    && controlBusNumber == other.controlBusNumber
                    && side == other.side
                    && finalTurnsRatio == other.finalTurnsRatio
                    && finalAngle == other.finalAngle
                    && minTapOrPhaseShift == other.minTapOrPhaseShift
                    && maxTapOrPhaseShift == other.maxTapOrPhaseShift
                    && stepSize == other.stepSize
                    && minVoltageActiveOrReactivePowerLimit == other.minVoltageActiveOrReactivePowerLimit
                    && maxVoltageActiveOrReactivePowerLimit == other.maxVoltageActiveOrReactivePowerLimit;
        }
        return false;
    }
}
