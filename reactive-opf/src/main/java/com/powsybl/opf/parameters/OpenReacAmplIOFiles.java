package com.powsybl.opf.parameters;

import com.powsybl.ampl.executor.IAmplInputFile;
import com.powsybl.ampl.executor.IAmplOutputFile;
import com.powsybl.ampl.executor.IAmplParameters;

import java.util.Collection;
import java.util.List;

/**
 * OpenReacAmplIOFiles will interface all inputs and outputs needed for OpenReac to the abtracted Ampl Executor.
 * <p>
 * The user of OpenReac should not see this class directly. One should use {@link OpenReacParameters} for inputs
 * and {@link OpenReacResults} for outputs.
 * However, when adding new inputs (outputs) to OpenReac, one must add {@link IAmplOutputFile} (@link IAmplInputFile)
 * here through {@link OpenReacAmplIOFiles#getInputParameters} ({@link OpenReacAmplIOFiles#getOutputParameters()})
 */
public class OpenReacAmplIOFiles implements IAmplParameters {

    private final FixedReactanceGeneratorInput fixedReactiveGeneratorInput;
    private final VariableReactanceShuntsInput variableReactanceShuntsInput;
    private final ReactiveTransformerInput reactiveTransformerInput;
    private final ReactiveInvestmentOutput reactiveInvestmentOutput;

    public OpenReacAmplIOFiles(String networkVariant, OpenReacParameters params) {
        this.fixedReactiveGeneratorInput = new FixedReactanceGeneratorInput(params.getModifiableShunts(),
                networkVariant);
        this.variableReactanceShuntsInput = new VariableReactanceShuntsInput(params.getFixedGenerators(),
                networkVariant);
        this.reactiveTransformerInput = new ReactiveTransformerInput(params.getModifiableTransformers(),
                networkVariant);
        this.reactiveInvestmentOutput = new ReactiveInvestmentOutput();
    }

    public List<ReactiveInvestmentOutput.ReactiveInvestment> getReactiveInvestments() {
        return reactiveInvestmentOutput.getInvestments();
    }

    @Override
    public Collection<IAmplInputFile> getInputParameters() {
        return List.of(fixedReactiveGeneratorInput, variableReactanceShuntsInput, reactiveTransformerInput);
    }

    @Override
    public Collection<IAmplOutputFile> getOutputParameters() {
        return List.of(reactiveInvestmentOutput);
    }

}
