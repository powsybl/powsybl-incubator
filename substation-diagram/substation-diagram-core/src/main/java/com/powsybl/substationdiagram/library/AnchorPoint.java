/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.library;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Objects;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@XmlJavaTypeAdapter(AnchorPointAdapter.class)
public class AnchorPoint {

    private final double x;

    private final double y;

    private final AnchorOrientation orientation;

    /**
     * Constructor
     *
     * @param x    abscissa
     * @param y    ordinate
     * @param orientation connection orientation
     */
    @JsonCreator
    public AnchorPoint(@JsonProperty("x") double x, @JsonProperty("y") double y,
                       @JsonProperty("orientation") AnchorOrientation orientation) {
        this.x = x;
        this.y = y;
        this.orientation = Objects.requireNonNull(orientation);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public AnchorOrientation getOrientation() {
        return orientation;
    }

    /**
     * Rotate the anchorPoints
     */
    public AnchorPoint rotate() {
        switch (orientation) {
            case VERTICAL:
                return new AnchorPoint(y, x, AnchorOrientation.HORIZONTAL);
            case HORIZONTAL:
                return new AnchorPoint(y, x, AnchorOrientation.VERTICAL);
            case NONE:
                return this;
            default:
                throw new AssertionError("Unknown anchor orientation " + orientation);
        }
    }

    @Override
    public String toString() {
        return "AnchorPoint(x=" + x + ", y=" + y + ", orientation=" + orientation + ")";
    }
}
