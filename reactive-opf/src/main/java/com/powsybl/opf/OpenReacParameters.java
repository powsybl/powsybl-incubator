package com.powsybl.opf;

import com.powsybl.ampl.executor.IAmplInputFile;
import com.powsybl.ampl.executor.IAmplOutputFile;
import com.powsybl.ampl.executor.IAmplParameters;

import java.util.Collection;
import java.util.List;

public class OpenReacParameters implements IAmplParameters {

    @Override
    public Collection<IAmplInputFile> getInputParameters() {
        return null;
    }

    @Override
    public Collection<IAmplOutputFile> getOutputParameters() {
        return List.of(new ReactiveInvestmentOutput());
    }
}
