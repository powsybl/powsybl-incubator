/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.catalog;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class CatalogReview extends Catalog {

    public CatalogReview(CatalogLocation location) {
        super(location);
    }

    protected void reviewAll(String pattern, Consumer<Path> consumer) throws IOException {
        reviewAll(pattern, consumer, null);
    }

    protected void reviewAll(String pattern, Consumer<Path> consumer, Consumer<Path> dryRunConsumer)
        throws IOException {
        // Review all files or folders that match a given pattern

        // Using "glob" patterns:
        // a double "**" means that there could be intermediate folders
        // a single "*" is any sequence of characters inside the same folder
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(pattern);

        try (Stream<Path> paths = Files.walk(location.dataRoot())) {
            paths.filter(pathMatcher::matches)
                .forEach(path -> {
                    if (catchExceptions) {
                        consumeCatching(path, consumer, dryRunConsumer);
                    } else {
                        consumeThrowing(path, consumer, dryRunConsumer);
                    }
                });
        }
    }

    private void consumeThrowing(Path path, Consumer<Path> consumer, Consumer<Path> dryRunConsumer) {
        LOG.info("reviewing model {}", path);
        if (!dryRun) {
            consumer.accept(path);
        } else if (dryRunConsumer != null) {
            dryRunConsumer.accept(path);
        }
    }

    private void consumeCatching(Path path, Consumer<Path> consumer, Consumer<Path> dryRunConsumer) {
        try {
            LOG.info("reviewing model {}", path);
            if (!dryRun) {
                consumer.accept(path);
            } else if (dryRunConsumer != null) {
                dryRunConsumer.accept(path);
            }
        } catch (Exception x) {
            wrong.put(path, x);
        }
    }

    public void setDryRun(boolean d) {
        dryRun = d;
    }

    public void setCatchExceptions(boolean b) {
        catchExceptions = b;
    }

    public void reportWrong() {
        if (wrong.isEmpty()) {
            return;
        }
        if (LOG.isErrorEnabled()) {
            LOG.error("Something went wrong with following models in catalog:");
            LOG.error("Model, Exception");
            wrong.keySet().stream()
                .sorted()
                .forEach(p -> LOG.error("{}, {}", modelName(p), wrong.get(p).getMessage()));
        }
    }

    protected final Map<Path, Exception> wrong = new HashMap<>();

    private boolean dryRun = false;
    private boolean catchExceptions = true;

    private static final Logger LOG = LoggerFactory.getLogger(CatalogReview.class);
}
