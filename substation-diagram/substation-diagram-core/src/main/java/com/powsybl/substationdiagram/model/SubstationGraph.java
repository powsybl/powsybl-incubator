/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.*;

/**
 * This class builds the connectivity among the voltageLevels of a substation
 * buildSubstationGraph establishes the List of nodes, edges
 *
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public class SubstationGraph {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubstationGraph.class);

    private Substation substation;

    private final boolean useName;

    private final List<Graph> nodes = new ArrayList<>();

    private final List<Edge> edges = new ArrayList<>();

    private final Map<String, Graph> nodesById = new HashMap<>();

    /**
     * Constructor
     */
    public SubstationGraph(Substation substation, boolean useName) {
        this.substation = Objects.requireNonNull(substation);
        this.useName = useName;
    }

    boolean isUseName() {
        return useName;
    }

    public static SubstationGraph create(Substation s) {
        return create(s, false);
    }

    public static SubstationGraph create(Substation s, boolean useName) {
        Objects.requireNonNull(s);
        SubstationGraph g = new SubstationGraph(s, useName);
        g.buildSubstationGraph(useName);
        return g;
    }

    public static Map<String, SubstationGraph> create(Network network) {
        return create(network, false);
    }

    public static Map<String, SubstationGraph> create(Network network, boolean useName) {
        Map<String, SubstationGraph> graphs = new HashMap<>();
        for (Substation s : network.getSubstations()) {
            graphs.put(s.getId(), create(s, useName));
        }
        return graphs;
    }

    private void buildSubstationGraph(boolean useName) {
        // building the graph for each voltageLevel (ordered by descending voltageLevel nominalV)
        substation.getVoltageLevelStream()
                .sorted(Comparator.comparing(VoltageLevel::getNominalV)
                        .reversed()).forEach(v -> {
                            Graph vlGraph = Graph.create(v, useName);
                            addNode(vlGraph);
                        });

        LOGGER.info("Number of node : {} ", nodes.size());

        // Creation des SnakeLine reliant les nodes dans les voltageLevels
        addSnakeLines();
    }

    private void addSnakeLines() {
        // Two windings transformer
        //
        for (TwoWindingsTransformer transfo : substation.getTwoWindingsTransformers()) {
            Terminal t1 = transfo.getTerminal1();
            Terminal t2 = transfo.getTerminal2();

            VoltageLevel v1 = t1.getVoltageLevel();
            VoltageLevel v2 = t2.getVoltageLevel();

            Graph g1 = getNode(v1.getId());
            Graph g2 = getNode(v2.getId());
            if (g1 == null) {
                throw new PowsyblException("Graph for voltageLevel " + v1.getId() + " not found");
            }
            if (g2 == null) {
                throw new PowsyblException("Graph for voltageLevel " + v2.getId() + " not found");
            }

            String id1 = transfo.getId() + "_" + transfo.getSide(t1).name();
            String id2 = transfo.getId() + "_" + transfo.getSide(t2).name();
            Node n1 = g1.getNode(id1);
            Node n2 = g2.getNode(id2);
            if (n1 == null) {
                throw new PowsyblException("Node " + id1 + " not found");
            }
            if (n2 == null) {
                throw new PowsyblException("Node " + id2 + " not found");
            }

            addEdge(n1, n2);
        }
    }

    public void whenSerializingUsingJsonAnyGetterThenCorrect(Writer writer) {
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        try {
            mapper.writeValue(writer, this);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void addNode(Graph node) {
        nodes.add(node);
        nodesById.put(node.getVoltageLevel().getId(), node);
    }

    public Graph getNode(String id) {
        Objects.requireNonNull(id);
        return nodesById.get(id);
    }

    public void addEdge(Node n1, Node n2) {
        Edge sl = new Edge(n1, n2);
        edges.add(sl);
    }

    public List<Graph> getNodes() {
        return new ArrayList<>(nodes);
    }

    public List<Edge> getEdges() {
        return new ArrayList<>(edges);
    }

    public Substation getSubstation() {
        return substation;
    }

    public boolean graphAdjacents(Graph g1, Graph g2) {
        int nbNodes = nodes.size();
        for (int i = 0; i < nbNodes; i++) {
            if (nodes.get(i) == g1 && i < (nbNodes - 1) && nodes.get(i + 1) == g2) {
                return true;
            }
        }
        return false;
    }
}
