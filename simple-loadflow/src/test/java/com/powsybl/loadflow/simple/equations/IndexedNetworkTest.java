/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.equations;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import org.junit.Test;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class IndexedNetworkTest {

    class MatrixCell implements Comparable<MatrixCell> {

        final int num1;
        final int num2;

        MatrixCell(int num1, int num2) {
            this.num1 = num1;
            this.num2 = num2;
        }

        @Override
        public int hashCode() {
            return Objects.hash(num1, num2);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof MatrixCell) {
                MatrixCell other = (MatrixCell) obj;
                return num1 == other.num1 && num2 == other.num2;
            }
            return false;
        }

        @Override
        public int compareTo(MatrixCell o) {
            int c = num1 - o.num1;
            if (c == 0) {
                return num2 - o.num2;
            }
            return c;
        }

        @Override
        public String toString() {
            return "(" + num1 + ", " + num2 + ")";
        }
    }

    @Test
    public void test() {
        Network network = EurostagTutorialExample1Factory.create();

        IndexedNetwork indexedNetwork = IndexedNetwork.of(network);

        assertEquals(indexedNetwork.getBusCount(), 4);

        Map<MatrixCell, Long> cellCount = network.getBranchStream().flatMap(branch -> {
            Bus bus1 = branch.getTerminal1().getBusView().getBus();
            Bus bus2 = branch.getTerminal2().getBusView().getBus();
            if (bus1 != null && bus2 != null) {
                int busNum1 = indexedNetwork.getIndex(bus1);
                int busNum2 = indexedNetwork.getIndex(bus2);
                List<MatrixCell> cells = new ArrayList<>();
                if (busNum1 != 0) {
                    cells.add(new MatrixCell(busNum1, busNum1));
                    cells.add(new MatrixCell(busNum1, busNum2));
                }
                if (busNum2 != 0) {
                    cells.add(new MatrixCell(busNum2, busNum2));
                    cells.add(new MatrixCell(busNum2, busNum1));
                }
                return cells.stream();
            }
            return Stream.empty();
        }).collect(Collectors.groupingBy(Function.identity(), TreeMap::new, Collectors.counting()));

        List<MatrixCell> cells2 = new ArrayList<>();
        indexedNetwork.forEachBranchInCscOrder((row, column, branch, side) -> {
            if (row != 0) {
                cells2.add(new MatrixCell(row, column));
            }
        });
        Map<MatrixCell, Long> cellCount2 = cells2.stream()
                .collect(Collectors.groupingBy(Function.identity(), TreeMap::new, Collectors.counting()));

        assertEquals(cellCount, cellCount2);
    }
}
