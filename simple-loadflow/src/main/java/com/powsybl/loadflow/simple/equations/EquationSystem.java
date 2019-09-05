/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.equations;

import com.powsybl.loadflow.simple.network.LfNetwork;
import com.powsybl.math.matrix.Matrix;
import com.powsybl.math.matrix.MatrixFactory;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class EquationSystem {

    private final LfNetwork network;

    private class EquationIndex {

        private final Map<Pair<Integer, EquationType>, Equation> equations = new HashMap<>();

        private boolean invalide = false;

        private final NavigableSet<Equation> sortedEquationsToSolve = new TreeSet<>();

        private final NavigableMap<Variable, NavigableMap<Equation, List<EquationTerm>>> sortedVariablesToFind = new TreeMap<>();

        private void update() {
            if (!invalide) {
                return;
            }

            // index derivatives per variable then per equation
            for (Equation equation : equations.values()) {
                if (equation.isToSolve()) {
                    sortedEquationsToSolve.add(equation);
                    for (EquationTerm equationTerm : equation.getTerms()) {
                        for (Variable variable : equationTerm.getVariables()) {
                            sortedVariablesToFind.computeIfAbsent(variable, k -> new TreeMap<>())
                                    .computeIfAbsent(equation, k -> new ArrayList<>())
                                    .add(equationTerm);
                        }
                    }
                }
            }

            int rowCount = 0;
            for (Equation equation : sortedEquationsToSolve) {
                equation.setRow(rowCount++);
            }

            int columnCount = 0;
            for (Variable variable : sortedVariablesToFind.keySet()) {
                variable.setColumn(columnCount++);
            }

            invalide = false;
        }

        private Equation getEquation(int num, EquationType type) {
            Pair<Integer, EquationType> p = Pair.of(num, type);
            Equation equation = equations.get(p);
            if (equation == null) {
                equation = createEquation(p);
                invalide = true;
            }
            return equation;
        }

        private Equation createEquation(Pair<Integer, EquationType> p) {
            Equation equation = new Equation(p.getLeft(), p.getRight(), EquationSystem.this);
            equations.put(p, equation);
            return equation;
        }

        private Collection<Equation> getEquations() {
            return equations.values();
        }

        private NavigableSet<Equation> getSortedEquationsToSolve() {
            update();
            return sortedEquationsToSolve;
        }

        private NavigableMap<Variable, NavigableMap<Equation, List<EquationTerm>>> getSortedVariablesToFind() {
            update();
            return sortedVariablesToFind;
        }
    }

    private final EquationIndex index = new EquationIndex();

    public EquationSystem(LfNetwork network) {
        this.network = Objects.requireNonNull(network);
    }

    public Equation getEquation(int num, EquationType type) {
        return index.getEquation(num, type);
    }

    public Collection<Equation> getEquations() {
        return index.getEquations();
    }

    public SortedSet<Equation> getEquationsToSolve() {
        return index.getSortedEquationsToSolve();
    }

    public SortedSet<Variable> getVariablesToFind() {
        return index.getSortedVariablesToFind().navigableKeySet();
    }

    public List<String> getRowNames() {
        return getEquationsToSolve().stream()
                .map(eq -> network.getBus(eq.getNum()).getId() + "/" + eq.getType())
                .collect(Collectors.toList());
    }

    public List<String> getColumnNames() {
        return getVariablesToFind().stream()
                .map(v -> network.getBus(v.getNum()).getId() + "/" + v.getType())
                .collect(Collectors.toList());
    }

    public double[] createStateVector(VoltageInitializer initializer) {
        double[] x = new double[getVariablesToFind().size()];
        for (Variable v : getVariablesToFind()) {
            v.initState(initializer, network, x);
        }
        return x;
    }

    public double[] createTargetVector() {
        double[] targets = new double[index.getSortedEquationsToSolve().size()];
        for (Equation equation : index.getSortedEquationsToSolve()) {
            equation.initTarget(network, targets);
        }
        return targets;
    }

    public double[] createEquationVector() {
        double[] fx = new double[index.getSortedEquationsToSolve().size()];
        updateEquationVector(fx);
        return fx;
    }

    public void updateEquationVector(double[] fx) {
        if (fx.length != index.getSortedEquationsToSolve().size()) {
            throw new IllegalArgumentException("Bad equation vector length: " + fx.length);
        }
        Arrays.fill(fx, 0);
        for (Equation equation : index.getSortedEquationsToSolve()) {
            fx[equation.getRow()] = equation.eval();
        }
    }

    public void updateEquations(double[] x) {
        for (Equation equation : index.getEquations()) {
            equation.update(x);
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

        int rowCount = index.getSortedEquationsToSolve().size();
        int columnCount = index.getSortedVariablesToFind().size();

        int estimatedNonZeroValueCount = rowCount * 3;
        Matrix j = matrixFactory.create(rowCount, columnCount, estimatedNonZeroValueCount);
        List<Jacobian.PartialDerivative> partialDerivatives = new ArrayList<>(estimatedNonZeroValueCount);

        for (Map.Entry<Variable, NavigableMap<Equation, List<EquationTerm>>> e : index.getSortedVariablesToFind().entrySet()) {
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
