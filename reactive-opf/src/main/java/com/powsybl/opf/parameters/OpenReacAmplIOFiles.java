package com.powsybl.opf.parameters;

import com.powsybl.ampl.executor.IAmplInputFile;
import com.powsybl.ampl.executor.IAmplOutputFile;
import com.powsybl.ampl.executor.IAmplParameters;

import java.util.Collection;
import java.util.List;

public class OpenReacAmplIOFiles implements IAmplParameters {

    private final String networkVariant;
    private final List<String> shunts;
    private final ReactiveInvestmentOutput reactiveInvestmentOutput;

    public OpenReacAmplIOFiles(String networkVariant, OpenReacParameters params) {
        this.networkVariant = networkVariant;
        this.shunts = params.getModifiableShunts();
        this.reactiveInvestmentOutput = new ReactiveInvestmentOutput();
    }

    public List<ReactiveInvestmentOutput.ReactiveInvestment> getReactiveInvestments() {
        return reactiveInvestmentOutput.getInvestments();
    }

    @Override
    public Collection<IAmplInputFile> getInputParameters() {
        return List.of(new ReactiveShuntsInput(shunts, networkVariant));
    }

    @Override
    public Collection<IAmplOutputFile> getOutputParameters() {
        return List.of(reactiveInvestmentOutput);
    }

}
