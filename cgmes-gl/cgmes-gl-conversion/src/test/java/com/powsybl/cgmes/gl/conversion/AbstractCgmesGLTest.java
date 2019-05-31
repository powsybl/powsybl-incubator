/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.gl.conversion;

import java.util.Arrays;

import org.junit.Before;

import com.powsybl.cgmes.iidm.extensions.gl.GLTestUtils;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public abstract class AbstractCgmesGLTest {

    protected final String namespace = "http://network#";
    protected PropertyBags substationsPropertyBags;
    protected PropertyBags linesPropertyBags;

    @Before
    public void setUp() {
        substationsPropertyBags = new PropertyBags(Arrays.asList(createSubstationPropertyBag(namespace + "Substation1", "Substation1", CgmesGLUtils.COORDINATE_SYSTEM_NAME,
                                                                                             CgmesGLUtils.COORDINATE_SYSTEM_URN, GLTestUtils.SUBSTATION_1_X, GLTestUtils.SUBSTATION_1_Y),
                                                                 createSubstationPropertyBag(namespace + "Substation2", "Substation2", CgmesGLUtils.COORDINATE_SYSTEM_NAME,
                                                                                             CgmesGLUtils.COORDINATE_SYSTEM_URN, GLTestUtils.SUBSTATION_2_X, GLTestUtils.SUBSTATION_2_Y)));
        linesPropertyBags = new PropertyBags(Arrays.asList(createLinePropertyBag(namespace + "Line", "Line", CgmesGLUtils.COORDINATE_SYSTEM_NAME, CgmesGLUtils.COORDINATE_SYSTEM_URN,
                                                                                 GLTestUtils.SUBSTATION_1_X, GLTestUtils.SUBSTATION_1_Y, 1),
                                                           createLinePropertyBag(namespace + "Line", "Line", CgmesGLUtils.COORDINATE_SYSTEM_NAME, CgmesGLUtils.COORDINATE_SYSTEM_URN,
                                                                                 GLTestUtils.LINE_1_X, GLTestUtils.LINE_1_Y, 2),
                                                           createLinePropertyBag(namespace + "Line", "Line", CgmesGLUtils.COORDINATE_SYSTEM_NAME, CgmesGLUtils.COORDINATE_SYSTEM_URN,
                                                                                 GLTestUtils.LINE_2_X, GLTestUtils.LINE_2_Y, 3),
                                                           createLinePropertyBag(namespace + "Line", "Line", CgmesGLUtils.COORDINATE_SYSTEM_NAME, CgmesGLUtils.COORDINATE_SYSTEM_URN,
                                                                                 GLTestUtils.SUBSTATION_2_X, GLTestUtils.SUBSTATION_2_Y, 4)));
    }

    protected PropertyBag createSubstationPropertyBag(String powerSystemResource, String name, String crsName, String crsUrn, double x, double y) {
        PropertyBag propertyBag = new PropertyBag(Arrays.asList("powerSystemResource", "name", "crsName", "crsUrn", "x", "y"));
        propertyBag.put("powerSystemResource", powerSystemResource);
        propertyBag.put("name", name);
        propertyBag.put("crsName", crsName);
        propertyBag.put("crsUrn", crsUrn);
        propertyBag.put("x", Double.toString(x));
        propertyBag.put("y", Double.toString(y));
        return propertyBag;
    }

    protected PropertyBag createLinePropertyBag(String powerSystemResource, String name, String crsName, String crsUrn, double x, double y, int seq) {
        PropertyBag propertyBag = new PropertyBag(Arrays.asList("powerSystemResource", "name", "crsName", "crsUrn", "x", "y", "seq"));
        propertyBag.put("powerSystemResource", powerSystemResource);
        propertyBag.put("name", name);
        propertyBag.put("crsName", crsName);
        propertyBag.put("crsUrn", crsUrn);
        propertyBag.put("x", Double.toString(x));
        propertyBag.put("y", Double.toString(y));
        propertyBag.put("seq", Integer.toString(seq));
        return propertyBag;
    }

}