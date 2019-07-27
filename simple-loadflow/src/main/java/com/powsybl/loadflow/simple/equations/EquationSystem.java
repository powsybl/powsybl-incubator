/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.equations;

import com.powsybl.commons.PowsyblException;
import com.powsybl.loadflow.simple.ac.nr.VoltageInitializer;
import com.powsybl.loadflow.simple.network.LfNetwork;
import com.powsybl.math.matrix.Matrix;
import com.powsybl.math.matrix.MatrixFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class EquationSystem {

    private final List<EquationTerm> equationTerms;

    private final LfNetwork network;

    private final NavigableMap<Equation, List<EquationTerm>> sortedEquationsToSolve = new TreeMap<>();

    private final Map<Equation, List<EquationTerm>> equationNotToSolve = new HashMap<>();

    private final NavigableMap<Variable, NavigableMap<Equation, List<EquationTerm>>> sortedVariablesToFind = new TreeMap<>();

    public EquationSystem(List<EquationTerm> equationTerms, LfNetwork network) {
        this.equationTerms = Objects.requireNonNull(equationTerms);
        this.network = Objects.requireNonNull(network);

        // index derivatives per variable then per equation
        for (EquationTerm equationTerm : equationTerms) {
            Equation equation = equationTerm.getEquation();
            if (equation.isToSolve()) {
                sortedEquationsToSolve.computeIfAbsent(equation, k -> new ArrayList<>())
                        .add(equationTerm);
                for (Variable variable : equationTerm.getVariables()) {
                    sortedVariablesToFind.computeIfAbsent(variable, k -> new TreeMap<>())
                            .computeIfAbsent(equation, k -> new ArrayList<>())
                            .add(equationTerm);
                }
            } else {
                equationNotToSolve.computeIfAbsent(equation, k -> new ArrayList<>())
                        .add(equationTerm);
            }
        }

        int rowCount = 0;
        for (Equation equation : sortedEquationsToSolve.keySet()) {
            equation.setRow(rowCount++);
        }

        int columnCount = 0;
        for (Variable variable : sortedVariablesToFind.keySet()) {
            variable.setColumn(columnCount++);
        }
    }

    public SortedSet<Equation> getEquationsToSolve() {
        return sortedEquationsToSolve.navigableKeySet();
    }

    public List<EquationTerm> getEquationTerms(Equation equation) {
        Objects.requireNonNull(equation);
        List<EquationTerm> terms = sortedEquationsToSolve.get(equation);
        if (terms == null) {
            terms = equationNotToSolve.get(equation);
            if (terms == null) {
                throw new PowsyblException("Equation " + equation + " not found");
            }
        }
        return terms;
    }

    public SortedSet<Variable> getVariablesToFind() {
        return sortedVariablesToFind.navigableKeySet();
    }

    public List<String> getRowNames() {
        return getEquationsToSolve().stream().map(eq -> network.getBus(eq.getNum()).getId() + "/" + eq.getType()).collect(Collectors.toList());
    }

    public List<String> getColumnNames() {
        return getVariablesToFind().stream().map(v -> network.getBus(v.getNum()).getId() + "/" + v.getType()).collect(Collectors.toList());
    }

    public double[] initStateVector(VoltageInitializer initializer) {
        double[] x = new double[getVariablesToFind().size()];
        for (Variable v : getVariablesToFind()) {
            v.initState(initializer, network, x);
        }
        return x;
    }

    public double[] initTargetVector() {
        double[] targets = new double[sortedEquationsToSolve.size()];
        for (Map.Entry<Equation, List<EquationTerm>> e : sortedEquationsToSolve.entrySet()) {
            Equation eq  = e.getKey();
            eq.initTarget(network, targets);
            for (EquationTerm equationTerm : e.getValue()) {
                if (equationTerm.hasRhs()) {
                    for (Variable variable : equationTerm.getVariables()) {
                        targets[equationTerm.getEquation().getRow()] -= equationTerm.rhs(variable);
                    }
                }
            }
        }
        return targets;
    }

    public double[] initEquationVector() {
        double[] fx = new double[sortedEquationsToSolve.size()];
        updateEquationVector(fx);
        return fx;
    }

    public void updateEquationVector(double[] fx) {
        if (fx.length != sortedEquationsToSolve.size()) {
            throw new IllegalArgumentException("Bad equation vector length: " + fx.length);
        }
        Arrays.fill(fx, 0);
        for (Map.Entry<Equation, List<EquationTerm>> e : sortedEquationsToSolve.entrySet()) {
            Equation equation = e.getKey();
            for (EquationTerm equationTerm : e.getValue()) {
                fx[equation.getRow()] += equationTerm.eval();
                if (equationTerm.hasRhs()) {
                    for (Variable variable : equationTerm.getVariables()) {
                        fx[equation.getRow()] -= equationTerm.rhs(variable);
                    }
                }
            }
        }
    }

    public void updateEquationTerms(double[] x) {
        for (EquationTerm equationTerm : equationTerms) {
            equationTerm.update(x);
        }
    }

    public void updateNetwork(double[] x) {
        // update state variable
        for (Variable v : getVariablesToFind()) {
            v.updateState(network, x);
        }
    }

    public Jacobian buildJacobian(MatrixFactory matrixFactory) {
        Objects.requireNonNull(matrixFactory);

        int rowCount = sortedEquationsToSolve.size();
        int columnCount = sortedVariablesToFind.size();

        Matrix j = matrixFactory.create(rowCount, columnCount, rowCount * 3);
        List<Jacobian.PartialDerivative> partialDerivatives = new ArrayList<>(equationTerms.size());

        for (Map.Entry<Variable, NavigableMap<Equation, List<EquationTerm>>> e : sortedVariablesToFind.entrySet()) {
            Variable var = e.getKey();
            int column = var.getColumn();
            for (Map.Entry<Equation, List<EquationTerm>> e2 : e.getValue().entrySet()) {
                Equation eq = e2.getKey();
                int row = eq.getRow();
                for (EquationTerm equationTerm : e2.getValue()) {
                    double value = equationTerm.der(var);
                    Matrix.Element element = j.addAndGetElement(row, column, value);
                    partialDerivatives.add(new Jacobian.PartialDerivative(equationTerm, element, var));
                }
            }
        }

        return new Jacobian(j, partialDerivatives);
    }
}
