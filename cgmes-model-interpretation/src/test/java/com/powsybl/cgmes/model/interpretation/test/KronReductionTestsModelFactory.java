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
        p = -39.104635;
        q = 192.513642;
        nodes.put(id, new CgmesNode(id, nominalVoltage, v, angle, p, q));

        id = "2";
        v = 1.00290856 * nominalVoltage;
        angle = Math.toDegrees(-0.01056277);
        p = 24.0;
        q = 50.0;
        nodes.put(id, new CgmesNode(id, nominalVoltage, v, angle, p, q));

        id = "3";
        v = 1.02660368 * nominalVoltage;
        angle = Math.toDegrees(-0.02365032);
        p = 13.0;
        q = 5.0;
        nodes.put(id, new CgmesNode(id, nominalVoltage, v, angle, p, q));

        id = "4";
        v = 1.02752795 * nominalVoltage;
        angle = Math.toDegrees(-0.01809537);
        p = 0.0;
        q = 0.0;
        nodes.put(id, new CgmesNode(id, nominalVoltage, v, angle, p, q));

        id = "5";
        v = 1.02034573 * nominalVoltage;
        angle = Math.toDegrees(-0.01258495);
        p = 0.0;
        q = 0.0;
        nodes.put(id, new CgmesNode(id, nominalVoltage, v, angle, p, q));

        id = "6";
        v = 1.02102662 * nominalVoltage;
        angle = Math.toDegrees(-0.01088041);
        p = 0.0;
        q = 0.0;
        nodes.put(id, new CgmesNode(id, nominalVoltage, v, angle, p, q));

        return nodes;
    }

    private static Map<String, CgmesLine> buildLines(double sbase, double vbase, boolean lineFrom2To5open, boolean lineFrom5To2open) {

        // Impedances are expressed in per-unit values,
        // we convert to engineering units when adding elements to IIDM
        double zpu = sbase / Math.pow(vbase, 2);

        Map<String, CgmesLine> lines = new HashMap<>();

        String id = "LineFromSlackTo2";
        double r = 0.0050 / zpu;
        double x = 0.0025 / zpu;
        double bch = 0.004 * zpu;
        double gch = 0;
        String nodeId1 = "Slack";
        boolean connected1 = true;
        String nodeId2 = "2";
        boolean connected2 = true;
        lines.put(id, new CgmesLine(id, r, x, bch, gch, nodeId1, connected1, nodeId2, connected2));

        id = "LineFrom2To5";
        r = 0.0075 / zpu;
        x = 0.064 / zpu;
        bch = 0.53 * zpu;
        gch = 0;
        nodeId1 = "2";
        connected1 = true;
        nodeId2 = "5";
        connected2 = !lineFrom2To5open;
        lines.put(id, new CgmesLine(id, r, x, bch, gch, nodeId1, connected1, nodeId2, connected2));

        id = "LineFrom5To2";
        r = 0.0075 / zpu;
        x = 0.065 / zpu;
        bch = 0.53 * zpu;
        gch = 0;
        nodeId1 = "5";
        connected1 = !lineFrom5To2open;
        nodeId2 = "2";
        connected2 = true;
        lines.put(id, new CgmesLine(id, r, x, bch, gch, nodeId1, connected1, nodeId2, connected2));

        return lines;
    }

    private static Map<String, CgmesTransformer> buildTransformers(double sbase, double vbase, boolean xfmr2From2To6open, boolean xfmr2From6To2open,
            boolean xfmr3open) {

        // Impedances are expressed in per-unit values,
        // we convert to engineering units when adding elements to IIDM
        double zpu = sbase / Math.pow(vbase, 2);

        Map<String, CgmesTransformer> transformers = new HashMap<>();

        String id = "Xfmr2From2To6";
        int endNumber = 1;
        double r = 0.0016 / zpu;
        double x = 0.091 / zpu;
        double b = 0.39 * zpu;
        double g = 0.0;
        double ratedU = 400.0;
        int phaseAngleClock = 0;
        String nodeId = "2";
        boolean connected = true;
        CgmesTransformerEnd end1 = new CgmesTransformerEnd(endNumber, ratedU, r, x, b, g, phaseAngleClock, nodeId, connected, CgmesRatioTapChanger.EMPTY, CgmesPhaseTapChanger.EMPTY);

        endNumber = 2;
        r = 0.0;
        x = 0.0;
        b = 0.0;
        g = 0.0;
        ratedU = 400.0;
        phaseAngleClock = 0;
        nodeId = "6";
        connected = !xfmr2From2To6open;
        CgmesTransformerEnd end2 = new CgmesTransformerEnd(endNumber, ratedU, r, x, b, g, phaseAngleClock, nodeId, connected, CgmesRatioTapChanger.EMPTY, CgmesPhaseTapChanger.EMPTY);
        transformers.put(id, new CgmesTransformer(id, end1, end2));

        id = "Xfmr2From6To2";
        endNumber = 1;
        r = 0.0016 / zpu;
        x = 0.091 / zpu;
        b = 0.39 * zpu;
        g = 0.0;
        ratedU = 400.0;
        phaseAngleClock = 0;
        nodeId = "6";
        connected = !xfmr2From2To6open;
        end1 = new CgmesTransformerEnd(endNumber, ratedU, r, x, b, g, phaseAngleClock, nodeId, connected, CgmesRatioTapChanger.EMPTY, CgmesPhaseTapChanger.EMPTY);

        endNumber = 2;
        r = 0.0;
        x = 0.0;
        b = 0.0;
        g = 0.0;
        ratedU = 400.0;
        phaseAngleClock = 0;
        nodeId = "2";
        connected = true;
        end2 = new CgmesTransformerEnd(endNumber, ratedU, r, x, b, g, phaseAngleClock, nodeId, connected, CgmesRatioTapChanger.EMPTY, CgmesPhaseTapChanger.EMPTY);
        transformers.put(id, new CgmesTransformer(id, end1, end2));

        id = "Xfmr3From2To3To4";
        endNumber = 1;
        r = 0.0025 / zpu;
        x = 0.050 / zpu;
        b = 0.25 * zpu;
        g = 0.0;
        ratedU = 400.0;
        phaseAngleClock = 0;
        nodeId = "2";
        connected = true;
        end1 = new CgmesTransformerEnd(endNumber, ratedU, r, x, b, g, phaseAngleClock, nodeId, connected, CgmesRatioTapChanger.EMPTY, CgmesPhaseTapChanger.EMPTY);

        endNumber = 2;
        r = 0.0020 / zpu;
        x = 0.045 / zpu;
        b = 0.20 * zpu;
        g = 0.0;
        ratedU = 400.0;
        phaseAngleClock = 0;
        nodeId = "3";
        connected = true;
        end2 = new CgmesTransformerEnd(endNumber, ratedU, r, x, b, g, phaseAngleClock, nodeId, connected, CgmesRatioTapChanger.EMPTY, CgmesPhaseTapChanger.EMPTY);

        endNumber = 3;
        r = 0.0015 / zpu;
        x = 0.040 / zpu;
        b = 0.15 * zpu;
        g = 0.0;
        ratedU = 400.0;
        phaseAngleClock = 0;
        nodeId = "4";
        connected = !xfmr3open;
        CgmesTransformerEnd end3 = new CgmesTransformerEnd(endNumber, ratedU, r, x, b, g, phaseAngleClock, nodeId, connected, CgmesRatioTapChanger.EMPTY, CgmesPhaseTapChanger.EMPTY);
        transformers.put(id, new CgmesTransformer(id, end1, end2, end3));

        return transformers;
    }

    private static Map<String, List<String>> equipmentAtNodes(
            Map<String, CgmesLine> lines, Map<String, CgmesTransformer> transformers) {
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

        transformers.keySet().forEach(id -> {
            CgmesTransformer transformer = transformers.get(id);
            String nodeId1 = transformer.end1().nodeId();
            boolean t1connected = transformer.end1().connected();
            if (t1connected) {
                List<String> idTransformers = equipmentAtNodes.computeIfAbsent(nodeId1, z -> new ArrayList<>());
                idTransformers.add(id);
            }
            String nodeId2 = transformer.end2().nodeId();
            boolean t2connected = transformer.end2().connected();
            if (t2connected) {
                List<String> idTransformers = equipmentAtNodes.computeIfAbsent(nodeId2, z -> new ArrayList<>());
                idTransformers.add(id);
            }
            if (transformer.end3() != null) {
                String nodeId3 = transformer.end3().nodeId();
                boolean t3connected = transformer.end3().connected();
                if (t3connected) {
                    List<String> idTransformers = equipmentAtNodes.computeIfAbsent(nodeId3, z -> new ArrayList<>());
                    idTransformers.add(id);
                }
            }
        });
        return equipmentAtNodes;
    }

    public static CgmesModelForInterpretation model(boolean lineFrom2To5open, boolean lineFrom5To2open,
            boolean xfmr2From2To6open, boolean xfmr2From6To2open, boolean xfmr3open) {
        double sbase = 100.0;
        double nominalVoltage = 400.0;

        Map<String, CgmesNode> nodes = buildNodes(nominalVoltage);
        Map<String, CgmesLine> lines = buildLines(sbase, nominalVoltage, lineFrom2To5open, lineFrom5To2open);
        Map<String, CgmesTransformer> transformers = buildTransformers(sbase, nominalVoltage, xfmr2From2To6open, xfmr2From6To2open, xfmr3open);
        Map<String, List<String>> equipmentAtNode = equipmentAtNodes(lines, transformers);

        CgmesModelForInterpretation model = new CgmesModelForInterpretation(
                "Line2To5.connected=" + lineFrom2To5open + ".Line5To2.connected=" + lineFrom5To2open + "Xfmr2To6.connected=" + xfmr2From2To6open + ".Xfmr6To2.connected=" + xfmr2From6To2open
                        + "Xfmr2To3To4.connected=" + xfmr3open,
                nodes,
                lines,
                transformers,
                equipmentAtNode);

        return model;
    }
}
