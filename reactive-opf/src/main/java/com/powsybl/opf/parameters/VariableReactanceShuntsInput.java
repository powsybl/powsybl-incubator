package com.powsybl.opf.parameters;

import com.powsybl.ampl.converter.AmplSubset;

import java.util.List;

/**
 * List of shunts that can be modified by OpenReac
 * timestep num bus id
 */
public class VariableReactanceShuntsInput extends AbstractReactiveElementInput {
    public static final String PARAM_SHUNT_FILE_NAME = "param_shunts.txt";

    public VariableReactanceShuntsInput(List<String> elementIds, String networkVariant) {
        super(elementIds, networkVariant);
    }

    @Override
    AmplSubset getElementAmplSubset() {
        return AmplSubset.SHUNT;
    }

    @Override
    public String getFileName() {
        return PARAM_SHUNT_FILE_NAME;
    }

}
