/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.iidm.extensions.gl;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class CoordinateTest {

    @Test
    public void test() {
        Coordinate coordinate = new Coordinate(1.033, 48.567);
        assertEquals(1.033, coordinate.getLon(), 0);
        assertEquals(48.567, coordinate.getLat(), 0);
        assertEquals("(48.567, 1.033)", coordinate.toString());

        new EqualsTester()
                .addEqualityGroup(new Coordinate(1.033, 48.567), new Coordinate(1.033, 48.567))
                .addEqualityGroup(new Coordinate(2.453, 49.547), new Coordinate(2.453, 49.547))
                .testEquals();
    }
}
