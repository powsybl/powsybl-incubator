/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.svg;

import java.util.Objects;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Branch.Side;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.substationdiagram.model.Node;

/**
 * @author Giovanni Ferrari <giovanni.ferrari at techrain.eu>
 */
public class DefaultSubstationDiagramInitialValueProvider implements SubstationDiagramInitialValueProvider {

    private final Network network;

    public DefaultSubstationDiagramInitialValueProvider(Network net) {
        network = Objects.requireNonNull(net);
    }

    @Override
    public InitialValue getInitialValue(Node node) {
        Objects.requireNonNull(node);
        InitialValue initialValue = new InitialValue(null, null, null, null, null, null);

        if (node.getType() == Node.NodeType.BUS) {
            initialValue = new InitialValue(null, null, node.getLabel(), null, null, null);
        } else {
            String nodeId = node.getId();
            switch (node.getComponentType()) {
                case LINE:
                case TWO_WINDINGS_TRANSFORMER: {
                    initialValue = getBranchInitialValue(nodeId);
                    break;
                }
                case LOAD: {
                    initialValue = getLoadInitialValue(network.getLoad(nodeId));
                    break;
                }
                case INDUCTOR:
                case CAPACITOR: {
                    initialValue = getInjectionInitialValue(network.getShuntCompensator(nodeId));
                    break;
                }
                case GENERATOR: {
                    initialValue = getInjectionInitialValue(network.getGenerator(nodeId));
                    break;
                }
                case STATIC_VAR_COMPENSATOR: {
                    initialValue = getInjectionInitialValue(network.getStaticVarCompensator(nodeId));
                    break;
                }
                case VSC_CONVERTER_STATION: {
                    initialValue = getInjectionInitialValue(network.getVscConverterStation(nodeId));
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

    private InitialValue getInjectionInitialValue(Injection<?> injection) {
        if (injection != null) {
            return new InitialValue(injection);
        } else {
            return new InitialValue(null, null, null, null, null, null);
        }
    }

    private InitialValue getLoadInitialValue(Load load) {
        if (load != null) {
            return new InitialValue(load);
        } else {
            return new InitialValue(null, null, null, null, null, null);
        }
    }

    private InitialValue getBranchInitialValue(String nodeId) {
        Branch branch = network.getBranch(nodeId.substring(0, nodeId.length() - 4));
        if (branch != null) {
            return new InitialValue(branch, Side.valueOf(nodeId.substring(nodeId.length() - 3)));
        } else {
            return new InitialValue(null, null, null, null, null, null);
        }
    }
}
