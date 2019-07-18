/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import com.powsybl.substationdiagram.library.ComponentType;

/**
 * Common interface for objects which expose node attributes,
 * in particular a 2D position and the underlying component type.
 *
 * TODO: is it useful to share this interface between actual nodes and handlers ??
 *
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public interface BaseNode {

    String getId();

    ComponentType getComponentType();

    boolean isRotated();

    double getX();

    double getY();
}
