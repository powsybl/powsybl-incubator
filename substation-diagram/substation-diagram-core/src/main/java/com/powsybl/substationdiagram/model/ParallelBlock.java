package com.powsybl.substationdiagram.model;

import java.util.List;

public interface ParallelBlock extends ComposedBlock {

    public static ParallelBlock parallelBlockFactory(List<Block> subBlocks, Cell cell, boolean allowMerge) {
        return new BodyParallelBlock(subBlocks, cell, allowMerge);
    }
}
