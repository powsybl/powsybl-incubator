/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.protobuf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.eu>
 */
public class TestsStatistics {
    private List<Entry> statistics = new ArrayList<>();

    class Entry {
        Path path;
        String op;
        long expTime;
        long fileSize;

        public Entry(Path path, String op, long expTime) {
            this.path = path;
            this.op = op;
            this.expTime = expTime;
            try {
                fileSize = Files.size(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public TestsStatistics() {
    }

    public void addEntry(Path path, String op, long expTime) {
        statistics.add(new Entry(path, op, expTime));
    }

    public void dumpStatistics() {
        System.out.println("Path,Event,time,file_size");
        statistics.forEach(entry -> {
            System.out.println(entry.path + "," + entry.op + "," + entry.expTime + "," + entry.fileSize);
        });

    }
}
