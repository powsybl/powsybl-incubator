/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.autotrol.util;

import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.substationdiagram.model.Edge;
import com.powsybl.substationdiagram.model.Feeder2WTNode;
import com.powsybl.substationdiagram.model.Fictitious3WTNode;
import com.powsybl.substationdiagram.model.Graph;
import com.powsybl.substationdiagram.model.Node;
import static com.powsybl.substationdiagram.svg.SubstationDiagramStyles.BUS_STYLE_CLASS;
import static com.powsybl.substationdiagram.svg.SubstationDiagramStyles.GRID_STYLE_CLASS;
import static com.powsybl.substationdiagram.svg.SubstationDiagramStyles.LABEL_STYLE_CLASS;
import static com.powsybl.substationdiagram.svg.SubstationDiagramStyles.SUBSTATION_STYLE_CLASS;
import static com.powsybl.substationdiagram.svg.SubstationDiagramStyles.WIRE_STYLE_CLASS;
import static com.powsybl.substationdiagram.svg.SubstationDiagramStyles.escapeClassName;
import com.powsybl.substationdiagram.util.NominalVoltageSubstationDiagramStyleProvider;
import java.util.Optional;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 *
 * @author cgalli
 */
public class NominalVoltageSubstationDiagramStyleProvider2 extends NominalVoltageSubstationDiagramStyleProvider
{
    private final BooleanProperty dark  =BooleanProperty.booleanProperty(new SimpleBooleanProperty(true));
    private static final String DEFAULT_COLOR = "rgb(171, 175, 40)";
    
    @Override
    public Optional<String> getColor(VoltageLevel vl)
    {
        String color;
        if (vl.getNominalV() >= 300) {
            color = "rgb(255, 0, 0)";
        } else if (vl.getNominalV() >= 170 && vl.getNominalV() < 300) {
            color = "rgb(34, 139, 34)";
        } else if (vl.getNominalV() >= 120 && vl.getNominalV() < 170) {
            color = "rgb(1, 175, 175)";
        } else if (vl.getNominalV() >= 70 && vl.getNominalV() < 120) {
            color = "rgb(204, 85, 0)";
        } else if (vl.getNominalV() >= 50 && vl.getNominalV() < 70) {
            color = "rgb(160, 32, 240)";
        } else if (vl.getNominalV() >= 30 && vl.getNominalV() < 50) {
            color = "rgb(255, 130, 144)";
        } else {
            color = DEFAULT_COLOR;
        }
        return Optional.of(color);
    }
    
    @Override
    public Optional<String> getGlobalStyle(Graph graph) {
        String idVL = escapeClassName(graph.getVoltageLevel().getId());
        String color = getColor(graph.getVoltageLevel()).orElse(DEFAULT_COLOR);
        StringBuilder style = new StringBuilder();
        style.append(".").append(SUBSTATION_STYLE_CLASS).append(" {fill:rgb(").append(dark.get()?"0,0,0":"255,255,255").append(");stroke-width:1;fill-opacity:0;}");
        style.append(".").append(WIRE_STYLE_CLASS).append("_").append(idVL).append(" {stroke:").append(color).append(";stroke-width:1;}");
        style.append(".").append(GRID_STYLE_CLASS).append(" {stroke:rgb(0,").append(dark.get()?"55":"200").append(",0);stroke-width:1;stroke-dasharray:1,10;}");
        style.append(".").append(BUS_STYLE_CLASS).append("_").append(idVL).append(" {stroke:").append(color).append(";stroke-width:3;}");
        style.append(".").append(LABEL_STYLE_CLASS).append(" {fill: ").append(dark.get()?"white;color:white":"black;color:black").append(";stroke:none;fill-opacity:1;}");
        return Optional.of(style.toString());
    }
    
    public void setDark(boolean dark)
    {
        this.dark.set(dark);
    }
    
    public boolean isDark()
    {
       return this.dark.get();
    }

    @Override
    public Optional<String> getWireStyle(Edge edge) {
        Node node1 = edge.getNode1();
        Node node2 = edge.getNode2();
        if ((node1 instanceof Fictitious3WTNode && node2 instanceof Feeder2WTNode) ||
                (node1 instanceof Feeder2WTNode && node2 instanceof Fictitious3WTNode)) {
            VoltageLevel vl = node1 instanceof Feeder2WTNode ? ((Feeder2WTNode) node1).getVlOtherSide() : ((Feeder2WTNode) node2).getVlOtherSide();
            String idVL = escapeClassName(vl.getId());
            String color = getColor(vl).orElse(DEFAULT_COLOR);
            StringBuilder style = new StringBuilder();
            style.append(".").append(WIRE_STYLE_CLASS).append("_").append(idVL).append(" {stroke:").append(color).append(";stroke-width:1;}");
            return Optional.of(style.toString());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> getNode3WTStyle(Fictitious3WTNode node, ThreeWindingsTransformer.Side side) {
        return getColor(node.getTransformer().getTerminal(side).getVoltageLevel());
    }

    @Override
    public Optional<String> getNode2WTStyle(Feeder2WTNode node, TwoWindingsTransformer.Side side) {
        return getColor(side == TwoWindingsTransformer.Side.ONE ? node.getGraph().getVoltageLevel() : node.getVlOtherSide());
    }
}
