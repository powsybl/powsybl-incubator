package com.powsybl.opf.parameters.input;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.executor.IAmplInputFile;
import com.powsybl.commons.util.StringToIntMapper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class AlgorithmInput implements IAmplInputFile {
    private static final String ALGORITHM_INPUT_FILE = "param_algo.txt";

    public enum OpenReacAlgoParam {
        TEST_PARAM("test-ampl-parameter");

        private final String name;

        OpenReacAlgoParam(String name) {
            this.name = name;
        }

        String getName() {
            return this.name;
        }
    }

    private final Map<OpenReacAlgoParam, String> paramsMap;

    public AlgorithmInput(Map<OpenReacAlgoParam, String> paramsMap) {
        this.paramsMap = paramsMap;
    }

    @Override
    public String getFileName() {
        return ALGORITHM_INPUT_FILE;
    }

    @Override
    public InputStream getParameterFileAsStream(StringToIntMapper<AmplSubset> stringToIntMapper) {
        StringBuilder dataBuilder = new StringBuilder();
        for (Map.Entry<OpenReacAlgoParam, String> entry : paramsMap.entrySet()) {
            dataBuilder.append(entry.getKey().getName()).append(" ").append(entry.getValue()).append("\n");
        }
        //add new line at the end of the file !
        dataBuilder.append("\n");
        return new ByteArrayInputStream(dataBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }
}
