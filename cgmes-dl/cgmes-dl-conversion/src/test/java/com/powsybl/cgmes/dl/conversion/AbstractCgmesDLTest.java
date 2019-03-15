/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.dl.conversion;

import java.util.Arrays;

import org.junit.Before;

import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public abstract class AbstractCgmesDLTest {

    protected static String NAMESPACE = "http://network#";

    protected PropertyBags terminalsPropertyBags;
    protected PropertyBags busesPropertyBags;
    protected PropertyBags busbarsPropertyBags;
    protected PropertyBags linesPropertyBags;
    protected PropertyBags danglingLinesPropertyBags;
    protected PropertyBags generatorsPropertyBags;
    protected PropertyBags loadsPropertyBags;
    protected PropertyBags shuntsPropertyBags;
    protected PropertyBags switchesPropertyBags;
    protected PropertyBags tranformersPropertyBags;
    protected PropertyBags tranformers3wPropertyBags;
    protected PropertyBags hvdcLinesPropertyBags;
    protected PropertyBags svcsPropertyBags;

    @Before
    public void setUp() {
        terminalsPropertyBags = new PropertyBags(Arrays.asList(createTerminalPropertyBag(NAMESPACE + "Generator", "1", 2, 10, 1),
                                                               createTerminalPropertyBag(NAMESPACE + "Generator", "1", 6, 10, 2),
                                                               createTerminalPropertyBag(NAMESPACE + "Load", "1", 2, 10, 1),
                                                               createTerminalPropertyBag(NAMESPACE + "Load", "1", 6, 10, 2),
                                                               createTerminalPropertyBag(NAMESPACE + "Shunt", "1", 2, 10, 1),
                                                               createTerminalPropertyBag(NAMESPACE + "Shunt", "1", 6, 10, 2),
                                                               createTerminalPropertyBag(NAMESPACE + "Switch", "1", 2, 10, 1),
                                                               createTerminalPropertyBag(NAMESPACE + "Switch", "1", 6, 10, 2),
                                                               createTerminalPropertyBag(NAMESPACE + "Switch", "2", 14, 10, 1),
                                                               createTerminalPropertyBag(NAMESPACE + "Switch", "2", 18, 10, 2),
                                                               createTerminalPropertyBag(NAMESPACE + "Transformer", "1", 2, 10, 1),
                                                               createTerminalPropertyBag(NAMESPACE + "Transformer", "1", 6, 10, 2),
                                                               createTerminalPropertyBag(NAMESPACE + "Transformer", "2", 14, 10, 1),
                                                               createTerminalPropertyBag(NAMESPACE + "Transformer", "2", 18, 10, 2),
                                                               createTerminalPropertyBag(NAMESPACE + "Transformer3w", "1", 2, 10, 1),
                                                               createTerminalPropertyBag(NAMESPACE + "Transformer3w", "1", 6, 10, 2),
                                                               createTerminalPropertyBag(NAMESPACE + "Transformer3w", "2", 14, 10, 1),
                                                               createTerminalPropertyBag(NAMESPACE + "Transformer3w", "2", 18, 10, 2),
                                                               createTerminalPropertyBag(NAMESPACE + "Transformer3w", "3", 10, 16, 1),
                                                               createTerminalPropertyBag(NAMESPACE + "Transformer3w", "3", 10, 20, 2),
                                                               createTerminalPropertyBag(NAMESPACE + "Svc", "1", 2, 10, 1),
                                                               createTerminalPropertyBag(NAMESPACE + "Svc", "1", 6, 10, 2)));
        busesPropertyBags = new PropertyBags(Arrays.asList(createBusPropertyBag(NAMESPACE + "Bus", "Bus", NAMESPACE + "VoltageLevel", "VoltageLevel", 20, 5, 1),
                                                           createBusPropertyBag(NAMESPACE + "Bus", "Bus", NAMESPACE + "VoltageLevel", "VoltageLevel", 20, 40, 2)));
        busbarsPropertyBags = new PropertyBags(Arrays.asList(createBusbarPropertyBag(NAMESPACE + "Busbar", "Busbar", 20, 5, 1),
                                                             createBusbarPropertyBag(NAMESPACE + "Busbar", "Busbar", 20, 40, 2)));
        linesPropertyBags = new PropertyBags(Arrays.asList(createPropertyBag(NAMESPACE + "Line", "Line", 20, 5, 1),
                                                           createPropertyBag(NAMESPACE + "Line", "Line", 20, 40, 2)));
        danglingLinesPropertyBags = new PropertyBags(Arrays.asList(createPropertyBag(NAMESPACE + "DanglingLine", "DanglingLine", 20, 5, 1),
                                                                   createPropertyBag(NAMESPACE + "DanglingLine", "DanglingLine", 20, 40, 2)));
        generatorsPropertyBags = new PropertyBags(Arrays.asList(createPropertyBag(NAMESPACE + "Generator", "Generator", 10, 10, 0, 90)));
        loadsPropertyBags = new PropertyBags(Arrays.asList(createPropertyBag(NAMESPACE + "Load", "Load", 10, 10, 0, 90)));
        shuntsPropertyBags = new PropertyBags(Arrays.asList(createPropertyBag(NAMESPACE + "Shunt", "Shunt", 10, 10, 0, 90)));
        switchesPropertyBags = new PropertyBags(Arrays.asList(createSwitchPropertyBag(NAMESPACE + "Switch", "Switch", 10, 10, 90)));
        tranformersPropertyBags = new PropertyBags(Arrays.asList(createPropertyBag(NAMESPACE + "Transformer", "Transformer", 10, 10, 0, 90)));
        tranformers3wPropertyBags = new PropertyBags(Arrays.asList(createPropertyBag(NAMESPACE + "Transformer3w", "Transformer3w", 10, 13, 0, 90)));
        hvdcLinesPropertyBags = new PropertyBags(Arrays.asList(createPropertyBag(NAMESPACE + "HvdcLine", "HvdcLine", 20, 5, 1),
                                                               createPropertyBag(NAMESPACE + "HvdcLine", "HvdcLine", 20, 40, 2)));
        svcsPropertyBags = new PropertyBags(Arrays.asList(createPropertyBag(NAMESPACE + "Svc", "Svc", 10, 10, 0, 90)));
    }

    protected PropertyBag createPropertyBag(String identifiedObject, String name, double x, double y, int seq) {
        PropertyBag propertyBag = new PropertyBag(Arrays.asList("identifiedObject", "name", "x", "y", "seq"));
        propertyBag.put("identifiedObject", identifiedObject);
        propertyBag.put("name", name);
        propertyBag.put("x", Double.toString(x));
        propertyBag.put("y", Double.toString(y));
        propertyBag.put("seq", Integer.toString(seq));
        return propertyBag;
    }

    protected PropertyBag createPropertyBag(String identifiedObject, String name, double x, double y, int seq, int rotation) {
        PropertyBag propertyBag = new PropertyBag(Arrays.asList("identifiedObject", "name", "x", "y", "seq", "rotation"));
        propertyBag.put("identifiedObject", identifiedObject);
        propertyBag.put("name", name);
        propertyBag.put("x", Double.toString(x));
        propertyBag.put("y", Double.toString(y));
        propertyBag.put("seq", Integer.toString(seq));
        propertyBag.put("rotation", Integer.toString(rotation));
        return propertyBag;
    }

    protected PropertyBag createTerminalPropertyBag(String terminalEquipment, String terminalSide, double x, double y, int seq) {
        PropertyBag propertyBag = new PropertyBag(Arrays.asList("terminalEquipment", "terminalSide", "x", "y", "seq"));
        propertyBag.put("terminalEquipment", terminalEquipment);
        propertyBag.put("terminalSide", terminalSide);
        propertyBag.put("x", Double.toString(x));
        propertyBag.put("y", Double.toString(y));
        propertyBag.put("seq", Integer.toString(seq));
        return propertyBag;
    }

    protected PropertyBag createBusPropertyBag(String identifiedObject, String name, String voltageLevel, String vlName, double x, double y, int seq) {
        PropertyBag propertyBag = new PropertyBag(Arrays.asList("identifiedObject", "name", "voltageLevel", "vlname", "x", "y", "seq"));
        propertyBag.put("identifiedObject", identifiedObject);
        propertyBag.put("name", name);
        propertyBag.put("voltageLevel", voltageLevel);
        propertyBag.put("vlname", vlName);
        propertyBag.put("x", Double.toString(x));
        propertyBag.put("y", Double.toString(y));
        propertyBag.put("seq", Integer.toString(seq));
        return propertyBag;
    }

    protected PropertyBag createBusbarPropertyBag(String identifiedObject, String name, double x, double y, int seq) {
        PropertyBag propertyBag = new PropertyBag(Arrays.asList("identifiedObject", "name", "x", "y", "seq"));
        propertyBag.put("busbarSection", identifiedObject);
        propertyBag.put("name", name);
        propertyBag.put("x", Double.toString(x));
        propertyBag.put("y", Double.toString(y));
        propertyBag.put("seq", Integer.toString(seq));
        return propertyBag;
    }

    protected PropertyBag createSwitchPropertyBag(String identifiedObject, String name, double x, double y, int rotation) {
        PropertyBag propertyBag = new PropertyBag(Arrays.asList("identifiedObject", "name", "x", "y", "rotation"));
        propertyBag.put("identifiedObject", identifiedObject);
        propertyBag.put("name", name);
        propertyBag.put("x", Double.toString(x));
        propertyBag.put("y", Double.toString(y));
        propertyBag.put("rotation", Integer.toString(rotation));
        return propertyBag;
    }

}
