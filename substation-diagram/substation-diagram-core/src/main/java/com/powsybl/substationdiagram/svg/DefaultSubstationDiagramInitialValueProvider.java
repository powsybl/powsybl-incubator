package com.powsybl.substationdiagram.svg;

import java.util.Objects;

import com.powsybl.iidm.network.Branch.Side;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.iidm.network.StaticVarCompensator;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.iidm.network.VscConverterStation;
import com.powsybl.substationdiagram.model.Cell;
import com.powsybl.substationdiagram.model.Node;

public class DefaultSubstationDiagramInitialValueProvider implements SubstationDiagramInitialValueProvider {

    private final Network network;

    public DefaultSubstationDiagramInitialValueProvider(Network net) {
        network = Objects.requireNonNull(net);
    }

    @Override
    public InitialValue getInitialValue(Node node) {
        Double valueP = null;
        Double valueQ = null;
        String nodeId = node.getId();
        switch (node.getComponentType()) {
            case LOAD: {
                Load ld = network.getLoad(nodeId);
                valueP = ld.getP0();
                valueQ = ld.getQ0();
            }
            case LINE: {
                if (nodeId.endsWith(Side.ONE.toString())) {
                    String lineId = nodeId.substring(0, nodeId.length() - 4);
                    Line ln = network.getLine(lineId);
                    valueP = ln.getTerminal1().getP();
                    valueQ = ln.getTerminal1().getQ();
                } else if (nodeId.endsWith(Side.TWO.toString())) {
                    String lineId = nodeId.substring(0, nodeId.length() - 4);
                    Line ln = network.getLine(lineId);
                    valueP = ln.getTerminal2().getP();
                    valueQ = ln.getTerminal2().getQ();
                }
                break;
            }
            case TWO_WINDINGS_TRANSFORMER: {
                TwoWindingsTransformer tw = network
                        .getTwoWindingsTransformer(nodeId.substring(0, nodeId.length() - 4));

                if (nodeId.endsWith(Side.ONE.toString())) {
                    valueP = tw.getTerminal(Side.ONE).getP();
                    valueQ = tw.getTerminal(Side.ONE).getQ();
                } else if (nodeId.endsWith(Side.TWO.toString())) {
                    valueP = tw.getTerminal(Side.TWO).getP();
                    valueQ = tw.getTerminal(Side.TWO).getQ();
                }
                break;
            }
            case INDUCTOR:
            case CAPACITOR: {
                ShuntCompensator sh = network.getShuntCompensator(nodeId);
                valueP = sh.getTerminal().getP();
                valueQ = sh.getTerminal().getQ();
                break;
            }
            case GENERATOR: {
                Generator g = network.getGenerator(nodeId);
                valueP = g.getTerminal().getP();
                valueQ = g.getTerminal().getQ();
                break;
            }
            case STATIC_VAR_COMPENSATOR: {
                StaticVarCompensator svc = network.getStaticVarCompensator(nodeId);
                valueP = svc.getTerminal().getP();
                valueQ = svc.getTerminal().getQ();
                break;
            }
            case VSC_CONVERTER_STATION: {
                VscConverterStation vsc = network.getVscConverterStation(nodeId);
                valueP = vsc.getTerminal().getP();
                valueQ = vsc.getTerminal().getQ();
                break;
            }
            case BUSBAR_SECTION:
            case BREAKER:
            case LOAD_BREAK_SWITCH:
            case DISCONNECTOR:
            default: {
                valueP = null;
                valueQ = null;
            }
        }
        String label1 = null;
        String label2 = null;
        Direction dir1 = null;
        Direction dir2 = null;

        if (valueP != null) {
            label1 = String.valueOf(Math.round(valueP.doubleValue()));
            if (valueP.doubleValue() > 0) {
                if (node.getCell() != null) {
                    dir1 =  node.getCell().getDirection() == Cell.Direction.TOP ? Direction.UP : Direction.DOWN;
                } else {
                    dir1 =  Direction.UP;
                }
            } else {
                if (node.getCell() != null) {
                    dir1 = node.getCell().getDirection() == Cell.Direction.TOP ? Direction.DOWN : Direction.UP;
                } else {
                    dir1 =  Direction.DOWN;
                }
            }
        }
        if (valueQ != null) {
            label2 = String.valueOf(Math.round(valueQ.doubleValue()));
            if (valueQ.doubleValue() > 0) {
                if (node.getCell() != null) {
                    dir2 =  node.getCell().getDirection() == Cell.Direction.TOP ? Direction.UP : Direction.DOWN;
                } else {
                    dir2 =  Direction.UP;
                }
            } else {
                if (node.getCell() != null) {
                    dir2 = node.getCell().getDirection() == Cell.Direction.TOP ? Direction.DOWN : Direction.UP;
                } else {
                    dir2 =  Direction.DOWN;
                }
            }
        }
        return new InitialValue(dir1, dir2, label1, label2);
    }

}
