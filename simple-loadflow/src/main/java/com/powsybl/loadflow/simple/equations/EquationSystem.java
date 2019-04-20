/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.equations;

import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.simple.network.NetworkContext;
import com.powsybl.math.matrix.Matrix;
import com.powsybl.math.matrix.MatrixFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class EquationSystem {

    private static final Logger LOGGER = LoggerFactory.getLogger(EquationSystem.class);

    private final List<EquationTerm> equationTerms;

    private final List<VariableUpdate> variableUpdates;

    private final NetworkContext networkContext;

    private final Map<Equation, List<EquationTerm>> equations = new TreeMap<>();

    private final Map<Variable, Map<Equation, List<EquationTerm>>> variables = new TreeMap<>();

    private final double[] targets;

    public EquationSystem(List<EquationTerm> equationTerms, NetworkContext network) {
        this(equationTerms, Collections.emptyList(), network);
    }

    public EquationSystem(List<EquationTerm> equationTerms, List<VariableUpdate> variableUpdates, NetworkContext networkContext) {
        this.equationTerms = Objects.requireNonNull(equationTerms);
        this.variableUpdates = Objects.requireNonNull(variableUpdates);
        this.networkContext = Objects.requireNonNull(networkContext);

        // index derivatives per variable then per equation
        for (EquationTerm equationTerm : equationTerms) {
            equations.computeIfAbsent(equationTerm.getEquation(), k -> new ArrayList<>())
                    .add(equationTerm);
            for (Variable variable : equationTerm.getVariables()) {
                variables.computeIfAbsent(variable, k -> new TreeMap<>())
                        .computeIfAbsent(equationTerm.getEquation(), k -> new ArrayList<>())
                        .add(equationTerm);
            }
        }

        int rowCount = 0;
        for (Equation equation : equations.keySet()) {
            equation.setRow(rowCount++);
        }

        int columnCount = 0;
        for (Variable variable : variables.keySet()) {
            variable.setColumn(columnCount++);
        }

        targets = new double[equations.size()];
        for (Map.Entry<Equation, List<EquationTerm>> e : equations.entrySet()) {
            Equation eq  = e.getKey();
            eq.initTarget(networkContext, targets);
        }
        for (EquationTerm equationTerm : equationTerms) {
            for (Variable variable : equationTerm.getVariables()) {
                targets[equationTerm.getEquation().getRow()] -= equationTerm.rhs(variable);
            }
        }
    }

    public Set<Equation> getEquations() {
        return equations.keySet();
    }

    public Set<Variable> getVariables() {
        return variables.keySet();
    }

    public NetworkContext getNetworkContext() {
        return networkContext;
    }

    public double[] getTargets() {
        return targets;
    }

    public List<String> getRowNames() {
        return getEquations().stream().sorted().map(eq -> eq.getId() + "/" + eq.getType()).collect(Collectors.toList());
    }

    public List<String> getColumnNames() {
        return getVariables().stream().sorted().map(v -> v.getId() + "/" + v.getType()).collect(Collectors.toList());
    }

    public double[] initState() {
        return initState(LoadFlowParameters.VoltageInitMode.UNIFORM_VALUES);
    }

    public double[] initState(LoadFlowParameters.VoltageInitMode mode) {
        double[] x = new double[getVariables().size()];
        for (Variable v : getVariables()) {
            v.initState(mode, networkContext, x);
        }
        return x;
    }

    public void updateState(double[] x) {
        // update state variable
        for (Variable v : getVariables()) {
            v.updateState(networkContext, x);
        }
        // then other variables
        for (VariableUpdate variableUpdate : variableUpdates) {
            variableUpdate.update(x);
        }
    }

    public void evalFx(double[] x, double[] fx) {
        if (fx.length != equations.size()) {
            throw new IllegalArgumentException("Bad fx vector length: " + fx.length);
        }
        Arrays.fill(fx, 0);
        for (Map.Entry<Equation, List<EquationTerm>> e : equations.entrySet()) {
            Equation equation = e.getKey();
            for (EquationTerm equationTerm : e.getValue()) {
                fx[equation.getRow()] += equationTerm.eval(x);
                for (Variable variable : equationTerm.getVariables()) {
                    fx[equation.getRow()] -= equationTerm.rhs(variable);
                }
            }
        }
        Vectors.minus(fx, targets);
    }

    public double[] evalFx(double[] x) {
        double[] fx = new double[equations.size()];
        evalFx(x, fx);
        return fx;
    }

    public void logLargestMistatches(double[] x) {
        logLargestMistatches(x, 10);
    }

    public void logLargestMistatches(double[] x, int count) {
        double[] fx = evalFx(x);
        Map<String, Double> mismatches = new HashMap<>(equations.size());
        for (Equation equation : equations.keySet()) {
            mismatches.put(equation.getId(), fx[equation.getRow()]);
        }
        mismatches.entrySet().stream()
                .filter(e -> Math.abs(e.getValue()) > Math.pow(10, 7))
                .sorted(Comparator.comparingDouble((Map.Entry<String, Double> e) -> Math.abs(e.getValue())).reversed())
                .limit(count)
                .forEach(e -> LOGGER.info("Mismatch at {}: {}", e.getKey(), e.getValue()));
    }

    public Matrix buildJacobian(MatrixFactory matrixFactory, double[] x) {
        Objects.requireNonNull(matrixFactory);
        Objects.requireNonNull(x);

        int rowCount = equations.size();
        int columnCount = variables.size();

        Matrix j = matrixFactory.create(rowCount, columnCount, rowCount * 3);

        for (Map.Entry<Variable, Map<Equation, List<EquationTerm>>> e : variables.entrySet()) {
            Variable var = e.getKey();
            int column = var.getColumn();
            for (Map.Entry<Equation, List<EquationTerm>> e2 : e.getValue().entrySet()) {
                Equation eq = e2.getKey();
                int row = eq.getRow();
                for (EquationTerm equationTerm : e2.getValue()) {
                    double v = equationTerm.der(var, x);
                    if (v != 0) {
                        j.addValue(row, column, v);
                    }
                }
            }
        }

        return j;
    }
}
