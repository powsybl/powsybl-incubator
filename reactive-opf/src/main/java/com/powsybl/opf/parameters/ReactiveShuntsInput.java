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
public class ReactiveShuntsInput implements IAmplInputFile {
    public static final String PARAM_SHUNT_FILE_NAME = "param_shunts.txt";
    private final List<String> shuntsIds;
    private final String networkVariant;

    public ReactiveShuntsInput(List<String> shuntsIds, String networkVariant) {
        this.shuntsIds = shuntsIds;
        this.networkVariant = networkVariant;
    }

    @Override
    public String getFileName() {
        return PARAM_SHUNT_FILE_NAME;
    }

    @Override
    public InputStream getParameterFileAsStream(StringToIntMapper<AmplSubset> stringToIntMapper) {
        StringBuilder dataBuilder = new StringBuilder();
        dataBuilder.append("#NetworkId amplId powsyblId");
        for (String shuntsId : shuntsIds) {
            String[] tokens = {this.networkVariant, Integer.toString(
                    stringToIntMapper.getInt(AmplSubset.SHUNT, shuntsId)), shuntsId};
            dataBuilder.append(String.join(" ", tokens));
            dataBuilder.append("\n");
        }
        //add new line at the end of the file !
        dataBuilder.append("\n");
        return new ByteArrayInputStream(dataBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }

}
