package com.powsybl.opf.parameters;

import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.extensions.AbstractExtendable;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class OpenReacParameters extends AbstractExtendable<OpenReacParameters> {
    private static final String MODULE_CONFIG_NAME = "open-reac";

    //TODO read from yaml this key
    private static final String MODIFIABLE_SHUNT_LIST = "modifiable-shunt-list";

    // VERSION 1.0 : VoltageNegativeDelta, VoltagePositiveDelta
    public static final String VERSION = "1.0";

    /**
     * This map allows to change the bounds of voltage levels.
     * <ul>
     *     <li>
     *         Key: VoltageLevel ID in the network
     *     </li>
     *     <li>
     *         Value: Pair for new voltage level bounds
     *         <ul>
     *            <li>Left: lower bound</li>
     *            <li>Right: upper bound</li>
     *         </ul>
     *     </li>
     * </ul>
     */
    private final Map<String, Pair<Double, Double>> specificVoltageDelta;
    /**
     * List of network's shunts ID
     */
    private final List<String> modifiableShunts;

    public static OpenReacParameters load() {
        return load(PlatformConfig.defaultConfig());
    }

    private static OpenReacParameters load(PlatformConfig config) {
        OpenReacParameters params = new OpenReacParameters();
        return load(params, config);
    }

    private static OpenReacParameters load(OpenReacParameters params, PlatformConfig platformConfig) {
        platformConfig.getOptionalModuleConfig(MODULE_CONFIG_NAME).ifPresent(config -> {
        });
        return params;
    }

    public OpenReacParameters() {
        this.modifiableShunts = new LinkedList<>();
        this.specificVoltageDelta = new HashMap<>();
    }

    public OpenReacParameters addVariableReactanceShunts(String... shuntsIds) {
        this.modifiableShunts.addAll(Arrays.asList(shuntsIds));
        return this;
    }

    public List<String> getModifiableShunts() {
        return modifiableShunts;
    }

    public OpenReacParameters addSpecificVoltageDelta(String voltageLevelId, double lowerVoltage, double upperVoltage) {
        this.specificVoltageDelta.put(voltageLevelId, Pair.of(lowerVoltage, upperVoltage));
        return this;
    }

    public Map<String, Pair<Double, Double>> getSpecificVoltageDelta() {
        return specificVoltageDelta;
    }
}
