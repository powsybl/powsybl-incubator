/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util.extensions;

import java.util.Map;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
//This class is used to carry additional info necessary for shortcircuit calculation that are not yet modeled in the iidm network or in the short circuit API
public class AdditionalDataInfo {

    public enum LegType {
        Y,
        Y_GROUNDED,
        DELTA;
    }

    private Map<String, LegType> leg1type;
    private Map<String, LegType> leg2type;
    private Map<String, LegType> leg3type;

    private Map<String, Double> tfo2wIdToCoeffRo;
    private Map<String, Double> tfo2wIdToCoeffXo;

    private Map<String, Double> tfo3wIdToLeg1CoeffRo;
    private Map<String, Double> tfo3wIdToLeg1CoeffXo;
    private Map<String, Double> tfo3wIdToLeg2CoeffRo;
    private Map<String, Double> tfo3wIdToLeg2CoeffXo;
    private Map<String, Double> tfo3wIdToLeg3CoeffRo;
    private Map<String, Double> tfo3wIdToLeg3CoeffXo;

    private Map<String, Boolean> tfo2wIdToFreeFluxes; // free fluxes mean that magnetizing impedance Zm is infinite, by default, fluxes are forced and Zm exists
    private Map<String, Boolean> tfo3wIdLeg1ToFreeFluxes;
    private Map<String, Boolean> tfo3wIdLeg2ToFreeFluxes;
    private Map<String, Boolean> tfo3wIdLeg3ToFreeFluxes;

    public AdditionalDataInfo() {
    }

    public AdditionalDataInfo(Map<String, LegType> leg1type, Map<String, LegType> leg2type) {
        this.leg1type = leg1type;
        this.leg2type = leg2type;
    }

    public AdditionalDataInfo(Map<String, LegType> leg1type, Map<String, LegType> leg2type, Map<String, Double> tfo2wIdToCoeffRo, Map<String, Double> tfo2wIdToCoeffXo) {
        this(leg1type, leg2type);
        this.tfo2wIdToCoeffRo = tfo2wIdToCoeffRo;
        this.tfo2wIdToCoeffXo = tfo2wIdToCoeffXo;

    }

    public AdditionalDataInfo(Map<String, LegType> leg1type, Map<String, LegType> leg2type, Map<String, Double> tfo2wIdToCoeffRo, Map<String, Double> tfo2wIdToCoeffXo,
                              Map<String, LegType> leg3type,
                              Map<String, Double> tfo3wIdToLeg1CoeffRo, Map<String, Double> tfo3wIdToLeg1CoeffXo,
                              Map<String, Double> tfo3wIdToLeg2CoeffRo, Map<String, Double> tfo3wIdToLeg2CoeffXo,
                              Map<String, Double> tfo3wIdToLeg3CoeffRo, Map<String, Double> tfo3wIdToLeg3CoeffXo,
                              Map<String, Boolean> tfo2wIdToFreeFluxes, Map<String, Boolean> tfo3wIdLeg1ToFreeFluxes, Map<String, Boolean> tfo3wIdLeg2ToFreeFluxes, Map<String, Boolean> tfo3wIdLeg3ToFreeFluxes) {
        this(leg1type, leg2type, tfo2wIdToCoeffRo, tfo2wIdToCoeffXo);
        this.leg3type = leg3type;
        this.tfo3wIdToLeg1CoeffRo = tfo3wIdToLeg1CoeffRo;
        this.tfo3wIdToLeg1CoeffXo = tfo3wIdToLeg1CoeffXo;
        this.tfo3wIdToLeg2CoeffRo = tfo3wIdToLeg2CoeffRo;
        this.tfo3wIdToLeg2CoeffXo = tfo3wIdToLeg2CoeffXo;
        this.tfo3wIdToLeg3CoeffRo = tfo3wIdToLeg3CoeffRo;
        this.tfo3wIdToLeg3CoeffXo = tfo3wIdToLeg3CoeffXo;
        this.tfo2wIdToFreeFluxes = tfo2wIdToFreeFluxes;
        this.tfo3wIdLeg1ToFreeFluxes = tfo3wIdLeg1ToFreeFluxes;
        this.tfo3wIdLeg2ToFreeFluxes = tfo3wIdLeg2ToFreeFluxes;
        this.tfo3wIdLeg3ToFreeFluxes = tfo3wIdLeg3ToFreeFluxes;

    }

    public Map<String, LegType> getLeg1type() {
        return leg1type;
    }

    public Map<String, LegType> getLeg2type() {
        return leg2type;
    }

    public Map<String, LegType> getLeg3type() {
        return leg3type;
    }

    public Map<String, Double> getTfo2wIdToCoeffRo() {
        return tfo2wIdToCoeffRo;
    }

    public Map<String, Double> getTfo2wIdToCoeffXo() {
        return tfo2wIdToCoeffXo;
    }

    public Map<String, Double> getTfo3wIdToLeg1CoeffRo() {
        return tfo3wIdToLeg1CoeffRo;
    }

    public Map<String, Double> getTfo3wIdToLeg1CoeffXo() {
        return tfo3wIdToLeg1CoeffXo;
    }

    public Map<String, Double> getTfo3wIdToLeg2CoeffRo() {
        return tfo3wIdToLeg2CoeffRo;
    }

    public Map<String, Double> getTfo3wIdToLeg2CoeffXo() {
        return tfo3wIdToLeg2CoeffXo;
    }

    public Map<String, Double> getTfo3wIdToLeg3CoeffRo() {
        return tfo3wIdToLeg3CoeffRo;
    }

    public Map<String, Double> getTfo3wIdToLeg3CoeffXo() {
        return tfo3wIdToLeg3CoeffXo;
    }

    public Map<String, Boolean> getTfo2wIdToFreeFluxes() {
        return tfo2wIdToFreeFluxes;
    }

    public Map<String, Boolean> getTfo3wIdLeg1ToFreeFluxes() {
        return tfo3wIdLeg1ToFreeFluxes;
    }

    public Map<String, Boolean> getTfo3wIdLeg2ToFreeFluxes() {
        return tfo3wIdLeg2ToFreeFluxes;
    }

    public Map<String, Boolean> getTfo3wIdLeg3ToFreeFluxes() {
        return tfo3wIdLeg3ToFreeFluxes;
    }
}
