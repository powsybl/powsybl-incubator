/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.matpower.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.eu>
 */
public class MatpowerReaderWriterTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private FileSystem fileSystem;

    @Before
    public void setUp() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
    }

    @After
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    private void testMatpowerFile(String fileName) throws IOException {
        MatpowerModel model;
        // read the source file in a model
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/" + fileName)))) {
            model = new MatpowerReader().read(reader);
            System.out.println(model);
        }

        // write the model in a file
        Path file = fileSystem.getPath("/work/" + fileName);
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            new MatpowerWriter(model).write(writer);
        }

        // read the model
        MatpowerModel model2;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            model2 = new MatpowerReader().read(reader);
        }

        // compare the two models
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model);
        String json2 = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model2);
        assertEquals(json, json2);
    }

    @Test
    public void testCase9() throws IOException {
        testMatpowerFile("case9.m");
    }

    @Test
    public void testCase14() throws IOException {
        testMatpowerFile("case14.m");
    }

    @Test
    public void testCase30() throws IOException {
        testMatpowerFile("case30.m");
    }

    @Test
    public void testCase57() throws IOException {
        testMatpowerFile("case57.m");
    }

    @Test
    public void testCase118() throws IOException {
        testMatpowerFile("case118.m");
    }

    @Test
    public void testCase300() throws IOException {
        testMatpowerFile("case300.m");
    }
}
