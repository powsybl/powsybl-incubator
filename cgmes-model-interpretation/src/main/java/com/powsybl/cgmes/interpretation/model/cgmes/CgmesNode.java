/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.interpretation.model.cgmes;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.CgmesNames1;
import com.powsybl.triplestore.api.PropertyBag;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class CgmesNode {
    public CgmesNode(String id, double nominalV, double v, double angle, double p, double q) {
        this.id = id;
        this.nominalV = nominalV;
        this.v = v;
        this.angle = angle;
        this.p = p;
        this.q = q;
    }

    CgmesNode(String id, double nominalV, double v, double angle) {
        this(id, nominalV, v, angle, 0, 0);
    }

    CgmesNode(String id, double v, double angle) {
        this(id, Double.NaN, v, angle, 0, 0);
    }

    // Build from TopologicalNode
    CgmesNode(CgmesModel cgmes, PropertyBag np) {
        this(
            np.getId(CgmesNames1.TOPOLOGICAL_NODE),
            cgmes.nominalVoltage(np.getId("BaseVoltage")),
            np.get("v") == null ? 0 : Double.parseDouble(np.get("v")),
            np.get("angle") == null ? 0 : Double.parseDouble(np.get("angle")));
    }

    public String id() {
        return id;
    }

    public double v() {
        return v;
    }

    public double angle() {
        return angle;
    }

    public double nominalV() {
        return nominalV;
    }

    public double p() {
        return p;
    }

    public double q() {
        return q;
    }

    public void addP(double p) {
        this.p += p;
    }

    public void addQ(double q) {
        this.q += q;
    }

    private final String id;
    private final double v;
    private final double angle;
    private final double nominalV;

    private double p;
    private double q;
}
