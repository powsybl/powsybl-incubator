/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.interpretation.model.cgmes;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.CgmesNames;
import com.powsybl.cgmes.model.CgmesTerminal;
import com.powsybl.triplestore.api.PropertyBag;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class CgmesLine {
    public CgmesLine(String id, double r, double x, double bch, double gch,
        String nodeId1, boolean connected1, String nodeId2, boolean connected2) {
        this.id = id;
        this.r = r;
        this.x = x;
        this.bch = bch;
        this.gch = gch;
        this.nodeId1 = nodeId1;
        this.connected1 = connected1;
        this.nodeId2 = nodeId2;
        this.connected2 = connected2;
    }

    CgmesLine(String id, double r, double x, double bch, double gch, CgmesTerminal t1, CgmesTerminal t2) {
        this(id, r, x, bch, gch, t1.topologicalNode(), t1.connected(), t2.topologicalNode(), t2.connected());
    }

    CgmesLine(CgmesModel cgmes, PropertyBag lp, String idPropertyName) {
        this(
            lp.getId(idPropertyName),
            lp.asDouble("r"),
            lp.asDouble("x"),
            lp.asDouble("bch", 0.0),
            lp.asDouble("gch", 0.0),
            cgmes.terminal(lp.getId(CgmesNames.TERMINAL + 1)),
            cgmes.terminal(lp.getId(CgmesNames.TERMINAL + 2)));
    }

    public String id() {
        return id;
    }

    public double r() {
        return r;
    }

    public double x() {
        return x;
    }

    public double bch() {
        return bch;
    }

    public double gch() {
        return gch;
    }

    public String nodeId1() {
        return nodeId1;
    }

    public String nodeId2() {
        return nodeId2;
    }

    public boolean connected1() {
        return connected1;
    }

    public boolean connected2() {
        return connected2;
    }

    private final String id;

    private final double r;
    private final double x;
    private final double bch;
    private final double gch;

    private final String nodeId1;
    private final boolean connected1;
    private final String nodeId2;
    private final boolean connected2;
}
