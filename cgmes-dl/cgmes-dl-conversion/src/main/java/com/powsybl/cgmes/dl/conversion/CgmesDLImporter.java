/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.dl.conversion;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.dl.conversion.importers.BusDiagramDataImporter;
import com.powsybl.cgmes.dl.conversion.importers.BusbarDiagramDataImporter;
import com.powsybl.cgmes.dl.conversion.importers.GeneratorDiagramDataImporter;
import com.powsybl.cgmes.dl.conversion.importers.HvdcLineDiagramDataImporter;
import com.powsybl.cgmes.dl.conversion.importers.LineDiagramDataImporter;
import com.powsybl.cgmes.dl.conversion.importers.LoadDiagramDataImporter;
import com.powsybl.cgmes.dl.conversion.importers.ShuntDiagramDataImporter;
import com.powsybl.cgmes.dl.conversion.importers.SvcDiagramDataImporter;
import com.powsybl.cgmes.dl.conversion.importers.SwitchDiagramDataImporter;
import com.powsybl.cgmes.dl.conversion.importers.TransformerDiagramDataImporter;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBags;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class CgmesDLImporter {

    private static final Logger LOG = LoggerFactory.getLogger(CgmesDLImporter.class);

    private Network network;
    private CgmesDLModel cgmesDLModel;
    private Map<String, PropertyBags> terminalsDiagramData = new HashMap<>();

    public CgmesDLImporter(Network network, CgmesDLModel cgmesDLModel) {
        this.network = Objects.requireNonNull(network);
        this.cgmesDLModel = Objects.requireNonNull(cgmesDLModel);
    }

    public void importDLData() {
        importTerminalsDLData();
        importBusesDLData();
        importBusbarsDLData();
        importLinesDLData();
        importGeneratorsDLData();
        importLoadsDLData();
        importShuntsDLData();
        importSwitchesDLData();
        importTransformersDLData();
        importHvdcLinesDLData();
        importSvcsDLData();
    }

    private void importTerminalsDLData() {
        LOG.info("Importing Terminals DL Data");
        cgmesDLModel.getTerminalsDiagramData().forEach(terminalDiagramData -> {
            String terminalKey = terminalDiagramData.getId("terminalEquipment") + "_" + terminalDiagramData.get("terminalSide");
            PropertyBags equipmentTerminalsDiagramData = new PropertyBags();
            if (terminalsDiagramData.containsKey(terminalKey)) {
                equipmentTerminalsDiagramData = terminalsDiagramData.get(terminalKey);
            }
            equipmentTerminalsDiagramData.add(terminalDiagramData);
            terminalsDiagramData.put(terminalKey, equipmentTerminalsDiagramData);
        });
    }

    private void importBusesDLData() {
        LOG.info("Importing Buses DL Data");
        BusDiagramDataImporter diagramDataImporter = new BusDiagramDataImporter(network);
        cgmesDLModel.getBusesDiagramData().forEach(busDiagramData -> {
            diagramDataImporter.importDiagramData(busDiagramData);
        });
    }

    private void importBusbarsDLData() {
        LOG.info("Importing Busbars DL Data");
        BusbarDiagramDataImporter busbarDiagramDataImporter = new BusbarDiagramDataImporter(network);
        cgmesDLModel.getBusbarsDiagramData().forEach(busbarDiagramData -> {
            busbarDiagramDataImporter.importDiagramData(busbarDiagramData);
        });
    }

    private void importLinesDLData() {
        LOG.info("Importing Lines DL Data");
        LineDiagramDataImporter diagramDataImporter = new LineDiagramDataImporter(network);
        cgmesDLModel.getLinesDiagramData().forEach(lineDiagramData -> {
            diagramDataImporter.importDiagramData(lineDiagramData);
        });
    }

    private void importGeneratorsDLData() {
        LOG.info("Importing Generators DL Data");
        GeneratorDiagramDataImporter diagramDataImporter = new GeneratorDiagramDataImporter(network, terminalsDiagramData);
        cgmesDLModel.getGeneratorsDiagramData().forEach(generatorDiagramData -> {
            diagramDataImporter.importDiagramData(generatorDiagramData);
        });
    }

    private void importLoadsDLData() {
        LOG.info("Importing Loads DL Data");
        LoadDiagramDataImporter diagramDataImporter = new LoadDiagramDataImporter(network, terminalsDiagramData);
        cgmesDLModel.getLoadsDiagramData().forEach(loadDiagramData -> {
            diagramDataImporter.importDiagramData(loadDiagramData);
        });
    }

    private void importShuntsDLData() {
        LOG.info("Importing Shunts DL Data");
        ShuntDiagramDataImporter diagramDataImporter = new ShuntDiagramDataImporter(network, terminalsDiagramData);
        cgmesDLModel.getShuntsDiagramData().forEach(shuntDiagramData -> {
            diagramDataImporter.importDiagramData(shuntDiagramData);
        });
    }

    private void importSwitchesDLData() {
        LOG.info("Importing Switches DL Data");
        SwitchDiagramDataImporter diagramDataImporter = new SwitchDiagramDataImporter(network, terminalsDiagramData);
        cgmesDLModel.getSwitchesDiagramData().forEach(switchesDiagramData -> {
            diagramDataImporter.importDiagramData(switchesDiagramData);
        });
    }

    private void importTransformersDLData() {
        LOG.info("Importing Transformers DL Data");
        TransformerDiagramDataImporter diagramDataImporter = new TransformerDiagramDataImporter(network, terminalsDiagramData);
        cgmesDLModel.getTransformersDiagramData().forEach(transformersDiagramData -> {
            diagramDataImporter.importDiagramData(transformersDiagramData);
        });
    }

    private void importHvdcLinesDLData() {
        LOG.info("Importing HVDC Lines DL Data");
        HvdcLineDiagramDataImporter diagramDataImporter = new HvdcLineDiagramDataImporter(network);
        cgmesDLModel.getHvdcLinesDiagramData().forEach(hvdcLineDiagramData -> {
            diagramDataImporter.importDiagramData(hvdcLineDiagramData);
        });
    }

    private void importSvcsDLData() {
        LOG.info("Importing Svcs DL Data");
        SvcDiagramDataImporter diagramDataImporter = new SvcDiagramDataImporter(network, terminalsDiagramData);
        cgmesDLModel.getSvcsDiagramData().forEach(svcDiagramData -> {
            diagramDataImporter.importDiagramData(svcDiagramData);
        });
    }

    public Network getNetworkWithDLData() {
        return network;
    }

}
