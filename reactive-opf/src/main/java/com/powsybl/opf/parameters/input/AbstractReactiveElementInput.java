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
public abstract class AbstractReactiveElementInput implements IAmplInputFile {
    private final List<String> elementIds;
    private final String networkVariant;

    public AbstractReactiveElementInput(List<String> elementIds, String networkVariant) {
        this.elementIds = elementIds;
        this.networkVariant = networkVariant;
    }

    @Override
    public InputStream getParameterFileAsStream(StringToIntMapper<AmplSubset> stringToIntMapper) {
        StringBuilder dataBuilder = new StringBuilder();
        dataBuilder.append("#NetworkId amplId powsyblId");
        for (String elementID : elementIds) {
            String[] tokens = {this.networkVariant, Integer.toString(
                    stringToIntMapper.getInt(getElementAmplSubset(), elementID)), elementID};
            dataBuilder.append(String.join(" ", tokens));
            dataBuilder.append("\n");
        }
        //add new line at the end of the file !
        dataBuilder.append("\n");
        return new ByteArrayInputStream(dataBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }

    abstract AmplSubset getElementAmplSubset();

}
