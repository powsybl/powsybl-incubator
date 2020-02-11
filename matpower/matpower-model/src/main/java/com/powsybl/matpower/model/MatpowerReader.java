/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.matpower.model;

import com.univocity.parsers.common.processor.BeanListProcessor;
import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.eu>
 */
public class MatpowerReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(MatpowerReader.class);

    enum MatpowerSection {
        BUS,
        BRANCH,
        GENERATOR
    }

    private String processCaseName(String str) {
        String str2 = str.replace(';', ' ');
        final StringTokenizer st = new StringTokenizer(str2, " ");
        st.nextToken(); // function
        st.nextToken(); // mpc
        st.nextToken(); // =
        final String id = st.nextToken().toString();
        return id;
    }

    private String processMatlabAssignment(String str) {
        String str2 = str.replace(';', ' ');
        final StringTokenizer st = new StringTokenizer(str2, " ");
        st.nextToken(); // mpc.XYZ
        st.nextToken(); // =
        final String val = st.nextToken().toString();
        return val;
    }

    public MatpowerModel read(BufferedReader reader) throws IOException {
        String line = reader.readLine();

        String title = processCaseName(line);
        MatpowerModel model = new MatpowerModel(title);

        MatpowerSection section = null;
        List<String> lines = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("%") || (line.trim().length() == 0)) {
                //skip comments and empty lines
            }  else if (line.startsWith("mpc.version ")) {
                String version = processMatlabAssignment(line);
                model.setVersion(version);
            }  else if (line.startsWith("mpc.baseMVA ")) {
                Double baseMva = Double.parseDouble(processMatlabAssignment(line));
                model.setBaseMva(baseMva);
            } else if (line.startsWith("mpc.bus ")) {
                section = MatpowerSection.BUS;
            } else if (line.startsWith("mpc.gen ")) {
                section = MatpowerSection.GENERATOR;
            } else if (line.startsWith("mpc.branch ")) {
                section = MatpowerSection.BRANCH;
            } else if (line.startsWith("];")) {
                if (section != null) {
                    parseLines(lines, model, section);
                    lines.clear();
                    section = null;
                }
            } else {
                if (section != null) {
                    lines.add(line);
                }
            }
        }

        return model;
    }

    private void parseLines(List<String> lines, MatpowerModel model, MatpowerSection section) {
        switch (section) {
            case BUS:
                model.getBuses().addAll(parseLines(lines, MBus.class));
                break;
            case GENERATOR:
                model.getGenerators().addAll(parseLines(lines, MGen.class));
                break;
            case BRANCH:
                model.getBranches().addAll(parseLines(lines, MBranch.class));
                break;
            default:
                throw new IllegalStateException("Section unknown: " + section);
        }
    }

    private static <T> List<T> parseLines(List<String> lines, Class<T> aClass) {
        LOGGER.debug("Parsing data for class {}", aClass);
        BeanListProcessor<T> rowProcessor = new BeanListProcessor<T>(aClass);
        TsvParserSettings settings = new TsvParserSettings();
        settings.setProcessor(rowProcessor);
        settings.setHeaderExtractionEnabled(false);
        settings.setLineSeparatorDetectionEnabled(true);
        settings.getFormat().setLineSeparator(";");
        TsvParser parser = new TsvParser(settings);
        lines.stream().map(String::trim).forEach(parser::parseLine);
        return rowProcessor.getBeans();
    }
}
