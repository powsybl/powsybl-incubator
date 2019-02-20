/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.view;

import java.util.Objects;

import com.powsybl.iidm.network.Branch.Side;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.iidm.network.StaticVarCompensator;
import com.powsybl.iidm.network.Switch;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.iidm.network.VscConverterStation;
import com.powsybl.substationdiagram.library.ComponentType;

/**
 * @author Giovanni Ferrari <giovanni.ferrari at techrain.eu>
 */
public class NetworkStyleHandler implements StyleHandler {

    private final Network network;

    public NetworkStyleHandler(Network network) {
        Objects.requireNonNull(network);
        this.network = network;
    }

    @Override
    public String getWireStyle(String wireId, String nodeId1, ComponentType type1, String nodeId2,
            ComponentType type2) {
        String style = "";
        String color = "blue";
        network.getBusView().getBuses();
        String id = nodeId1;
        ComponentType type = type1;
        if ((type1.equals(ComponentType.BREAKER) || type1.equals(ComponentType.LOAD_BREAK_SWITCH)
                || type1.equals(ComponentType.DISCONNECTOR)) && (type2.equals(ComponentType.LINE) || type2.equals(ComponentType.TWO_WINDINGS_TRANSFORMER))) {
            Switch sw = (Switch) network.getIdentifiable(nodeId1);
            boolean swOpen = sw != null && sw.isOpen() ? true : false;

            if (type2.equals(ComponentType.TWO_WINDINGS_TRANSFORMER)) {
                TwoWindingsTransformer tw = this.network
                        .getTwoWindingsTransformer(nodeId2.substring(0, nodeId2.length() - 4));
                if (nodeId2.endsWith(Side.ONE.toString())) {
                    if (!tw.getTerminal(Side.TWO).isConnected() && swOpen) {
                        style = "";
                        color = "black";
                    } else if (!tw.getTerminal(Side.TWO).isConnected() || swOpen) {
                        style = "-fx-stroke-dash-array: 10;";
                        color = swOpen ? "black" : "blue";
                    }
                } else if (nodeId2.endsWith(Side.TWO.toString())) {
                    if (!tw.getTerminal(Side.ONE).isConnected() && swOpen) {
                        style = "";
                        color = "black";
                    } else if (!tw.getTerminal(Side.ONE).isConnected() || swOpen) {
                        style = "-fx-stroke-dash-array: 10;";
                        color = swOpen ? "black" : "blue";
                    }
                }
            } else if (type2.equals(ComponentType.LINE)) {
                Line ln = this.network
                        .getLine(nodeId2.substring(0, nodeId2.length() - 4));
                if (nodeId2.endsWith(Side.ONE.toString())) {

                    if (!ln.getTerminal(Side.TWO).isConnected() && swOpen) {
                        style = "";
                        color = "black";
                    } else if (!ln.getTerminal(Side.TWO).isConnected() || swOpen) {
                        style = "-fx-stroke-dash-array: 10;";
                        color = swOpen ? "black" : "blue";
                    }
                } else if (nodeId2.endsWith(Side.TWO.toString())) {
                    if (!ln.getTerminal(Side.ONE).isConnected() && swOpen) {
                        style = "";
                        color = "black";
                    } else if (!ln.getTerminal(Side.ONE).isConnected() || swOpen) {
                        style = "-fx-stroke-dash-array: 10;";
                        color = swOpen ? "black" : "blue";
                    }
                }
            }

            type = type2;
            id = nodeId2;
        } else if ((type2.equals(ComponentType.BREAKER) || type2.equals(ComponentType.LOAD_BREAK_SWITCH)
                || type2.equals(ComponentType.DISCONNECTOR)) && (type1.equals(ComponentType.LINE) || type1.equals(ComponentType.TWO_WINDINGS_TRANSFORMER))) {
            Switch sw = (Switch) this.network.getIdentifiable(nodeId2);
            boolean swOpen = sw != null && sw.isOpen() ? true : false;

            if (type1.equals(ComponentType.TWO_WINDINGS_TRANSFORMER)) {
                TwoWindingsTransformer tw = this.network
                        .getTwoWindingsTransformer(nodeId1.substring(0, nodeId1.length() - 4));
                if (nodeId1.endsWith(Side.ONE.toString())) {

                    if (!tw.getTerminal(Side.TWO).isConnected() && swOpen) {
                        style = "";
                        color = "black";
                    } else if (!tw.getTerminal(Side.TWO).isConnected() || swOpen) {
                        style = "-fx-stroke-dash-array: 10;";
                        color = swOpen ? "black" : "blue";
                    }
                } else if (nodeId1.endsWith(Side.TWO.toString())) {
                    if (!tw.getTerminal(Side.ONE).isConnected() && swOpen) {
                        style = "";
                        color = "black";
                    } else if (!tw.getTerminal(Side.ONE).isConnected() || swOpen) {
                        style = "-fx-stroke-dash-array: 10;";
                        color = swOpen ? "black" : "blue";
                    }
                }
            } else if (type1.equals(ComponentType.LINE)) {
                Line ln = this.network
                        .getLine(nodeId1.substring(0, nodeId1.length() - 4));
                if (nodeId1.endsWith(Side.ONE.toString())) {

                    if (!ln.getTerminal(Side.TWO).isConnected() && swOpen) {
                        style = "";
                        color = "black";
                    } else if (!ln.getTerminal(Side.TWO).isConnected() || swOpen) {
                        style = "-fx-stroke-dash-array: 10;";
                        color = swOpen ? "black" : "blue";
                    }
                } else if (nodeId1.endsWith(Side.TWO.toString())) {
                    if (!ln.getTerminal(Side.ONE).isConnected() && swOpen) {
                        style = "";
                        color = "black";
                    } else if (!ln.getTerminal(Side.ONE).isConnected() || swOpen) {
                        style = "-fx-stroke-dash-array: 10;";
                        color = swOpen ? "black" : "blue";
                    }
                }
            }
        }
        return style + " -fx-stroke: " + color;
    }

    @Override
    public String getNodeStyle(String nodeId, ComponentType type) {
        String style = "";
        String color = "blue";
        switch (type) {
            case LOAD: {
                break;
            }
            case LINE: {
                break;
            }
            case BREAKER:
            case LOAD_BREAK_SWITCH:
            case DISCONNECTOR: {
                Switch sw = (Switch) this.network.getIdentifiable(nodeId);
                if (sw != null  && sw.isFictitious()) {
                    color = "red";
                }
                break;
            }
            default: {
                color = "black";
            }
        }
        return style + " -fx-stroke: " + color;
    }

    @Override
    public Double[] getPowers(String wireId, String nodeId, ComponentType type) {
        Double[] labels = new Double[2];
        switch (type) {
            case LOAD: {
                Load ld = this.network.getLoad(nodeId);
                labels[0] = ld.getP0();
                labels[1] = ld.getQ0();
            }
            case LINE: {
                if (nodeId.endsWith(Side.ONE.toString())) {
                    String lineId = nodeId.substring(0, nodeId.length() - 4);
                    Line ln = this.network.getLine(lineId);
                    labels[0] = ln.getTerminal1().getP();
                    labels[1] = ln.getTerminal1().getQ();
                } else if (nodeId.endsWith(Side.TWO.toString())) {
                    String lineId = nodeId.substring(0, nodeId.length() - 4);
                    Line ln = this.network.getLine(lineId);
                    labels[0] = ln.getTerminal2().getP();
                    labels[1] = ln.getTerminal2().getQ();
                }
                break;
            }
            case TWO_WINDINGS_TRANSFORMER: {
                TwoWindingsTransformer tw = this.network
                        .getTwoWindingsTransformer(nodeId.substring(0, nodeId.length() - 4));

                if (nodeId.endsWith(Side.ONE.toString())) {
                    labels[0] = tw.getTerminal(Side.ONE).getP();
                    labels[1] = tw.getTerminal(Side.ONE).getQ();
                } else if (nodeId.endsWith(Side.TWO.toString())) {
                    labels[0] = tw.getTerminal(Side.TWO).getP();
                    labels[1] = tw.getTerminal(Side.TWO).getQ();
                }
                break;
            }
            case INDUCTOR:
            case CAPACITOR: {
                ShuntCompensator sh = this.network.getShuntCompensator(nodeId);
                labels[0] = sh.getTerminal().getP();
                labels[1] = sh.getTerminal().getQ();
                break;
            }
            case GENERATOR: {
                Generator g = this.network.getGenerator(nodeId);
                labels[0] = g.getTerminal().getP();
                labels[1] = g.getTerminal().getQ();
                break;
            }
            case STATIC_VAR_COMPENSATOR: {
                StaticVarCompensator svc = this.network.getStaticVarCompensator(nodeId);
                labels[0] = svc.getTerminal().getP();
                labels[1] = svc.getTerminal().getQ();
                break;
            }
            case VSC_CONVERTER_STATION: {
                VscConverterStation vsc = this.network.getVscConverterStation(nodeId);
                labels[0] = vsc.getTerminal().getP();
                labels[1] = vsc.getTerminal().getQ();
                break;
            }
            case BREAKER:
            case LOAD_BREAK_SWITCH:
            case DISCONNECTOR:
            default: {
                labels[0] = null;
                labels[1] = null;
            }
        }
        return labels;
    }

}
