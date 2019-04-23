package com.powsybl.cgmes.model.interpretation.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.powsybl.cgmes.interpretation.model.cgmes.CgmesLine;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesModelForInterpretation;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesNode;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesPhaseTapChanger;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesRatioTapChanger;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesTransformer;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesTransformerEnd;

final class KronReductionTestsModelFactory {
    private KronReductionTestsModelFactory() {
    }

    private static Map<String, CgmesNode> buildNodes(double nominalVoltage) {

        Map<String, CgmesNode> nodes = new HashMap<>();
        String id;
        double v;
        double angle;
        double p;
        double q;

        id = "Slack";
        v = nominalVoltage;
        angle = 0.0;
        p = -37.685531;
        q = 13.094454;
        nodes.put(id, new CgmesNode(id, nominalVoltage, v, angle, p, q));

        id = "1";
        v = 1.00982854 * nominalVoltage;
        angle = Math.toDegrees(-0.03386869);
        p = 24.0;
        q = 102.0;
        nodes.put(id, new CgmesNode(id, nominalVoltage, v, angle, p, q));

        id = "2";
        v = 1.08423359 * nominalVoltage;
        angle = Math.toDegrees(-0.04845154);
        p = 0.0;
        q = 0.0;
        nodes.put(id, new CgmesNode(id, nominalVoltage, v, angle, p, q));

        id = "3";
        v = 1.06519040 * nominalVoltage;
        angle = Math.toDegrees(-0.04950536);
        p = 13.0;
        q = 5.0;
        nodes.put(id, new CgmesNode(id, nominalVoltage, v, angle, p, q));

        return nodes;
    }

    private static Map<String, CgmesLine> buildLines(double sbase, double vbase) {

        // Impedances are expressed in per-unit values,
        // we convert to engineering units when adding elements to IIDM
        double zpu = sbase / Math.pow(vbase, 2);

        Map<String, CgmesLine> lines = new HashMap<>();

        String id = "LSlack1";
        double r = 0.0052 / zpu;
        double x = 0.089 / zpu;
        double bch = 0.01 * zpu;
        double gch = 0;
        String nodeId1 = "Slack";
        boolean connected1 = true;
        String nodeId2 = "1";
        boolean connected2 = true;
        lines.put(id, new CgmesLine(id, r, x, bch, gch, nodeId1, connected1, nodeId2, connected2));

        return lines;
    }

    private static Map<String, List<String>> equipmentAtNodes(
        Map<String, CgmesLine> lines) {
        Map<String, List<String>> equipmentAtNodes = new HashMap<>();

        lines.keySet().forEach(id -> {
            CgmesLine line = lines.get(id);
            String nodeId1 = line.nodeId1();
            boolean t1connected = line.connected1();
            if (t1connected) {
                List<String> idLines = equipmentAtNodes.computeIfAbsent(nodeId1, z -> new ArrayList<>());
                idLines.add(id);
            }
            String nodeId2 = line.nodeId2();
            boolean t2connected = line.connected2();
            if (t2connected) {
                List<String> idLines = equipmentAtNodes.computeIfAbsent(nodeId2, z -> new ArrayList<>());
                idLines.add(id);
            }
        });
        return equipmentAtNodes;
    }

    public static CgmesModelForInterpretation modelWithLine(boolean lineT2Connected) {
        double sbase = 100.0;
        double nominalVoltage = 400.0;

        // Impedances are expressed in per-unit values,
        // we convert to engineering units when adding elements to IIDM
        double zpu = sbase / Math.pow(nominalVoltage, 2);

        Map<String, CgmesNode> nodes = buildNodes(nominalVoltage);
        String id;

        id = "T";
        double v = 1.06499392 * nominalVoltage;
        double angle = Math.toDegrees(-0.04813391);
        double p = 0;
        double q = 0;
        nodes.put(id, new CgmesNode(id, nominalVoltage, v, angle, p, q));

        Map<String, CgmesLine> lines = buildLines(sbase, nominalVoltage);
        double r;
        double x;
        double bch;
        double gch;
        String nodeId1;
        boolean connected1 = true;
        String nodeId2;
        boolean connected2 = true;

        id = "1T";
        r = 0.0075 / zpu;
        x = 0.064 / zpu;
        bch = 0.53 * zpu;
        gch = 0;
        nodeId1 = "1";
        nodeId2 = "T";
        lines.put(id, new CgmesLine(id, r, x, bch, gch, nodeId1, connected1, nodeId2, connected2));

        id = "T3";
        r = 0.0037 / zpu;
        x = 0.01 / zpu;
        bch = 0.21 * zpu;
        gch = 0;
        nodeId1 = "T";
        nodeId2 = "3";
        lines.put(id, new CgmesLine(id, r, x, bch, gch, nodeId1, connected1, nodeId2, connected2));

        id = "T2";
        r = 0.0016 / zpu;
        x = 0.091 / zpu;
        bch = 0.39 * zpu;
        gch = 0;
        nodeId1 = "T";
        nodeId2 = "2";
        connected2 = lineT2Connected;
        lines.put(id, new CgmesLine(id, r, x, bch, gch, nodeId1, connected1, nodeId2, connected2));

        Map<String, List<String>> equipmentAtNode = equipmentAtNodes(lines);

        Map<String, CgmesTransformer> transformerParameters = new HashMap<>();

        CgmesModelForInterpretation model = new CgmesModelForInterpretation(
            "Line-T2.connected=" + lineT2Connected,
            nodes,
            lines,
            transformerParameters,
            equipmentAtNode);

        return model;
    }

    public static CgmesModelForInterpretation modelWithXfmr2(boolean xfmr2T2Connected) {
        double sbase = 100.0;
        double nominalVoltage = 400.0;
        // Impedances are expressed in per-unit values,
        // we convert to engineering units when adding elements to IIDM
        double zpu = sbase / Math.pow(nominalVoltage, 2);

        Map<String, CgmesNode> nodes = buildNodes(nominalVoltage);
        String id;

        id = "T";
        double v = 1.06499392 * nominalVoltage;
        double angle = Math.toDegrees(-0.04813391);
        double p = 0;
        double q = 0;
        nodes.put(id, new CgmesNode(id, nominalVoltage, v, angle, p, q));

        Map<String, CgmesLine> lines = buildLines(sbase, nominalVoltage);
        double r;
        double x;
        double bch;
        double gch;
        String nodeId1;
        boolean connected1 = true;
        String nodeId2;
        boolean connected2 = true;

        id = "1T";
        r = 0.0075 / zpu;
        x = 0.064 / zpu;
        bch = 0.53 * zpu;
        gch = 0;
        nodeId1 = "1";
        nodeId2 = "T";
        lines.put(id, new CgmesLine(id, r, x, bch, gch, nodeId1, connected1, nodeId2, connected2));

        id = "T3";
        r = 0.0037 / zpu;
        x = 0.01 / zpu;
        bch = 0.21 * zpu;
        gch = 0;
        nodeId1 = "T";
        nodeId2 = "3";
        lines.put(id, new CgmesLine(id, r, x, bch, gch, nodeId1, connected1, nodeId2, connected2));

        Map<String, List<String>> equipmentsAtNodes = equipmentAtNodes(lines);

        Map<String, CgmesTransformer> transformerParameters = new HashMap<>();

        id = "T2";
        double r1 = 0.0016 / zpu;
        double x1 = 0.091 / zpu;
        double b1 = 0.39 * zpu;
        double g1 = 0.0;
        double ratedU1 = 400.0;
        double r2 = 0.0;
        double x2 = 0.0;
        double b2 = 0.0;
        double g2 = 0.0;
        double ratedU2 = 400.0;
        nodeId1 = "T";
        connected1 = true;
        nodeId2 = "2";
        connected2 = xfmr2T2Connected;
        int pac1 = 0;
        int pac2 = 0;
        CgmesTransformer xfmr2 = new CgmesTransformer(
            id,
            new CgmesTransformerEnd(
                1,
                ratedU1,
                r1, x1,
                b1, g1,
                pac1,
                nodeId1, connected1,
                CgmesRatioTapChanger.EMPTY, CgmesPhaseTapChanger.EMPTY),
            new CgmesTransformerEnd(
                2,
                ratedU2,
                r2, x2,
                b2, g2,
                pac2,
                nodeId2, connected2,
                CgmesRatioTapChanger.EMPTY, CgmesPhaseTapChanger.EMPTY));

        equipmentsAtNodes.computeIfAbsent(nodeId1, z -> new ArrayList<>()).add(id);
        if (xfmr2T2Connected) {
            equipmentsAtNodes.computeIfAbsent(nodeId2, z -> new ArrayList<>()).add(id);
        }
        transformerParameters.put(id, xfmr2);

        CgmesModelForInterpretation model = new CgmesModelForInterpretation(
            "Xfmr2-T2.connected=" + xfmr2T2Connected,
            nodes,
            lines,
            transformerParameters,
            equipmentsAtNodes);

        return model;
    }

    public static CgmesModelForInterpretation modelWithXfmr3(boolean xfmr3T2Connected) {
        double sbase = 100.0;
        double vbase = 400.0;
        // Impedances are expressed in per-unit values,
        // we convert to engineering units when adding elements to IIDM
        double zpu = sbase / Math.pow(vbase, 2);

        Map<String, CgmesNode> nodeParameters = buildNodes(vbase);
        Map<String, CgmesLine> lineParameters = buildLines(sbase, vbase);
        Map<String, List<String>> equipmentAtNode = equipmentAtNodes(lineParameters);

        Map<String, CgmesTransformer> transformers = new HashMap<>();

        String id = "123";

        double r1 = 0.0016 / zpu;
        double x1 = 0.091 / zpu;
        double b1 = 0.39 * zpu;
        double g1 = 0.0;
        double ratedU1 = 400.0;
        double r2 = 0.0;
        double x2 = 0.0;
        double b2 = 0.0;
        double g2 = 0.0;
        double ratedU2 = 400.0;
        double r3 = 0.0;
        double x3 = 0.0;
        double b3 = 0.0;
        double g3 = 0.0;
        double ratedU3 = 400.0;

        String nodeId1 = "1";
        boolean connected1 = true;
        String nodeId2 = "2";
        boolean connected2 = xfmr3T2Connected;
        String nodeId3 = "3";
        boolean connected3 = true;

        int pac1 = 0;
        int pac2 = 0;
        int pac3 = 0;
        CgmesTransformer xfmr3 = new CgmesTransformer(
            id,
            new CgmesTransformerEnd(
                1,
                ratedU1,
                r1, x1,
                b1, g1,
                pac1,
                nodeId1, connected1,
                CgmesRatioTapChanger.EMPTY, CgmesPhaseTapChanger.EMPTY),
            new CgmesTransformerEnd(
                2,
                ratedU2,
                r2, x2,
                b2, g2,
                pac2,
                nodeId2, connected2,
                CgmesRatioTapChanger.EMPTY, CgmesPhaseTapChanger.EMPTY),
            new CgmesTransformerEnd(
                3,
                ratedU3,
                r3, x3,
                b3, g3,
                pac3,
                nodeId3, connected3,
                CgmesRatioTapChanger.EMPTY, CgmesPhaseTapChanger.EMPTY));

        ///////
        transformers.put(id, xfmr3);
        equipmentAtNode.computeIfAbsent(nodeId1, z -> new ArrayList<>()).add(id);
        if (connected2) {
            equipmentAtNode.computeIfAbsent(nodeId2, z -> new ArrayList<>()).add(id);
        }
        equipmentAtNode.computeIfAbsent(nodeId3, z -> new ArrayList<>()).add(id);

        CgmesModelForInterpretation model = new CgmesModelForInterpretation(
            "Xfmr3-T2.connected=" + xfmr3T2Connected,
            nodeParameters,
            lineParameters,
            transformers,
            equipmentAtNode);

        return model;
    }
}
