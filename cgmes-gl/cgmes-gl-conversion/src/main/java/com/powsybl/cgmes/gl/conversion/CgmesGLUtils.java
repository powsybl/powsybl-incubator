/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.gl.conversion;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public final class CgmesGLUtils {

    public static final String COORDINATE_SYSTEM_NAME = "WGS84";
    public static final String COORDINATE_SYSTEM_URN = "urn:ogc:def:crs:EPSG::4326";

    private CgmesGLUtils() {
    }

    public static boolean checkCoordinateSystem(String crsName, String crsUrn) {
        return COORDINATE_SYSTEM_NAME.equals(crsName) && COORDINATE_SYSTEM_URN.equals(crsUrn);
    }

}
