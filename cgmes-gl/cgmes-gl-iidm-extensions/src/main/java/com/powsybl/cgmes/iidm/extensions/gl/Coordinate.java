/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.iidm.extensions.gl;

/**
 * <a href="https://en.wikipedia.org/wiki/World_Geodetic_System">WGS84</a> coordinate.
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class Coordinate {

    private final double lon;
    private final double lat;

    public Coordinate(double lon, double lat) {
        this.lon = lon;
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public double getLat() {
        return lat;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(lon) + Double.hashCode(lat);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Coordinate) {
            Coordinate c = (Coordinate) obj;
            return lon == c.lon && lat == c.lat;
        }
        return false;
    }

    @Override
    public String toString() {
        return "(" + lat + ", " + lon + ")";
    }

}
