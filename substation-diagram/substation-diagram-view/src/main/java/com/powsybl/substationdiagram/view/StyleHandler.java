/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.view;

import com.powsybl.substationdiagram.library.ComponentType;

/**
 * @author Giovanni Ferrari <giovanni.ferrari at techrain.eu>
 */
public interface StyleHandler {

    String getNodeStyle(String nodeId, ComponentType type);

    String getWireStyle(String wireId, String nodeId1, ComponentType type1, String nodeId2, ComponentType type2);

    Double[] getPowers(String wireId, String nodeId, ComponentType type);

}
