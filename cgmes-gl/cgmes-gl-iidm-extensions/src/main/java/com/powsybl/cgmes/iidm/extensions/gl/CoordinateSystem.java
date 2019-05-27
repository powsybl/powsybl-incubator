/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.iidm.extensions.gl;

import java.util.Objects;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class CoordinateSystem {

    private final String name;
    private final String urn;

    public CoordinateSystem(String name, String urn) {
        this.name = Objects.requireNonNull(name);
        this.urn = Objects.requireNonNull(urn);
    }

    public String getName() {
        return name;
    }

    public String getUrn() {
        return urn;
    }

    @Override
    public String toString() {
        return "[" + name + "," + urn + "]";
    }

}
