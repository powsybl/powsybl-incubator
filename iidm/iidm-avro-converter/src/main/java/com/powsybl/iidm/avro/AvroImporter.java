/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.avro;

import com.google.auto.service.AutoService;
import com.google.common.base.Joiner;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.iidm.import_.ImportOptions;
import com.powsybl.iidm.import_.Importer;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.NetworkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Properties;

import static com.powsybl.iidm.avro.IidmAvroConstants.VERSION;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.eu>
 */
@AutoService(Importer.class)
public class AvroImporter implements Importer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AvroImporter.class);

    private static final String[] EXTENSIONS = {"aiidm"};

    @Override
    public String getFormat() {
        return "AIIDM";
    }

    @Override
    public String getComment() {
        return "IIDM avro v " + VERSION + " importer";
    }

    private String findExtension(ReadOnlyDataSource dataSource) throws IOException {
        for (String ext : EXTENSIONS) {
            if (dataSource.exists(null, ext)) {
                return ext;
            }
        }
        return null;
    }

    @Override
    public boolean exists(ReadOnlyDataSource dataSource) {
        try {
            String ext = findExtension(dataSource);
            return exists(dataSource, ext);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean exists(ReadOnlyDataSource dataSource, String ext) throws IOException {
        try {
            if (ext != null) {
                try (InputStream is = dataSource.newInputStream(null, ext)) {
                    //Iidm.Network pNetwork = Iidm.Network.parseFrom(is);
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public Network importData(ReadOnlyDataSource dataSource, NetworkFactory networkFactory, Properties parameters) {
        Objects.requireNonNull(dataSource);
        Network network;

        long startTime = System.currentTimeMillis();
        try {
            String ext = findExtension(dataSource);
            if (ext == null) {
                throw new PowsyblException("File " + dataSource.getBaseName()
                        + "." + Joiner.on("|").join(EXTENSIONS) + " not found");
            }

            ImportOptions options = new ImportOptions();
            network = IidmAvro.read(dataSource, networkFactory, options, ext);
            LOGGER.debug("AIIDM import done in {} ms", System.currentTimeMillis() - startTime);
        } catch (IOException e) {
            throw new PowsyblException(e);
        }
        return network;
    }
}
