package com.powsybl.opf.parameters;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.executor.IAmplInputFile;
import com.powsybl.commons.util.StringToIntMapper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * List of shunts that can be modified by OpenReac
 * timestep num bus id
 */
public class ReactiveTransformerInput implements IAmplInputFile {
    public static class AmplTransformerParameter {
        public final String transformerId;
        public final String busId1;
        public final String busId2;

        public AmplTransformerParameter(String transformerId, String busId1, String busId2) {
            this.transformerId = transformerId;
            this.busId1 = busId1;
            this.busId2 = busId2;
        }
    }

    public static final String PARAM_TRANSFORMER_FILE_NAME = "param_generators_reactive.txt";

    private final List<AmplTransformerParameter> variablesTransformers;
    private final String networkVariant;

    public ReactiveTransformerInput(List<AmplTransformerParameter> variablesTransformers, String networkVariant) {
        this.variablesTransformers = variablesTransformers;
        this.networkVariant = networkVariant;
    }

    @Override
    public String getFileName() {
        return PARAM_TRANSFORMER_FILE_NAME;
    }

    @Override
    public InputStream getParameterFileAsStream(StringToIntMapper<AmplSubset> stringToIntMapper) {
        StringBuilder dataBuilder = new StringBuilder();
        dataBuilder.append("#NetworkId amplTransformerId amplBusId1 amplBusId2 powsyblTransformerId");
        for (AmplTransformerParameter transformerParam : variablesTransformers) {
            String[] tokens = {this.networkVariant, Integer.toString(
                    stringToIntMapper.getInt(AmplSubset.THREE_WINDINGS_TRANSFO,
                            transformerParam.transformerId)), Integer.toString(
                    stringToIntMapper.getInt(AmplSubset.BUS, transformerParam.busId1)), Integer.toString(
                    stringToIntMapper.getInt(AmplSubset.BUS, transformerParam.busId2)), transformerParam.transformerId};
            dataBuilder.append(String.join(" ", tokens));
            dataBuilder.append("\n");
        }
        //add new line at the end of the file !
        dataBuilder.append("\n");
        return new ByteArrayInputStream(dataBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }

}
