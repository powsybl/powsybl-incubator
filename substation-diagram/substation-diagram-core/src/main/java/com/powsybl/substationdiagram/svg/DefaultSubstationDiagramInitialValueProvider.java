package com.powsybl.substationdiagram.svg;

import java.util.Objects;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Branch.Side;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.iidm.network.StaticVarCompensator;
import com.powsybl.iidm.network.VscConverterStation;
import com.powsybl.substationdiagram.model.Node;

public class DefaultSubstationDiagramInitialValueProvider implements SubstationDiagramInitialValueProvider {

    private final Network network;

    public DefaultSubstationDiagramInitialValueProvider(Network net) {
        network = Objects.requireNonNull(net);
    }

    @Override
    public InitialValue getInitialValue(Node node) {
        InitialValue initialValue = new InitialValue(null, null, null, null, null, null);

        if (node.getType() == Node.NodeType.BUS) {
            initialValue = new InitialValue(null, null, node.getLabel(), null, null, null);
        } else {
            String nodeId = node.getId();
            switch (node.getComponentType()) {
                case LINE:
                case TWO_WINDINGS_TRANSFORMER: {
                    Branch branch = network.getBranch(nodeId.substring(0, nodeId.length() - 4));
                    if (branch != null) {
                        initialValue = new InitialValue(branch, Side.valueOf(nodeId.substring(nodeId.length() - 3)));
                    }
                    break;
                }
                case LOAD: {
                    Load ld = network.getLoad(nodeId);
                    if (ld != null) {
                        initialValue = new InitialValue(ld);
                    }
                }
                case INDUCTOR:
                case CAPACITOR: {
                    ShuntCompensator sh = network.getShuntCompensator(nodeId);
                    if (sh != null)  {
                        initialValue = new InitialValue(sh);
                    }
                    break;
                }
                case GENERATOR: {
                    Generator g = network.getGenerator(nodeId);
                    if (g != null) {
                        initialValue = new InitialValue(g);
                    }
                    break;
                }
                case STATIC_VAR_COMPENSATOR: {
                    StaticVarCompensator svc = network.getStaticVarCompensator(nodeId);
                    if (svc != null) {
                        initialValue = new InitialValue(svc);
                    }
                    break;
                }
                case VSC_CONVERTER_STATION: {
                    VscConverterStation vsc = network.getVscConverterStation(nodeId);
                    if (vsc != null) {
                        initialValue = new InitialValue(vsc);
                    }
                    break;
                }
                case BUSBAR_SECTION:
                case BREAKER:
                case LOAD_BREAK_SWITCH:
                case DISCONNECTOR:
                default: {
                    break;
                }
            }
        }
        return initialValue;
    }

}
