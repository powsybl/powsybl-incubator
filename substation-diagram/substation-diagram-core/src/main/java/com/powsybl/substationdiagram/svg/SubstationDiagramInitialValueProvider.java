/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.svg;

import com.powsybl.substationdiagram.model.Node;

/**
 * @author Giovanni Ferrari <giovanni.ferrari at techrain.eu>
 */
public interface SubstationDiagramInitialValueProvider {

    public enum Direction {
        UP, DOWN;
    }

    InitialValue getInitialValue(Node node);

}
