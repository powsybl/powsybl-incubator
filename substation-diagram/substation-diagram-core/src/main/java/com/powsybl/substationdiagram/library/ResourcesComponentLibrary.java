/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.library;

import com.google.common.collect.Sets;
import com.powsybl.substationdiagram.svg.SVGLoaderToDocument;
import org.apache.batik.anim.dom.SVGOMDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ResourcesComponentLibrary implements ComponentLibrary {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcesComponentLibrary.class);

    private final Map<ComponentType, SVGOMDocument> svgDocuments = new HashMap<>();

    private final Map<ComponentType, Component> components;

    public ResourcesComponentLibrary(String directory) {
        Objects.requireNonNull(directory);
        LOGGER.info("Loading component library from {}...", directory);

        components = Components.load(directory).getComponents()
                .stream()
                .collect(Collectors.toMap(c -> c.getMetadata().getType(), c -> c));

        Set<ComponentType> diff = Sets.difference(EnumSet.copyOf(Arrays.asList(ComponentType.values())), components.keySet());
//        if (!diff.isEmpty()) {
//            throw new PowsyblException("Incomplete component library, " + diff + " component are missing");
//        }

        // preload SVG documents
        SVGLoaderToDocument svgLoadDoc = new SVGLoaderToDocument();
        for (Component component : components.values()) {
            String resourceName = directory + "/" + component.getFileName();
            LOGGER.debug("Reading component {}", resourceName);
            SVGOMDocument doc = svgLoadDoc.read(resourceName);
            svgDocuments.put(component.getMetadata().getType(), doc);
        }
    }

    @Override
    public SVGOMDocument getSvgDocument(ComponentType type) {
        Objects.requireNonNull(type);
        return svgDocuments.get(type);
    }

    @Override
    public List<AnchorPoint> getAnchorPoints(ComponentType type) {
        Objects.requireNonNull(type);
        Component component = components.get(type);
        return component != null ? component.getMetadata().getAnchorPoints()
                                 : Collections.singletonList(new AnchorPoint(0, 0, AnchorOrientation.NONE));
    }

    @Override
    public ComponentSize getSize(ComponentType type) {
        Objects.requireNonNull(type);
        Component component = components.get(type);
        return component != null ? component.getMetadata().getSize() : new ComponentSize(0, 0);
    }
}
