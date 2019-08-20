/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.svg;

import static com.powsybl.substationdiagram.svg.SubstationDiagramStyles.BUS_STYLE_CLASS;
import static com.powsybl.substationdiagram.svg.SubstationDiagramStyles.GRID_STYLE_CLASS;
import static com.powsybl.substationdiagram.svg.SubstationDiagramStyles.LABEL_STYLE_CLASS;
import static com.powsybl.substationdiagram.svg.SubstationDiagramStyles.SUBSTATION_STYLE_CLASS;
import static com.powsybl.substationdiagram.svg.SubstationDiagramStyles.WIRE_STYLE_CLASS;
import static com.powsybl.substationdiagram.svg.SubstationDiagramStyles.escapeClassName;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.substationdiagram.model.Edge;
import com.powsybl.substationdiagram.model.Feeder2WTNode;
import com.powsybl.substationdiagram.model.FeederNode;
import com.powsybl.substationdiagram.model.Fictitious3WTNode;
import com.powsybl.substationdiagram.model.Graph;
import com.powsybl.substationdiagram.model.Node;

/**
 * @author Giovanni Ferrari <giovanni.ferrari at techrain.eu>
 */
public class DefaultSubstationDiagramStyleProvider implements SubstationDiagramStyleProvider {

    private static final String ARROW1 = ".ARROW1_";
    private static final String ARROW2 = ".ARROW2_";
    private static final String UP = "_UP";
    private static final String DOWN = "_DOWN";

    @Override
    public Optional<String> getGlobalStyle(Graph graph) {
        String idVL = escapeClassName(graph.getVoltageLevel().getId());
        StringBuilder style = new StringBuilder();
        style.append(".").append(SUBSTATION_STYLE_CLASS).append(" {fill:rgb(255,255,255);stroke-width:1;fill-opacity:0;}");
        style.append(".").append(WIRE_STYLE_CLASS).append("_").append(idVL).append(" {stroke:rgb(200,0,0);stroke-width:1;}");
        style.append(".").append(GRID_STYLE_CLASS).append(" {stroke:rgb(0,55,0);stroke-width:1;stroke-dasharray:1,10;}");
        style.append(".").append(BUS_STYLE_CLASS).append("_").append(idVL).append(" {stroke:rgb(0,0,0);stroke-width:3;}");
        style.append(".").append(LABEL_STYLE_CLASS).append(" {fill:black;color:black;stroke:none;fill-opacity:1;}");
        return Optional.of(style.toString());
    }

    @Override
    public Optional<String> getNodeStyle(Node node) {
        Objects.requireNonNull(node);
        if (node.getType() == Node.NodeType.SWITCH) {
            try {
                StringBuilder style = new StringBuilder();
                String className = escapeClassName(URLEncoder.encode(node.getId(), StandardCharsets.UTF_8.name()));
                style.append(".").append(className)
                        .append(" .open { visibility: ").append(node.isOpen() ? "visible;}" : "hidden;}");

                style.append(".").append(className)
                        .append(" .closed { visibility: ").append(node.isOpen() ? "hidden;}" : "visible;}");

                return Optional.of(style.toString());
            } catch (UnsupportedEncodingException e) {
                throw new UncheckedIOException(e);
            }
        }
        if (node instanceof FeederNode) {
            try {
                StringBuilder style = new StringBuilder();
                style.append(ARROW1).append(escapeClassName(URLEncoder.encode(node.getId(), StandardCharsets.UTF_8.name())))
                        .append("_UP").append(" .arrow-up {stroke: black; fill: black; fill-opacity:1; visibility: visible;}");
                style.append(ARROW1).append(escapeClassName(URLEncoder.encode(node.getId(), StandardCharsets.UTF_8.name())))
                .append(UP).append(" .arrow-down { visibility: hidden;}");

                style.append(ARROW1).append(escapeClassName(URLEncoder.encode(node.getId(), StandardCharsets.UTF_8.name())))
                .append(DOWN).append(" .arrow-down {stroke: black; fill: black; fill-opacity:1;  visibility: visible;}");
                style.append(ARROW1).append(escapeClassName(URLEncoder.encode(node.getId(), StandardCharsets.UTF_8.name())))
                .append(DOWN).append(" .arrow-up { visibility: hidden;}");

                style.append(ARROW2).append(escapeClassName(URLEncoder.encode(node.getId(), StandardCharsets.UTF_8.name())))
                .append(UP).append(" .arrow-up {stroke: blue; fill: blue; fill-opacity:1; visibility: visible;}");
                style.append(ARROW2).append(escapeClassName(URLEncoder.encode(node.getId(), StandardCharsets.UTF_8.name())))
                .append(UP).append(" .arrow-down { visibility: hidden;}");

                style.append(ARROW2).append(escapeClassName(URLEncoder.encode(node.getId(), StandardCharsets.UTF_8.name())))
                .append(DOWN).append(" .arrow-down {stroke: blue; fill: blue; fill-opacity:1;  visibility: visible;}");
                style.append(ARROW2).append(escapeClassName(URLEncoder.encode(node.getId(), StandardCharsets.UTF_8.name())))
                .append(DOWN).append(" .arrow-up { visibility: hidden;}");

                return Optional.of(style.toString());
            } catch (UnsupportedEncodingException e) {
                throw new UncheckedIOException(e);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> getWireStyle(Edge edge) {
        return  Optional.empty();
    }

    @Override
    public Optional<String> getNode3WTStyle(Fictitious3WTNode node, ThreeWindingsTransformer.Side side) {
        return Optional.empty();
    }

    @Override
    public Optional<String> getNode2WTStyle(Feeder2WTNode node, TwoWindingsTransformer.Side side) {
        return Optional.empty();
    }
}
