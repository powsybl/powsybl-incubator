/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.svg;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Objects;
import java.util.Optional;

import com.powsybl.substationdiagram.library.ComponentType;
import com.powsybl.substationdiagram.model.Edge;
import com.powsybl.substationdiagram.model.Graph;
import com.powsybl.substationdiagram.model.Node;

/**
 * @author Giovanni Ferrari <giovanni.ferrari at techrain.eu>
 */
public class DefaultSubstationDiagramStyleProvider implements SubstationDiagramStyleProvider {

    @Override
    public Optional<String> getGlobalStyle(Graph graph) {
        StringBuffer style = new StringBuffer();
        style.append(".").append(SUBSTATION_STYLE_CLASS).append(" {fill:rgb(255,255,255);stroke-width:1;stroke:rgb(0,0,255);fill-opacity:0;}");
        style.append(".").append(WIRE_STYLE_CLASS).append(" {stroke:rgb(200,0,0);stroke-width:1;}");
        style.append(".").append(GRID_STYLE_CLASS).append(" {stroke:rgb(0,55,0);stroke-width:1;stroke-dasharray:1,10;}");
        style.append(".").append(BUS_STYLE_CLASS).append(" {stroke:rgb(0,0,0);stroke-width:3;}");
        style.append(".").append(LABEL_STYLE_CLASS).append(" {fill: black;color:black;stroke:none;fill-opacity:1;}");
        return Optional.of(style.toString());
    }

    @Override
    public Optional<String> getCompomentStyle(Graph graph, ComponentType componentType) {
        Objects.requireNonNull(componentType);
        StringBuffer style = new StringBuffer();
        style.append(" .").append(componentType);
        switch (componentType) {
            case BREAKER: {
                style.append(" {stroke-width:2;fill-opacity:1;}");
                break;
            }
            case DANGLING_LINE: {
                style.append(" {stroke:rgb(0,0,0);stroke-width:2;}");
                break;
            }
            case DISCONNECTOR: {
                style.append(" {stroke:rgb(0,0,0);stroke-width:3;}");
                break;
            }
            case NODE: {
                style.append(" {stroke:rgb(0,0,0);fill:rgb(0,0,0);fill-opacity:1;}");
                break;
            }
            case PHASE_SHIFT_TRANSFORMER: {
                style.append(" {stroke-linecap:butt;stroke-linejoin:miter;stroke-miterlimit:4;}");
                break;
            }
            case STATIC_VAR_COMPENSATOR: {
                style.append("{}");
                style.append("#path5227 { stroke-width:0.4; stroke-linecap:butt;stroke-linejoin:miter;stroke-miterlimit:1.4;}");
                style.append("#path5229 {stroke-width:0.4;stroke-linecap:butt;stroke-linejoin:miter;stroke-miterlimit:4;}");
                style.append("#path5231 {stroke-width:0.4;stroke-linecap:butt;stroke-linejoin:miter;stroke-miterlimit:4;}");
                break;
            }
            case VSC_CONVERTER_STATION: {
                style.append(" {font-size:7.4314661px;line-height:1.25;font-family:sans-serif;-inkscape-font-specification:'sans-serif, Normal';font-variant-ligatures:normal;font-variant-caps:normal;font-variant-numeric:normal;font-feature-settings:normal;text-align:start;letter-spacing:0px;word-spacing:0px;writing-mode:lr-tb;text-anchor:start;stroke-miterlimit:4;}");
                break;
            }
            default: {
                style.append("{}");
                break;
            }
        }
        return Optional.of(style.toString());
    }

    @Override
    public Optional<String> getNodeStyle(Node node) {
        Objects.requireNonNull(node);
        if (node.getType().equals(Node.NodeType.SWITCH)) {
            try {
                StringBuffer style = new StringBuffer();
                style.append(".").append(escapeClassName(URLEncoder.encode(node.getId(), "UTF-8"))).append(" .open { visibility: ").append(node.isOpen() ? "visible;" : "hidden;}");

                style.append(".").append(escapeClassName(URLEncoder.encode(node.getId(), "UTF-8"))).append(" .closed { visibility: ").append(node.isOpen() ? "hidden;" : "visible;}");

                return Optional.of(style.toString());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> getWireStyle(Edge edge) {
        return  Optional.empty();
    }

    public static String escapeClassName(String input) {
        return Objects.requireNonNull(input).replaceAll("\\+", "\\\\+").replaceAll("\\.", "\\\\.");
    }

}
