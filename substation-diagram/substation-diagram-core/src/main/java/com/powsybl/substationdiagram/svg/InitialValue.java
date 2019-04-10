package com.powsybl.substationdiagram.svg;

import java.util.Optional;

import com.powsybl.substationdiagram.svg.SubstationDiagramInitialValueProvider.Direction;

public class InitialValue {

    private final Direction direction1;
    private final Direction direction2;
    private final String label1;
    private final String label2;

    public InitialValue(Direction dir1, Direction dir2, String text1, String text2) {
        direction1 = dir1;
        direction2 = dir2;
        label1 = text1;
        label2 = text2;
    }

    public Optional<Direction> getArrowDirection1() {
        return Optional.ofNullable(direction1);
    }

    public Optional<Direction> getArrowDirection2() {
        return Optional.ofNullable(direction2);
    }

    public Optional<String> getLabel1() {
        return Optional.ofNullable(label1);
    }

    public Optional<String> getLabel2() {
        return Optional.ofNullable(label2);
    }

}
