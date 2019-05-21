package com.powsybl.cgmes.model.interpretation.test;

import java.util.List;

import com.powsybl.cgmes.interpretation.Interpretation;
import com.powsybl.cgmes.interpretation.InterpretationAlternatives;
import com.powsybl.cgmes.interpretation.InterpretationResults;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesModelForInterpretation;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative;

public class CgmesInterpretationTester {

    public CgmesInterpretationTester (CgmesModelForInterpretation model) {
        this.model = model;
    }
    
    public InterpretationResults test() {
        List<InterpretationAlternative> alternatives = InterpretationAlternatives.configured();
        return new Interpretation(model).interpret(alternatives);
    }
    
    private final CgmesModelForInterpretation model;
}
