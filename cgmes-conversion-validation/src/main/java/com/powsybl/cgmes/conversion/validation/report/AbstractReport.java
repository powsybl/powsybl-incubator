/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.conversion.validation.report;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

import com.powsybl.cgmes.conversion.validation.ValidationResults;
import com.powsybl.cgmes.interpretation.Configuration;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */

public abstract class AbstractReport {

    public AbstractReport(Path output) {
        this.output = output;
    }

    public abstract void report(Collection<ValidationResults> results) throws IOException;

    String filename(String name, String extension) {
        String prefix = "CGMES";
        String suffix = "";
        if (Configuration.REPORT_FILENAME_WITH_TIMESTAMP) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            LocalDateTime dateTime = LocalDateTime.now();
            suffix = "." + dateTime.format(formatter);
        }
        if (extension != null) {
            suffix = suffix + "." + extension;
        }
        return prefix + name + suffix;
    }

    String filename(String name) {
        return filename(name, null);
    }

    Path outputFile(String name) {
        return output.resolve(filename(name));
    }

    Path outputFile(String name, String extension) {
        return output.resolve(filename(name, extension));
    }

    private final Path output;
}
