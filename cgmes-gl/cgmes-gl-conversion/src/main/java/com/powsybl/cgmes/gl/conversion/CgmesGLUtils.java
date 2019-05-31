package com.powsybl.cgmes.gl.conversion;

public final class CgmesGLUtils {

    public static final String COORDINATE_SYSTEM_NAME = "WGS84";
    public static final String COORDINATE_SYSTEM_URN = "urn:ogc:def:crs:EPSG::4326";

    private CgmesGLUtils() {
    }

    public static boolean checkCoordinateSystem(String crsName, String crsUrn) {
        return COORDINATE_SYSTEM_NAME.equals(crsName) && COORDINATE_SYSTEM_URN.equals(crsUrn);
    }

}
