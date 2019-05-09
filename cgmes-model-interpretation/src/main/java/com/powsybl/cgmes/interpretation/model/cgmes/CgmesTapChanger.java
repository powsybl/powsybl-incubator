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
import com.powsybl.triplestore.api.PropertyBags;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class CgmesTapChanger {
    protected CgmesTapChanger() {
        neutralU = 0;
        lowStep = 0;
        highStep = 0;
        neutralStep = 0;
        step = 0;
        regulatingControlEnabled = false;
        tableId = null;
        table = null;
        tabularDifferentRatios = false;
        tabularDifferentAngles = false;
    }

    CgmesTapChanger(PropertyBag tcp, String tableIdPropertyName, CgmesModel cgmes) {
        this.neutralU = tcp.asDouble("neutralU");
        this.lowStep = tcp.asInt("lowStep");
        this.highStep = tcp.asInt("highStep");
        this.neutralStep = tcp.asInt("neutralStep");
        // Consider finding the closest step to given value for step
        // and keep the original value but add an int attribute "stepk" or "stepi"?
        this.step = tcp.asDouble("SVtapStep");
        this.regulatingControlEnabled = tcp.asBoolean("regulatingControlEnabled", false);
        this.tableId = tcp.getId(tableIdPropertyName);
        // TODO Check if we can access "all tables"
        // They are available in CgmesModelForInterpretation
        // Maybe receive CgmesModelForInterpretation instead of only the original CGMES
        PropertyBags table0 = null;
        if (tableId != null) {
            if (tableIdPropertyName.startsWith("Ratio")) {
                table0 = cgmes.ratioTapChangerTable(tableId);
            } else if (tableIdPropertyName.startsWith("Phase")) {
                table0 = cgmes.phaseTapChangerTable(tableId);
            }
        }
        this.table = table0;
        this.tabularDifferentRatios = table != null
                && table.stream()
                        .map(pb -> pb.asDouble(CgmesNames1.RATIO))
                        .mapToDouble(Double::doubleValue)
                        .distinct()
                        .limit(2)
                        .count() > 1;
        this.tabularDifferentAngles = table != null
                && table.stream()
                        .map(pb -> pb.asDouble("angle"))
                        .mapToDouble(Double::doubleValue)
                        .distinct()
                        .limit(2)
                        .count() > 1;
    }

    public boolean isRegulatingControlEnabled() {
        return regulatingControlEnabled;
    }

    public PropertyBag point(double step) {
        PropertyBag closestPoint = null;
        if (table != null && !table.isEmpty()) {
            // Choose the closest step to the given floating point value for step
            double dmin = Double.MAX_VALUE;
            for (PropertyBag point : table) {
                double d = Math.abs(point.asInt("step") - step);
                if (d < dmin) {
                    dmin = d;
                    closestPoint = point;
                }
            }
        }
        return closestPoint;
    }

    public static double getCorrectionFactor(double xc) {
        return 1.0 + xc / 100.0;
    }

    static double pointValue(PropertyBag point, String parameter, double defaultValue) {
        double value = point.asDouble(parameter, defaultValue);
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return defaultValue;
        }
        return value;
    }

    final String tableId;
    final PropertyBags table;
    final double neutralU;
    final int lowStep;
    final int highStep;
    final int neutralStep;
    final double step;

    final boolean regulatingControlEnabled;
    final boolean tabularDifferentRatios;
    final boolean tabularDifferentAngles;
}
