/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.shortcircuit;

import com.powsybl.incubator.simulator.util.CalculationLocation;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class ShortCircuitFault extends CalculationLocation {

    public ShortCircuitFault(String busLocation, String faultId, double zfr, double zfi, ShortCircuitType type) {
        super(busLocation);
        this.zfr = zfr;
        this.zfi = zfi;
        this.type = type;
        this.faultId = faultId;
    }

    public ShortCircuitFault(String busLocation, String busLocationBiPhased, String faultId, double zfr, double zfi, ShortCircuitType type, ShortCircuitBiphasedType biphasedType) {
        super(busLocation, busLocationBiPhased);
        this.zfr = zfr;
        this.zfi = zfi;
        this.type = type;
        this.faultId = faultId;
        this.biphasedType = biphasedType;
    }


    public enum ShortCircuitType {
        TRIPHASED_GROUND,
        BIPHASED,
        BIPHASED_GROUND,
        BIPHASED_COMMON_SUPPORT,
        MONOPHASED;
    }

    public enum ShortCircuitBiphasedType {
        C1_C2,
        C1_B2,
        C1_A2;
    }

    private String faultId;

    private double zfr; //real part of the short circuit impedance Zf
    private double zfi; //imaginary part of the short circuit impedance Zf

    private ShortCircuitType type;

    private ShortCircuitBiphasedType biphasedType;

    public ShortCircuitType getType() {
        return type;
    }

    public double getZfr() {
        return zfr;
    }

    public double getZfi() {
        return zfi;
    }

    public ShortCircuitBiphasedType getBiphasedType() {
        return biphasedType;
    }

    public String getFaultId() {
        return faultId;
    }
}
