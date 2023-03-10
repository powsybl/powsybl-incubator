package com.powsybl.opf.parameters.input;

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
    public static final String PARAM_TRANSFORMER_FILE_NAME = "param_generators_reactive.txt";

    private final List<String> variablesTransformers;
    private final String networkVariant;

    public ReactiveTransformerInput(List<String> variablesTransformers, String networkVariant) {
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
        dataBuilder.append("#NetworkId amplId powsyblId");
        for (String transformerId : variablesTransformers) {
            String[] tokens = {this.networkVariant, Integer.toString(
                    stringToIntMapper.getInt(AmplSubset.THREE_WINDINGS_TRANSFO, transformerId)), transformerId};
            dataBuilder.append(String.join(" ", tokens));
            dataBuilder.append("\n");
        }
        //add new line at the end of the file !
        dataBuilder.append("\n");
        return new ByteArrayInputStream(dataBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }

}
