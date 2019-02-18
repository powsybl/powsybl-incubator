/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.svg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.substationdiagram.library.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Jeanson Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class GraphMetadataTest {

    @Test
    public void test() throws IOException {
        GraphMetadata metadata = new GraphMetadata();
        metadata.addComponentMetadata(new ComponentMetadata(ComponentType.BREAKER,
                                                            null,
                                                            ImmutableList.of(new AnchorPoint(5, 4, AnchorOrientation.NONE)),
                                                            new ComponentSize(10, 12)));
        metadata.addNodeMetadata(new GraphMetadata.NodeMetadata("id1", ComponentType.BREAKER, true));
        metadata.addNodeMetadata(new GraphMetadata.NodeMetadata("id2", ComponentType.BUSBAR_SECTION, false));
        metadata.addWireMetadata(new GraphMetadata.WireMetadata("id3", "id1", "id2"));

        ObjectMapper objectMapper = JsonUtil.createObjectMapper();
        String json = objectMapper.writerWithDefaultPrettyPrinter()
                                  .writeValueAsString(metadata);

        GraphMetadata metadata2 = objectMapper.readValue(json, GraphMetadata.class);
        assertEquals(1, metadata2.getComponentMetadata().size());
        assertNotNull(metadata2.getComponentMetadata(ComponentType.BREAKER));
        Assert.assertEquals(1, metadata2.getComponentMetadata(ComponentType.BREAKER).getAnchorPoints().size());
        Assert.assertEquals(5, metadata2.getComponentMetadata(ComponentType.BREAKER).getAnchorPoints().get(0).getX(), 0);
        Assert.assertEquals(4, metadata2.getComponentMetadata(ComponentType.BREAKER).getAnchorPoints().get(0).getY(), 0);
        Assert.assertEquals(AnchorOrientation.NONE, metadata2.getComponentMetadata(ComponentType.BREAKER).getAnchorPoints().get(0).getOrientation());
        Assert.assertEquals(10, metadata2.getComponentMetadata(ComponentType.BREAKER).getSize().getWidth(), 0);
        Assert.assertEquals(12, metadata2.getComponentMetadata(ComponentType.BREAKER).getSize().getHeight(), 0);
        assertEquals(2, metadata2.getNodeMetadata().size());
        assertNotNull(metadata2.getNodeMetadata("id1"));
        assertEquals("id1", metadata2.getNodeMetadata("id1").getId());
        Assert.assertEquals(ComponentType.BREAKER, metadata2.getNodeMetadata("id1").getComponentType());
        Assert.assertEquals(ComponentType.BUSBAR_SECTION, metadata2.getNodeMetadata("id2").getComponentType());
        assertEquals("id2", metadata2.getNodeMetadata("id2").getId());
        assertNotNull(metadata2.getNodeMetadata("id2"));
        assertEquals(1, metadata2.getWireMetadata().size());
        assertNotNull(metadata2.getWireMetadata("id3"));
        assertEquals("id3", metadata2.getWireMetadata("id3").getId());
        assertEquals("id1", metadata2.getWireMetadata("id3").getNodeId1());
        assertEquals("id2", metadata2.getWireMetadata("id3").getNodeId2());
    }
}
