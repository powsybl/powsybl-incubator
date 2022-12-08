package com.powsybl.incubator.simulator.util.extensions;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.TwoWindingsTransformer;

import java.util.HashMap;
import java.util.Map;

public class ShortCircuitNormExtensions {

    private Map<ThreeWindingsTransformer, ThreeWindingsTransformerNorm> t3wNormExtensions;
    private Map<TwoWindingsTransformer, TwoWindingsTransformerNorm> t2wNormExtensions;
    private Map<Generator, GeneratorNorm> genNormExtensions;

    public ShortCircuitNormExtensions() {
        this.t3wNormExtensions = new HashMap<>();
        this.t2wNormExtensions = new HashMap<>();
        this.genNormExtensions = new HashMap<>();
    }

    public ThreeWindingsTransformerNorm getNormExtension(ThreeWindingsTransformer t3w) {
        if (t3wNormExtensions.containsKey(t3w)) {
            return t3wNormExtensions.get(t3w);
        } else {
            return null;
        }
    }

    public TwoWindingsTransformerNorm getNormExtension(TwoWindingsTransformer t2w) {
        if (t2wNormExtensions.containsKey(t2w)) {
            return t2wNormExtensions.get(t2w);
        } else {
            return null;
        }
    }

    public GeneratorNorm getNormExtension(Generator gen) {
        if (genNormExtensions.containsKey(gen)) {
            return genNormExtensions.get(gen);
        } else {
            return null;
        }
    }

    public void setNormExtension(ThreeWindingsTransformer t3w, ThreeWindingsTransformerNorm t3wNorm) {
        t3wNormExtensions.put(t3w, t3wNorm);
    }

    public void setNormExtension(TwoWindingsTransformer t2w, TwoWindingsTransformerNorm t2wNorm) {
        t2wNormExtensions.put(t2w, t2wNorm);
    }

    public void setNormExtension(Generator gen, GeneratorNorm genNorm) {
        genNormExtensions.put(gen, genNorm);
    }

}
