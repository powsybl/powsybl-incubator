/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.matpower.converter;

import com.powsybl.commons.AbstractConverterTest;
import com.powsybl.commons.datasource.FileDataSource;
import com.powsybl.commons.datasource.ResourceDataSource;
import com.powsybl.commons.datasource.ResourceSet;
import com.powsybl.iidm.import_.Importer;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.xml.NetworkXml;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.eu>
 */
public class MatpowerImporterTest extends AbstractConverterTest {

    @Test
    public void baseTest() {
        Importer importer = new MatpowerImporter();
        assertEquals("MATPOWER", importer.getFormat());
        assertEquals("MATPOWER Format to IIDM converter", importer.getComment());
    }

    @Test
    public void copyTest() {
        new MatpowerImporter().copy(new ResourceDataSource("case118", new ResourceSet("/", "case118.m")),
            new FileDataSource(fileSystem.getPath("/work"), "copy"));
        assertTrue(Files.exists(fileSystem.getPath("/work").resolve("copy.m")));
    }

    @Test
    public void existsTest() {
        assertTrue(new MatpowerImporter().exists(new ResourceDataSource("case118", new ResourceSet("/", "case118.m"))));
    }

    private void testNetwork(Network network) throws IOException {
        Path file = fileSystem.getPath("/work/" + network.getId() + ".xiidm");
        NetworkXml.write(network, file);
        try (InputStream is = Files.newInputStream(file)) {
            compareTxt(getClass().getResourceAsStream("/" + network.getId() + ".xiidm"), is);
        }
    }

    @Test
    public void testCase118() throws IOException {
        testNetwork(MatpowerNetworkFactory.create118());
    }

    @Test
    public void testCase14() throws IOException {
        testNetwork(MatpowerNetworkFactory.create14());
    }

    @Test
    public void testCase30() throws IOException {
        testNetwork(MatpowerNetworkFactory.create30());
    }

    @Test
    public void testCase300() throws IOException {
        testNetwork(MatpowerNetworkFactory.create300());
    }

    @Test
    public void testCase57() throws IOException {
        testNetwork(MatpowerNetworkFactory.create57());
    }

    @Test
    public void testCase9() throws IOException {
        testNetwork(MatpowerNetworkFactory.create9());
    }

}
