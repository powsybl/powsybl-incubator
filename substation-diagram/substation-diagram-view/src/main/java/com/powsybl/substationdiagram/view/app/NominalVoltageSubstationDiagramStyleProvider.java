/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.view.app;

import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.substationdiagram.model.Edge;
import com.powsybl.substationdiagram.model.Graph;
import com.powsybl.substationdiagram.model.Node;
import com.powsybl.substationdiagram.svg.SubstationDiagramStyleProvider;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static com.powsybl.substationdiagram.svg.SubstationDiagramStyles.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class NominalVoltageSubstationDiagramStyleProvider implements SubstationDiagramStyleProvider {

    private static String getColor(VoltageLevel vl) {
        if (vl.getNominalV() >= 300) {
            return "rgb(255, 0, 0)";
        } else if (vl.getNominalV() >= 170 && vl.getNominalV() < 300) {
            return "rgb(34, 139, 34)";
        } else if (vl.getNominalV() >= 120 && vl.getNominalV() < 170) {
            return "rgb(1, 175, 175)";
        } else if (vl.getNominalV() >= 70 && vl.getNominalV() < 120) {
            return "rgb(204, 85, 0)";
        } else if (vl.getNominalV() >= 50 && vl.getNominalV() < 70) {
            return "rgb(160, 32, 240)";
        } else if (vl.getNominalV() >= 30 && vl.getNominalV() < 50) {
            return "rgb(255, 130, 144)";
        } else {
            return "rgb(171, 175, 40)";
        }
    }

    @Override
    public Optional<String> getGlobalStyle(Graph graph) {
        String idVL = escapeClassName(graph.getVoltageLevel().getId());
        String color = getColor(graph.getVoltageLevel());
        StringBuilder style = new StringBuilder();
        style.append(".").append(SUBSTATION_STYLE_CLASS).append(" {fill:rgb(255,255,255);stroke-width:1;fill-opacity:0;}");
        style.append(".").append(WIRE_STYLE_CLASS).append("_").append(idVL).append(" {stroke:").append(color).append(";stroke-width:1;}");
        style.append(".").append(GRID_STYLE_CLASS).append(" {stroke:rgb(0,55,0);stroke-width:1;stroke-dasharray:1,10;}");
        style.append(".").append(BUS_STYLE_CLASS).append("_").append(idVL).append(" {stroke:").append(color).append(";stroke-width:3;}");
        style.append(".").append(LABEL_STYLE_CLASS).append(" {fill: black;color:black;stroke:none;fill-opacity:1;}");
        return Optional.of(style.toString());
    }

    @Override
    public Optional<String> getNodeStyle(Node node) {
        String color = getColor(node.getGraph().getVoltageLevel());
        try {
            String className = escapeClassName(URLEncoder.encode(node.getId(), StandardCharsets.UTF_8.name()));
            if (node.getType() == Node.NodeType.SWITCH) {
                String style = "." + className +
                        " .open { visibility: " + (node.isOpen() ? "visible;}" : "hidden;}") +
                        "." + className +
                        " .closed { visibility: " + (node.isOpen() ? "hidden;}" : "visible;}");
                return Optional.of(style);
            } else {
                return Optional.of("." + className + " {stroke:" + color + ";stroke-width:1;fill-opacity:0;}"); // FIXME Doesn't work?
            }
        } catch (UnsupportedEncodingException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Optional<String> getWireStyle(Edge edge) {
        return Optional.empty();
    }
}
