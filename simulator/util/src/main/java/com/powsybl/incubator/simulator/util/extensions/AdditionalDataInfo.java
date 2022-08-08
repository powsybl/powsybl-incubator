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

    private Map<String, LegConnectionType> leg1type;
    private Map<String, LegConnectionType> leg2type;
    private Map<String, LegConnectionType> leg3type;

    private Map<String, Double> tfo3wIdToLeg1CoeffRo;
    private Map<String, Double> tfo3wIdToLeg1CoeffXo;
    private Map<String, Double> tfo3wIdToLeg2CoeffRo;
    private Map<String, Double> tfo3wIdToLeg2CoeffXo;
    private Map<String, Double> tfo3wIdToLeg3CoeffRo;
    private Map<String, Double> tfo3wIdToLeg3CoeffXo;

    private Map<String, Boolean> tfo3wIdLeg1ToFreeFluxes;
    private Map<String, Boolean> tfo3wIdLeg2ToFreeFluxes;
    private Map<String, Boolean> tfo3wIdLeg3ToFreeFluxes;

    public AdditionalDataInfo() {
    }

    public AdditionalDataInfo(Map<String, LegConnectionType> leg1type, Map<String, LegConnectionType> leg2type) {
        this.leg1type = leg1type;
        this.leg2type = leg2type;
    }

    public AdditionalDataInfo(Map<String, LegConnectionType> leg1type, Map<String, LegConnectionType> leg2type,
                              Map<String, LegConnectionType> leg3type,
                              Map<String, Double> tfo3wIdToLeg1CoeffRo, Map<String, Double> tfo3wIdToLeg1CoeffXo,
                              Map<String, Double> tfo3wIdToLeg2CoeffRo, Map<String, Double> tfo3wIdToLeg2CoeffXo,
                              Map<String, Double> tfo3wIdToLeg3CoeffRo, Map<String, Double> tfo3wIdToLeg3CoeffXo,
                              Map<String, Boolean> tfo3wIdLeg1ToFreeFluxes, Map<String, Boolean> tfo3wIdLeg2ToFreeFluxes, Map<String, Boolean> tfo3wIdLeg3ToFreeFluxes) {
        this(leg1type, leg2type);
        this.leg3type = leg3type;
        this.tfo3wIdToLeg1CoeffRo = tfo3wIdToLeg1CoeffRo;
        this.tfo3wIdToLeg1CoeffXo = tfo3wIdToLeg1CoeffXo;
        this.tfo3wIdToLeg2CoeffRo = tfo3wIdToLeg2CoeffRo;
        this.tfo3wIdToLeg2CoeffXo = tfo3wIdToLeg2CoeffXo;
        this.tfo3wIdToLeg3CoeffRo = tfo3wIdToLeg3CoeffRo;
        this.tfo3wIdToLeg3CoeffXo = tfo3wIdToLeg3CoeffXo;
        this.tfo3wIdLeg1ToFreeFluxes = tfo3wIdLeg1ToFreeFluxes;
        this.tfo3wIdLeg2ToFreeFluxes = tfo3wIdLeg2ToFreeFluxes;
        this.tfo3wIdLeg3ToFreeFluxes = tfo3wIdLeg3ToFreeFluxes;

    }

    public Map<String, LegConnectionType> getLeg1type() {
        return leg1type;
    }

    public Map<String, LegConnectionType> getLeg2type() {
        return leg2type;
    }

    public Map<String, LegConnectionType> getLeg3type() {
        return leg3type;
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
