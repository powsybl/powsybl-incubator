/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.protobuf;

import com.powsybl.iidm.network.MinMaxReactiveLimits;
import com.powsybl.iidm.network.ReactiveCapabilityCurve;
import com.powsybl.iidm.network.ReactiveCapabilityCurveAdder;
import com.powsybl.iidm.network.ReactiveLimitsHolder;
import com.powsybl.iidm.protobuf.proto.Iidm;

import java.util.List;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.eu>
 */
public class ReactiveLimitsProto {

    static final ReactiveLimitsProto INSTANCE = new ReactiveLimitsProto();

    public Iidm.ReactiveCapabilityCurve writeReactiveCababilityCurve(ReactiveLimitsHolder holder) {
        ReactiveCapabilityCurve curve = holder.getReactiveLimits(ReactiveCapabilityCurve.class);
        Iidm.ReactiveCapabilityCurve.Builder reactiveBuilder = Iidm.ReactiveCapabilityCurve.newBuilder();
        for (ReactiveCapabilityCurve.Point point : curve.getPoints()) {
            Iidm.Point.Builder pointBuilder = Iidm.Point.newBuilder();
            pointBuilder.setP(point.getP());
            pointBuilder.setMinQ(point.getMinQ());
            pointBuilder.setMaxQ(point.getMaxQ());
            reactiveBuilder.addPoint(pointBuilder);
        }
        return reactiveBuilder.build();
    }

    public Iidm.MinMaxReactiveLimits writeMixMaxReactiveLimits(ReactiveLimitsHolder holder) {
        MinMaxReactiveLimits limits = holder.getReactiveLimits(MinMaxReactiveLimits.class);
        Iidm.MinMaxReactiveLimits.Builder mmReactiveBuilder = Iidm.MinMaxReactiveLimits.newBuilder();
        mmReactiveBuilder.setMinQ(limits.getMinQ());
        mmReactiveBuilder.setMaxQ(limits.getMaxQ());
        return mmReactiveBuilder.build();
    }

    public void readReactiveCapabilityCurve(ReactiveLimitsHolder holder, Iidm.ReactiveCapabilityCurve pReactiveCurve) {
        ReactiveCapabilityCurveAdder curveAdder = holder.newReactiveCapabilityCurve();
        List<Iidm.Point> lPoints = pReactiveCurve.getPointList();
        lPoints.forEach(lPoint -> {
            curveAdder.beginPoint()
                    .setP(lPoint.getP())
                    .setMinQ(lPoint.getMinQ())
                    .setMaxQ(lPoint.getMaxQ())
                    .endPoint();

        });
        curveAdder.add();
    }

    public void readMinMaxReactiveLimits(ReactiveLimitsHolder holder, Iidm.MinMaxReactiveLimits mmLimits) {
        holder.newMinMaxReactiveLimits()
                .setMinQ(mmLimits.getMinQ())
                .setMaxQ(mmLimits.getMaxQ())
                .add();
    }
}
