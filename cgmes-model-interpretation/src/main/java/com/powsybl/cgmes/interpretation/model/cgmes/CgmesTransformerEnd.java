/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.interpretation.model.cgmes;

import java.util.Map;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.CgmesNames;
import com.powsybl.cgmes.model.CgmesTerminal;
import com.powsybl.triplestore.api.PropertyBag;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class CgmesTransformerEnd {
    CgmesTransformerEnd(
        CgmesModel cgmes,
        PropertyBag endp,
        Map<String, PropertyBag> allTapChangers,
        boolean discreteStep) {
        this.endNumber = endp.asInt("endNumber");
        this.r = endp.asDouble("r", 0);
        this.x = endp.asDouble("x", 0);
        this.g = endp.asDouble("g", 0);
        this.b = endp.asDouble("b", 0);
        this.phaseAngleClock = endp.asInt("phaseAngleClock", 0);
        this.ratedU = endp.asDouble("ratedU");
        PropertyBag rtcp = allTapChangers.get(endp.getId("RatioTapChanger"));
        this.rtc = rtcp != null ? new CgmesRatioTapChanger(rtcp, cgmes, discreteStep) : CgmesRatioTapChanger.EMPTY;
        PropertyBag ptcp = allTapChangers.get(endp.getId("PhaseTapChanger"));
        this.ptc = ptcp != null ? new CgmesPhaseTapChanger(ptcp, cgmes, discreteStep) : CgmesPhaseTapChanger.EMPTY;
        CgmesTerminal t = cgmes.terminal(endp.getId(CgmesNames.TERMINAL));
        this.nodeId = t.topologicalNode();
        this.connected = t.connected();
    }

    public CgmesTransformerEnd(
        int endNumber,
        double ratedU,
        double r, double x,
        double b, double g,
        int phaseAngleClock,
        String nodeId,
        boolean connected,
        CgmesRatioTapChanger rtc,
        CgmesPhaseTapChanger ptc) {
        this.endNumber = endNumber;
        this.ratedU = ratedU;
        this.r = r;
        this.x = x;
        this.b = b;
        this.g = g;
        this.phaseAngleClock = phaseAngleClock;
        this.nodeId = nodeId;
        this.connected = connected;
        this.rtc = rtc;
        this.ptc = ptc;
    }

    public int endNumber() {
        return endNumber;
    }

    public double ratedU() {
        return ratedU;
    }

    public double r() {
        return r;
    }

    public double x() {
        return x;
    }

    public double b() {
        return b;
    }

    public double g() {
        return g;
    }

    public int phaseAngleClock() {
        return phaseAngleClock;
    }

    public String nodeId() {
        return nodeId;
    }

    public boolean connected() {
        return connected;
    }

    public CgmesRatioTapChanger rtc() {
        return rtc;
    }

    public CgmesPhaseTapChanger ptc() {
        return ptc;
    }

    public double phaseAngleClockDegrees() {
        double angle = phaseAngleClock * 30.0;
        angle = Math.IEEEremainder(angle, 360.0);
        if (angle > 180.0) {
            angle -= 360.0;
        }
        return angle;
    }

    private final int endNumber;

    private final double ratedU;
    private final double r;
    private final double x;
    private final double g;
    private final double b;
    private final int phaseAngleClock;

    private final String nodeId;
    private final boolean connected;

    private final CgmesRatioTapChanger rtc;
    private final CgmesPhaseTapChanger ptc;
}
