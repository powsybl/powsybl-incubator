/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.protobuf;

import com.google.auto.service.AutoService;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.iidm.export.Exporter;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Properties;

import static com.powsybl.iidm.protobuf.IidmProtobufConstants.VERSION;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.eu>
 */
@AutoService(Exporter.class)
public class ProtobufExporter implements Exporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtobufExporter.class);

    @Override
    public String getFormat() {
        return "PIIDM";
    }

    @Override
    public String getComment() {
        return "IIDM protobuf v" + VERSION + " exporter";
    }

    @Override
    public void export(Network network, Properties properties, DataSource dataSource) {
        if (network == null) {
            throw new IllegalArgumentException("network is null");
        }
        try {
            long startTime = System.currentTimeMillis();
            IidmProtobuf.write(network, dataSource, "piidm");
            LOGGER.debug("PIIDM export done in {} ms", System.currentTimeMillis() - startTime);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
