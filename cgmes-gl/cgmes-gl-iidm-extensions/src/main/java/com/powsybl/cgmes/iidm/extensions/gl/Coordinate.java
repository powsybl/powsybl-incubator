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

    private final double longitude;
    private final double latitude;

    public Coordinate(double longitude, double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(longitude) + Double.hashCode(latitude);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Coordinate) {
            Coordinate c = (Coordinate) obj;
            return longitude == c.longitude && latitude == c.latitude;
        }
        return false;
    }

    @Override
    public String toString() {
        return "(" + latitude + ", " + longitude + ")";
    }

}
