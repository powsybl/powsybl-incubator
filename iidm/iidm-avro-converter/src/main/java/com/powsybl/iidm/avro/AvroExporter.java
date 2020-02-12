/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.avro;

import com.google.auto.service.AutoService;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.iidm.export.Exporter;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Properties;

import static com.powsybl.iidm.avro.IidmAvroConstants.VERSION;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.eu>
 */
@AutoService(Exporter.class)
public class AvroExporter implements Exporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AvroExporter.class);

    @Override
    public String getFormat() {
        return "AIIDM";
    }

    @Override
    public String getComment() {
        return "IIDM avro v" + VERSION + " exporter";
    }

    @Override
    public void export(Network network, Properties properties, DataSource dataSource) {
        if (network == null) {
            throw new IllegalArgumentException("network is null");
        }
        try {
            long startTime = System.currentTimeMillis();
            IidmAvro.write(network, dataSource, "aiidm");
            LOGGER.debug("AIIDM export done in {} ms", System.currentTimeMillis() - startTime);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
