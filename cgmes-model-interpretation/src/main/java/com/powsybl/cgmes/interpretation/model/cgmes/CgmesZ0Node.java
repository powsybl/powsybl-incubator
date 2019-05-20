/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.interpretation.model.cgmes;

import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableList;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class CgmesZ0Node {

    public CgmesZ0Node(List<String> nodeIds) {
        Objects.requireNonNull(nodeIds);
        this.nodeIds = ImmutableList.copyOf(nodeIds);
    }

    public List<String> nodeIds() {
        return nodeIds;
    }

    @Override
    public int hashCode() {
        return nodeIds.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return nodeIds.equals(o);
    }

    @Override
    public String toString() {
        return nodeIds.toString();
    }

    private final ImmutableList<String> nodeIds;
}
