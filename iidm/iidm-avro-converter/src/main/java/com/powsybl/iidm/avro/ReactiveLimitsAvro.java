/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.avro;

import com.powsybl.iidm.network.MinMaxReactiveLimits;
import com.powsybl.iidm.network.ReactiveCapabilityCurve;
import com.powsybl.iidm.network.ReactiveCapabilityCurveAdder;
import com.powsybl.iidm.network.ReactiveLimitsHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.eu>
 */
public class ReactiveLimitsAvro {

    static final ReactiveLimitsAvro INSTANCE = new ReactiveLimitsAvro();

    public com.powsybl.iidm.avro.ReactiveCapabilityCurve writeReactiveCababilityCurve(ReactiveLimitsHolder holder) {
        ReactiveCapabilityCurve curve = holder.getReactiveLimits(ReactiveCapabilityCurve.class);
        com.powsybl.iidm.avro.ReactiveCapabilityCurve.Builder reactiveBuilder = com.powsybl.iidm.avro.ReactiveCapabilityCurve.newBuilder();
        List<Point> aPoints = new ArrayList<>();
        for (ReactiveCapabilityCurve.Point point : curve.getPoints()) {
            com.powsybl.iidm.avro.Point.Builder pointBuilder = com.powsybl.iidm.avro.Point.newBuilder();
            pointBuilder.setP(point.getP());
            pointBuilder.setMinQ(point.getMinQ());
            pointBuilder.setMaxQ(point.getMaxQ());
            aPoints.add(pointBuilder.build());
        }
        reactiveBuilder.setPoint(aPoints);
        return reactiveBuilder.build();
    }

    public com.powsybl.iidm.avro.MinMaxReactiveLimits writeMixMaxReactiveLimits(ReactiveLimitsHolder holder) {
        MinMaxReactiveLimits limits = holder.getReactiveLimits(MinMaxReactiveLimits.class);
        com.powsybl.iidm.avro.MinMaxReactiveLimits.Builder mmReactiveBuilder = com.powsybl.iidm.avro.MinMaxReactiveLimits.newBuilder();
        mmReactiveBuilder.setMinQ(limits.getMinQ());
        mmReactiveBuilder.setMaxQ(limits.getMaxQ());
        return mmReactiveBuilder.build();
    }

    public void readReactiveCapabilityCurve(ReactiveLimitsHolder holder, com.powsybl.iidm.avro.ReactiveCapabilityCurve pReactiveCurve) {
        ReactiveCapabilityCurveAdder curveAdder = holder.newReactiveCapabilityCurve();
        List<com.powsybl.iidm.avro.Point> lPoints = pReactiveCurve.getPoint();
        lPoints.forEach(lPoint -> {
            curveAdder.beginPoint()
                    .setP(lPoint.getP())
                    .setMinQ(lPoint.getMinQ())
                    .setMaxQ(lPoint.getMaxQ())
                    .endPoint();

        });
        curveAdder.add();
    }

    public void readMinMaxReactiveLimits(ReactiveLimitsHolder holder, com.powsybl.iidm.avro.MinMaxReactiveLimits mmLimits) {
        holder.newMinMaxReactiveLimits()
                .setMinQ(mmLimits.getMinQ())
                .setMaxQ(mmLimits.getMaxQ())
                .add();
    }
}
