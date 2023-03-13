package com.powsybl.opf.parameters;

import com.powsybl.ampl.executor.IAmplInputFile;
import com.powsybl.ampl.executor.IAmplOutputFile;
import com.powsybl.ampl.executor.IAmplParameters;
import com.powsybl.opf.parameters.input.*;
import com.powsybl.opf.parameters.output.IndicatorOutput;
import com.powsybl.opf.parameters.output.OpenReacResults;
import com.powsybl.opf.parameters.output.ReactiveInvestmentOutput;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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
    private final AlgorithmInput algorithmParams;
    private final ReactiveInvestmentOutput reactiveInvestmentOutput;
    private final IndicatorOutput indicators;

    public OpenReacAmplIOFiles(OpenReacParameters params) {
        this.fixedReactiveGeneratorInput = new FixedReactanceGeneratorInput(params.getFixedGenerators());
        this.variableReactanceShuntsInput = new VariableReactanceShuntsInput(params.getModifiableShunts());
        this.reactiveTransformerInput = new ReactiveTransformerInput(params.getModifiableTransformers());
        this.algorithmParams = new AlgorithmInput(params.getAlgorithmParams());
        this.reactiveInvestmentOutput = new ReactiveInvestmentOutput();
        this.indicators = new IndicatorOutput();
    }

    public List<ReactiveInvestmentOutput.ReactiveInvestment> getReactiveInvestments() {
        return reactiveInvestmentOutput.getInvestments();
    }

    public Map<String, String> getIndicators() {
        return indicators.getIndicators();
    }

    @Override
    public Collection<IAmplInputFile> getInputParameters() {
        return List.of(fixedReactiveGeneratorInput, variableReactanceShuntsInput, reactiveTransformerInput,
                algorithmParams);
    }

    @Override
    public Collection<IAmplOutputFile> getOutputParameters() {
        return List.of(reactiveInvestmentOutput, indicators);
    }

}
