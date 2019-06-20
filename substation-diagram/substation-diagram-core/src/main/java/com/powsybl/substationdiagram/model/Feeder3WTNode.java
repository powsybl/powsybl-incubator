/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.substationdiagram.library.ComponentType;

import java.util.Objects;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public class Feeder3WTNode extends FeederNode {

    private String id2;
    private String name2;
    private String id3;
    private String name3;

    protected Feeder3WTNode(String id, String name, ComponentType componentType, boolean fictitious, Graph graph) {
        super(id, name, componentType, fictitious, graph);
    }

    public static Feeder3WTNode create(Graph graph, ThreeWindingsTransformer twt, ThreeWindingsTransformer.Side side) {
        Objects.requireNonNull(graph);
        Objects.requireNonNull(twt);
        Objects.requireNonNull(side);
        String id = twt.getId() + "_" + side.name();
        String name = twt.getName() + "_" + side.name();
        Feeder3WTNode f = new Feeder3WTNode(id, name, ComponentType.THREE_WINDINGS_TRANSFORMER, false, graph);
        switch (side) {
            case ONE: f.setId2(ThreeWindingsTransformer.Side.TWO.name());
                      f.setName2(ThreeWindingsTransformer.Side.TWO.name());
                      f.setId3(ThreeWindingsTransformer.Side.THREE.name());
                      f.setName3(ThreeWindingsTransformer.Side.THREE.name());
                      break;
            case TWO: f.setId2(ThreeWindingsTransformer.Side.ONE.name());
                      f.setName2(ThreeWindingsTransformer.Side.ONE.name());
                      f.setId3(ThreeWindingsTransformer.Side.THREE.name());
                      f.setName3(ThreeWindingsTransformer.Side.THREE.name());
                      break;
            case THREE: f.setId2(ThreeWindingsTransformer.Side.ONE.name());
                        f.setName2(ThreeWindingsTransformer.Side.ONE.name());
                        f.setId3(ThreeWindingsTransformer.Side.TWO.name());
                        f.setName3(ThreeWindingsTransformer.Side.TWO.name());
                        break;
            default: break;
        }
        return f;
    }

    public String getId2() {
        return id2;
    }

    private void setId2(String id2) {
        this.id2 = id2;
    }

    public String getId3() {
        return id3;
    }

    private void setId3(String id3) {
        this.id3 = id3;
    }

    public String getName2() {
        return name2;
    }

    private void setName2(String name2) {
        this.name2 = name2;
    }

    public String getName3() {
        return name3;
    }

    private void setName3(String name3) {
        this.name3 = name3;
    }
}
