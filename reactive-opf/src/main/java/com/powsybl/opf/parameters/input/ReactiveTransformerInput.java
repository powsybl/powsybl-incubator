package com.powsybl.opf.parameters.input;

import com.powsybl.ampl.converter.AmplConstants;
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
    public static final String PARAM_TRANSFORMER_FILE_NAME = "param_transformers.txt";

    private final List<String> variablesTransformers;
    private static final String QUOTE = "'";

    public String addQuotes(String str) {
        return QUOTE + str + QUOTE;
    }

    public ReactiveTransformerInput(List<String> variablesTransformers) {
        this.variablesTransformers = variablesTransformers;
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
            int amplId = stringToIntMapper.getInt(AmplSubset.THREE_WINDINGS_TRANSFO, transformerId);
            String[] tokens = {Integer.toString(AmplConstants.DEFAULT_VARIANT_INDEX), Integer.toString(
                    amplId), addQuotes(transformerId)};
            dataBuilder.append(String.join(" ", tokens));
            dataBuilder.append("\n");
        }
        //add new line at the end of the file !
        dataBuilder.append("\n");
        return new ByteArrayInputStream(dataBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }

}
