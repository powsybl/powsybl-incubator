/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.layout;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class LayoutParameters {

    private double translateX = 20;
    private double translateY = 50;
    private double initialXBus = 0;
    private double initialYBus = 260;
    private double verticalSpaceBus = 25;
    private double horizontalBusPadding = 20;

    private double cellWidth = 50;

    private double externCellHeight = 250;
    private double internCellHeight = 40;

    private double stackHeight = 30;

    private boolean showGrid = false;

    private boolean showInternalNodes = false;

    private double scaleFactor = 1;

    private double horizontalSubstationPadding = 50;
    private double verticalSubstationPadding = 50;

    private boolean drawStraightWires = false;

    private double horizontalSnakeLinePadding = 20;
    private double verticalSnakeLinePadding = 25;
    private double arrowDistance = 20;

    @JsonCreator
    public LayoutParameters() {
    }

    @JsonCreator
    public LayoutParameters(@JsonProperty("translateX") double translateX,
                            @JsonProperty("translateY") double translateY,
                            @JsonProperty("initialXBus") double initialXBus,
                            @JsonProperty("initialYBus") double initialYBus,
                            @JsonProperty("verticalSpaceBus") double verticalSpaceBus,
                            @JsonProperty("horizontalBusPadding") double horizontalBusPadding,
                            @JsonProperty("cellWidth") double cellWidth,
                            @JsonProperty("externCellHeight") double externCellHeight,
                            @JsonProperty("internCellHeight") double internCellHeight,
                            @JsonProperty("stackHeight") double stackHeight,
                            @JsonProperty("showGrid") boolean showGrid,
                            @JsonProperty("showInternalNodes") boolean showInternalNodes,
                            @JsonProperty("scaleFactor") double scaleFactor,
                            @JsonProperty("horizontalSubstationPadding") double horizontalSubstationPadding,
                            @JsonProperty("verticalSubstationPadding") double verticalSubstationPadding,
                            @JsonProperty("drawStraightWires") boolean drawStraightWires,
                            @JsonProperty("horizontalSnakeLinePadding") double horizontalSnakeLinePadding,
                            @JsonProperty("verticalSnakeLinePadding") double verticalSnakeLinePadding,
                            @JsonProperty("arrowDistance") double arrowDistance) {
        this.translateX = translateX;
        this.translateY = translateY;
        this.initialXBus = initialXBus;
        this.initialYBus = initialYBus;
        this.verticalSpaceBus = verticalSpaceBus;
        this.horizontalBusPadding = horizontalBusPadding;
        this.cellWidth = cellWidth;
        this.externCellHeight = externCellHeight;
        this.internCellHeight = internCellHeight;
        this.stackHeight = stackHeight;
        this.showGrid = showGrid;
        this.showInternalNodes = showInternalNodes;
        this.scaleFactor = scaleFactor;
        this.horizontalSubstationPadding = horizontalSubstationPadding;
        this.verticalSubstationPadding = verticalSubstationPadding;
        this.drawStraightWires = drawStraightWires;
        this.horizontalSnakeLinePadding = horizontalSnakeLinePadding;
        this.verticalSnakeLinePadding = verticalSnakeLinePadding;
        this.arrowDistance = arrowDistance;
    }

    public LayoutParameters(LayoutParameters other) {
        Objects.requireNonNull(other);
        translateX = other.translateX;
        translateY = other.translateY;
        initialXBus = other.initialXBus;
        initialYBus = other.initialYBus;
        verticalSpaceBus = other.verticalSpaceBus;
        horizontalBusPadding = other.horizontalBusPadding;
        cellWidth = other.cellWidth;
        externCellHeight = other.externCellHeight;
        internCellHeight = other.internCellHeight;
        stackHeight = other.stackHeight;
        showGrid = other.showGrid;
        showInternalNodes = other.showInternalNodes;
        scaleFactor = other.scaleFactor;
        horizontalSubstationPadding = other.horizontalSubstationPadding;
        verticalSubstationPadding = other.verticalSubstationPadding;
        drawStraightWires = other.drawStraightWires;
        horizontalSnakeLinePadding = other.horizontalSnakeLinePadding;
        verticalSnakeLinePadding = other.verticalSnakeLinePadding;
        arrowDistance = other.arrowDistance;
    }

    public double getTranslateX() {
        return translateX;
    }

    public LayoutParameters setTranslateX(double translateX) {
        this.translateX = translateX;
        return this;
    }

    public double getTranslateY() {
        return translateY;
    }

    public LayoutParameters setTranslateY(double translateY) {
        this.translateY = translateY;
        return this;
    }

    public double getInitialXBus() {
        return initialXBus;
    }

    public LayoutParameters setInitialXBus(double initialXBus) {
        this.initialXBus = initialXBus;
        return this;
    }

    public double getInitialYBus() {
        return initialYBus;
    }

    public LayoutParameters setInitialYBus(double initialYBus) {
        this.initialYBus = initialYBus;
        return this;
    }

    public double getVerticalSpaceBus() {
        return verticalSpaceBus;
    }

    public LayoutParameters setVerticalSpaceBus(double verticalSpaceBus) {
        this.verticalSpaceBus = verticalSpaceBus;
        return this;
    }

    public double getHorizontalBusPadding() {
        return horizontalBusPadding;
    }

    public LayoutParameters setHorizontalBusPadding(double horizontalSpaceBus) {
        this.horizontalBusPadding = horizontalSpaceBus;
        return this;
    }

    public double getCellWidth() {
        return cellWidth;
    }

    public LayoutParameters setCellWidth(double cellWidth) {
        this.cellWidth = cellWidth;
        return this;
    }

    public double getExternCellHeight() {
        return externCellHeight;
    }

    public LayoutParameters setExternCellHeight(double externCellHeight) {
        this.externCellHeight = externCellHeight;
        return this;
    }

    public double getInternCellHeight() {
        return internCellHeight;
    }

    public LayoutParameters setInternCellHeight(double internCellHeight) {
        this.internCellHeight = internCellHeight;
        return this;
    }

    public double getStackHeight() {
        return stackHeight;
    }

    public LayoutParameters setStackHeight(double stackHeight) {
        this.stackHeight = stackHeight;
        return this;
    }

    public boolean isShowGrid() {
        return showGrid;
    }

    public LayoutParameters setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
        return this;
    }

    public boolean isShowInternalNodes() {
        return showInternalNodes;
    }

    public LayoutParameters setShowInternalNodes(boolean showInternalNodes) {
        this.showInternalNodes = showInternalNodes;
        return this;
    }

    public double getScaleFactor() {
        return scaleFactor;
    }

    public LayoutParameters setScaleFactor(double scaleFactor) {
        this.scaleFactor = scaleFactor;
        return this;
    }

    public double getHorizontalSubstationPadding() {
        return horizontalSubstationPadding;
    }

    public LayoutParameters setHorizontalSubstationPadding(double padding) {
        this.horizontalSubstationPadding = padding;
        return this;
    }

    public double getVerticalSubstationPadding() {
        return verticalSubstationPadding;
    }

    public LayoutParameters setVerticalSubstationPadding(double padding) {
        this.verticalSubstationPadding = padding;
        return this;
    }

    public boolean isDrawStraightWires() {
        return drawStraightWires;
    }

    public LayoutParameters setDrawStraightWires(boolean drawStraightWires) {
        this.drawStraightWires = drawStraightWires;
        return this;
    }

    public double getHorizontalSnakeLinePadding() {
        return horizontalSnakeLinePadding;
    }

    public LayoutParameters setHorizontalSnakeLinePadding(double horizontalSnakeLinePadding) {
        this.horizontalSnakeLinePadding = horizontalSnakeLinePadding;
        return this;
    }

    public double getVerticalSnakeLinePadding() {
        return verticalSnakeLinePadding;
    }

    public LayoutParameters setVerticalSnakeLinePadding(double verticalSnakeLinePadding) {
        this.verticalSnakeLinePadding = verticalSnakeLinePadding;
        return this;
    }

    public double getArrowDistance() {
        return arrowDistance;
    }

    public LayoutParameters setArrowDistance(double arrowDistance) {
        this.arrowDistance = arrowDistance;
        return this;
    }
}
