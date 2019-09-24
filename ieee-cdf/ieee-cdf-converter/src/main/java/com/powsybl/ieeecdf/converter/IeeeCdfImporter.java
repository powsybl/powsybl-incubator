/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ieeecdf.converter;

import com.google.common.io.ByteStreams;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.ieeecdf.model.IeeeCdfModel;
import com.powsybl.ieeecdf.model.IeeeCdfReader;
import com.powsybl.iidm.import_.Importer;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.NetworkFactory;
import com.powsybl.iidm.parameters.Parameter;

import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class IeeeCdfImporter implements Importer {

    private static final String FORMAT = "IEEE CDF";
    private static final String EXT = "txt";

    @Override
    public String getFormat() {
        return FORMAT;
    }

    @Override
    public List<Parameter> getParameters() {
        return Collections.emptyList();
    }

    @Override
    public String getComment() {
        return "IEEE Common Data Format to IIDM converter";
    }

    @Override
    public boolean exists(ReadOnlyDataSource dataSource) {
        try {
            if (dataSource.exists(null, EXT)) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(dataSource.newInputStream(null, EXT)))) {
                    String titleLine = reader.readLine();
                    if (titleLine != null) {
                        return titleLine.length() >= 44
                                && titleLine.charAt(3) == '/'
                                && titleLine.charAt(6) == '/'
                                && (titleLine.charAt(43) == 'S' || titleLine.charAt(43) == 'W');
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return false;
    }

    @Override
    public void copy(ReadOnlyDataSource fromDataSource, DataSource toDataSource) {
        Objects.requireNonNull(fromDataSource);
        Objects.requireNonNull(toDataSource);
        try {
            try (InputStream is = fromDataSource.newInputStream(null, EXT);
                 OutputStream os = toDataSource.newOutputStream(null, EXT, false)) {
                ByteStreams.copy(is, os);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Network importData(ReadOnlyDataSource dataSource, NetworkFactory networkFactory, Properties parameters) {
        Objects.requireNonNull(dataSource);
        Objects.requireNonNull(networkFactory);

        Network network = networkFactory.createNetwork(dataSource.getBaseName(), FORMAT);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(dataSource.newInputStream(null, EXT)))) {
            IeeeCdfModel model = new IeeeCdfReader().read(reader);

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return network;
    }
}
