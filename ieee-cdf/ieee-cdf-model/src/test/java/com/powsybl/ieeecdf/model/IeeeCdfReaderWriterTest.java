/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ieeecdf.model;

import org.junit.Test;

import java.io.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class IeeeCdfReaderWriterTest {

    @Test
    public void test14bus() throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/ieee14cdf.txt")))) {
            IeeeCdfModel model = new IeeeCdfReader().read(reader);
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out))) {
                new IeeeCdfWriter(model).write(writer);
            }
        }
    }
}
