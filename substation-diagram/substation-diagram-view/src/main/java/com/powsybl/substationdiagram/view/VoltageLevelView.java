/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.view;

import com.powsybl.iidm.network.VoltageLevel;
import javafx.scene.layout.Pane;

/**
 * @author Giovanni Ferrari <giovanni.ferrari at techrain.eu>
 */
public interface VoltageLevelView {

    public Pane view(VoltageLevel vl, NavigationListener navigationListener, StyleHandler styleHandler);

}
