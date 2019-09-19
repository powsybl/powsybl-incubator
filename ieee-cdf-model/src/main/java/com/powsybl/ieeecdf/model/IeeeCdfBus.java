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
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;

import java.util.Objects;

/**
 * Columns  1- 4   Bus number (I) *
 * Columns  7-17   Name (A) (left justify) *
 * Columns 19-20   Load flow area number (I) Don't use zero! *
 * Columns 21-23   Loss zone number (I)
 * Columns 25-26   Type (I) *
 * 0 - Unregulated (load, PQ)
 * 1 - Hold MVAR generation within voltage limits, (PQ)
 * 2 - Hold voltage within VAR limits (gen, PV)
 * 3 - Hold voltage and angle (swing, V-Theta) (must always have one)
 * Columns 28-33   Final voltage, p.u. (F) *
 * Columns 34-40   Final angle, degrees (F) *
 * Columns 41-49   Load MW (F) *
 * Columns 50-59   Load MVAR (F) *
 * Columns 60-67   Generation MW (F) *
 * Columns 68-75   Generation MVAR (F) *
 * Columns 77-83   Base KV (F)
 * Columns 85-90   Desired volts (pu) (F) (This is desired remote voltage if this bus is controlling another bus.
 * Columns 91-98   Maximum MVAR or voltage limit (F)
 * Columns 99-106  Minimum MVAR or voltage limit (F)
 * Columns 107-114 Shunt conductance G (per unit) (F) *
 * Columns 115-122 Shunt susceptance B (per unit) (F) *
 * Columns 124-127 Remote controlled bus number
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@Record
public class IeeeCdfBus {

    /**
     * 0 - Unregulated (load, PQ)
     * 1 - Hold MVAR generation within voltage limits, (PQ)
     * 2 - Hold voltage within VAR limits (gen, PV)
     * 3 - Hold voltage and angle (swing, V-Theta) (must always have one)
     */
    enum Type {
        UNREGULATED,
        HOLD_MVAR_GENERATION_WITHIN_VOLTAGE_LIMITS,
        HOLD_VOLTAGE_WITHIN_VAR_LIMITS,
        HOLD_VOLTAGE_AND_ANGLE
    }

    private int number;
    private String name;
    private int areaNumber;
    private int lossZoneNumber;
    private Type type;
    private float finalVoltage;
    private float finalAngle;
    private float activeLoad;
    private float reactiveLoad;
    private float activeGeneration;
    private float reactiveGeneration;
    private float baseVoltage;
    private float desiredVoltage;
    private float maxReactivePowerOrVoltageLimit;
    private float minReactivePowerOrVoltageLimit;
    private float shuntConductance;
    private float shuntSusceptance;
    private int remoteControlledBusNumber;

    /**
     * Bus number
     */
    @Field(offset = 1, length = 4, align = Align.RIGHT)
    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    /**
     * Name
     */
    @Field(offset = 6, length = 12)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Load flow area number
     */
    @Field(offset = 19, length = 2, align = Align.RIGHT)
    public int getAreaNumber() {
        return areaNumber;
    }

    public void setAreaNumber(int areaNumber) {
        this.areaNumber = areaNumber;
    }

    /**
     * Loss zone number
     */
    @Field(offset = 21, length = 3, align = Align.RIGHT)
    public int getLossZoneNumber() {
        return lossZoneNumber;
    }

    public void setLossZoneNumber(int lossZoneNumber) {
        this.lossZoneNumber = lossZoneNumber;
    }

    /**
     * Type
     */
    @Field(offset = 25, length = 2, align = Align.RIGHT, formatter = BusTypeFormatter.class)
    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    /**
     * Final voltage, p.u.
     */
    @Field(offset = 28, length = 5, align = Align.RIGHT)
    @FixedFormatDecimal(decimals = 3, useDecimalDelimiter = true)
    public float getFinalVoltage() {
        return finalVoltage;
    }

    public void setFinalVoltage(float finalVoltage) {
        this.finalVoltage = finalVoltage;
    }

    /**
     * Final angle, degrees
     */
    @Field(offset = 34, length = 6, align = Align.RIGHT)
    @FixedFormatDecimal(decimals = 2, useDecimalDelimiter = true)
    public float getFinalAngle() {
        return finalAngle;
    }

    public void setFinalAngle(float finalAngle) {
        this.finalAngle = finalAngle;
    }

    /**
     * Load MW
     */
    @Field(offset = 41, length = 8, align = Align.RIGHT)
    @FixedFormatDecimal(decimals = 1, useDecimalDelimiter = true)
    public float getActiveLoad() {
        return activeLoad;
    }

    public void setActiveLoad(float activeLoad) {
        this.activeLoad = activeLoad;
    }

    /**
     * Load MVAR
     */
    @Field(offset = 50, length = 8, align = Align.RIGHT)
    @FixedFormatDecimal(decimals = 1, useDecimalDelimiter = true)
    public float getReactiveLoad() {
        return reactiveLoad;
    }

    public void setReactiveLoad(float reactiveLoad) {
        this.reactiveLoad = reactiveLoad;
    }

    /**
     * Generation MW
     */
    @Field(offset = 60, length = 7, align = Align.RIGHT)
    @FixedFormatDecimal(decimals = 1, useDecimalDelimiter = true)
    public float getActiveGeneration() {
        return activeGeneration;
    }

    public void setActiveGeneration(float activeGeneration) {
        this.activeGeneration = activeGeneration;
    }

    /**
     * Generation MVAR
     */
    @Field(offset = 68, length = 7, align = Align.RIGHT)
    @FixedFormatDecimal(decimals = 1, useDecimalDelimiter = true)
    public float getReactiveGeneration() {
        return reactiveGeneration;
    }

    public void setReactiveGeneration(float reactiveGeneration) {
        this.reactiveGeneration = reactiveGeneration;
    }

    /**
     * Base KV
     */
    @Field(offset = 77, length = 6, align = Align.RIGHT)
    @FixedFormatDecimal(decimals = 1, useDecimalDelimiter = true)
    public float getBaseVoltage() {
        return baseVoltage;
    }

    public void setBaseVoltage(float baseVoltage) {
        this.baseVoltage = baseVoltage;
    }

    /**
     * Desired volts (pu) (F) (This is desired remote voltage if this bus is controlling another bus.
     */
    @Field(offset = 85, length = 5, align = Align.LEFT)
    @FixedFormatDecimal(decimals = 3, useDecimalDelimiter = true)
    public float getDesiredVoltage() {
        return desiredVoltage;
    }

    public void setDesiredVoltage(float desiredVoltage) {
        this.desiredVoltage = desiredVoltage;
    }

    /**
     * Maximum MVAR or voltage limit
     */
    @Field(offset = 91, length = 7, align = Align.RIGHT)
    @FixedFormatDecimal(decimals = 1, useDecimalDelimiter = true)
    public float getMaxReactivePowerOrVoltageLimit() {
        return maxReactivePowerOrVoltageLimit;
    }

    public void setMaxReactivePowerOrVoltageLimit(float maxReactivePowerOrVoltageLimit) {
        this.maxReactivePowerOrVoltageLimit = maxReactivePowerOrVoltageLimit;
    }

    /**
     * Minimum MVAR or voltage limit
     */
    @Field(offset = 99, length = 7, align = Align.RIGHT)
    @FixedFormatDecimal(decimals = 1, useDecimalDelimiter = true)
    public float getMinReactivePowerOrVoltageLimit() {
        return minReactivePowerOrVoltageLimit;
    }

    public void setMinReactivePowerOrVoltageLimit(float minReactivePowerOrVoltageLimit) {
        this.minReactivePowerOrVoltageLimit = minReactivePowerOrVoltageLimit;
    }

    /**
     * Shunt conductance G (per unit)
     */
    @Field(offset = 106, length = 6, align = Align.RIGHT)
    @FixedFormatDecimal(decimals = 2, useDecimalDelimiter = true)
    public float getShuntConductance() {
        return shuntConductance;
    }

    public void setShuntConductance(float shuntConductance) {
        this.shuntConductance = shuntConductance;
    }

    /**
     * Shunt susceptance B (per unit)
     */
    @Field(offset = 112, length = 7, align = Align.RIGHT)
    @FixedFormatDecimal(decimals = 2, useDecimalDelimiter = true)
    public float getShuntSusceptance() {
        return shuntSusceptance;
    }

    public void setShuntSusceptance(float shuntSusceptance) {
        this.shuntSusceptance = shuntSusceptance;
    }

    /**
     * Remote controlled bus number
     */
    @Field(offset = 124, length = 4, align = Align.RIGHT)
    public int getRemoteControlledBusNumber() {
        return remoteControlledBusNumber;
    }

    public void setRemoteControlledBusNumber(int remoteControlledBusNumber) {
        this.remoteControlledBusNumber = remoteControlledBusNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, name, areaNumber, lossZoneNumber, type, finalVoltage, finalAngle,
                            activeLoad, reactiveLoad, activeGeneration, reactiveGeneration, baseVoltage,
                            desiredVoltage, maxReactivePowerOrVoltageLimit, minReactivePowerOrVoltageLimit,
                            shuntConductance, shuntSusceptance, remoteControlledBusNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IeeeCdfBus) {
            IeeeCdfBus other = (IeeeCdfBus) obj;
            return number == other.number
                    && Objects.equals(name, other.name)
                    && lossZoneNumber == other.lossZoneNumber
                    && type == other.type
                    && finalVoltage == other.finalVoltage
                    && finalAngle == other.finalAngle
                    && activeLoad == other.activeLoad
                    && reactiveLoad == other.reactiveLoad
                    && activeGeneration == other.activeGeneration
                    && reactiveGeneration == other.reactiveGeneration
                    && baseVoltage == other.baseVoltage
                    && desiredVoltage == other.desiredVoltage
                    && maxReactivePowerOrVoltageLimit == other.maxReactivePowerOrVoltageLimit
                    && minReactivePowerOrVoltageLimit == other.minReactivePowerOrVoltageLimit
                    && shuntConductance == other.shuntConductance
                    && shuntSusceptance == other.shuntSusceptance
                    && remoteControlledBusNumber == other.remoteControlledBusNumber;
        }
        return false;
    }

    public static void main(String[] args) {
        String a = "   1 Bus 1     HV  1  1  3 1.060    0.0      0.0      0.0    232.4   -16.9     0.0  1.060     0.0     0.0   0.0    0.0        0";
        FixedFormatManager manager = new FixedFormatManagerImpl();
        IeeeCdfBus record = manager.load(IeeeCdfBus.class, a);
        System.out.println(a);
        System.out.println(manager.export(record));
    }
}
