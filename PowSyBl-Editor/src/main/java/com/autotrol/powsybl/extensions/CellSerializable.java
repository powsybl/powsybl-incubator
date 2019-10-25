/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.autotrol.powsybl.extensions;

import com.fasterxml.jackson.core.JsonGenerator;
import com.powsybl.substationdiagram.layout.LayoutParameters;
import com.powsybl.substationdiagram.model.Block;
import com.powsybl.substationdiagram.model.Cell;
import com.powsybl.substationdiagram.model.Graph;
import com.powsybl.substationdiagram.model.Node;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author cgalli
 */
public class CellSerializable implements Cell, Serializable{

    private Cell    original;
    
    private CellSerializable()
    {
        
    }
    
    public CellSerializable(Cell other)
    {
        original=other;
    }
    
    @Override
    public void addNodes(Collection<Node> nodesToAdd) {
        original.addNodes(nodesToAdd);
    }

    @Override
    public List<Node> getNodes() {
        return original.getNodes();
    }

    @Override
    public void removeAllNodes(List<Node> nodeToRemove) {
        original.removeAllNodes(nodeToRemove);
    }

    @Override
    public void setNodes(List<Node> nodes) {
        original.setNodes(nodes);
    }

    @Override
    public void setType(CellType type) {
        original.setType(type);
    }

    @Override
    public CellType getType() {
        return original.getType();
    }

    @Override
    public Block getRootBlock() {
        return original.getRootBlock();
    }

    @Override
    public void setRootBlock(Block rootBlock) {
        original.setRootBlock(rootBlock);
    }

    @Override
    public int getNumber() {
        return original.getNumber();
    }

    @Override
    public void calculateCoord(LayoutParameters layoutParam) {
        original.calculateCoord(layoutParam);
    }

    @Override
    public void writeJson(JsonGenerator generator) throws IOException {
        original.writeJson(generator);
    }

    @Override
    public String getFullId() {
        return original.getFullId();
    }

    @Override
    public Graph getGraph() {
        return original.getGraph();
    }
    
}
