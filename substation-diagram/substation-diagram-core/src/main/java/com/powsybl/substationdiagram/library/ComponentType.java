/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.library;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public enum ComponentType {
    BUSBAR_SECTION,
    BREAKER,
    DISCONNECTOR,
    GENERATOR,
    LINE,
    LOAD,
    LOAD_BREAK_SWITCH,
    NODE,
    CAPACITOR,
    INDUCTOR,
    STATIC_VAR_COMPENSATOR,
    TWO_WINDINGS_TRANSFORMER,
    VSC_CONVERTER_STATION,
    DANGLING_LINE
}
