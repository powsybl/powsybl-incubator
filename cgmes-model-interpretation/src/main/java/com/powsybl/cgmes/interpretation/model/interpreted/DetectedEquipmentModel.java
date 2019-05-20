/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.interpretation.model.interpreted;

import java.util.ArrayList;
import java.util.List;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public class DetectedEquipmentModel {

    public DetectedEquipmentModel(DetectedBranchModel branchModel) {
        detectedBranchModels = new ArrayList<>();
        this.detectedBranchModels.add(branchModel);
        total = 0;
        calculated = 0;
        ok = 0;
    }

    public DetectedEquipmentModel(DetectedBranchModel branchModel1, DetectedBranchModel branchModel2,
        DetectedBranchModel branchModel3) {
        detectedBranchModels = new ArrayList<>();
        this.detectedBranchModels.add(branchModel1);
        this.detectedBranchModels.add(branchModel2);
        this.detectedBranchModels.add(branchModel3);
        total = 0;
        calculated = 0;
        ok = 0;
    }

    public DetectedEquipmentModel(List<DetectedBranchModel> detectedBranchModels) {
        this.detectedBranchModels = detectedBranchModels;
        total = 0;
        calculated = 0;
        ok = 0;
    }

    public String code() {
        if (detectedBranchModels.size() == 1) {
            return detectedBranchModels.get(0).code();
        }
        StringBuilder code = new StringBuilder();
        code.append(detectedBranchModels.get(0).code());
        code.append(".");
        code.append(detectedBranchModels.get(1).code());
        code.append(".");
        code.append(detectedBranchModels.get(2).code());
        return code.toString();
    }

    public String conversionCode() {
        String detectedEquipmentCode = code();
        if (detectedEquipmentCode.length() == 6) {
            // The code corresponds to a Xfmr2
            return conversionCodeXfmr2(detectedEquipmentCode);
        } else if (detectedEquipmentCode.length() == 20) {
            // The code corresponds to a Xfmr3, end1 always network side
            return conversionCodeXfmr3(detectedEquipmentCode);
        } else {
            // For Lines the conversionCode will always be "ok"
            return "ok";
        }
    }

    private static String encodeNotExact(String code, String part) {
        return code.replace(part, part + "(ko)");
    }

    private static String encodeExact(String code, String part) {
        return code.replace(part, part + "(C)");
    }

    private static boolean containsConversionMarks(String code) {
        return code.contains("(C)") || code.contains("(ko)");
    }

    private String conversionCodeXfmr2(String detectedEquipmentCode) {
        String code1 = detectedEquipmentCode.substring(0, 3);
        String code2 = detectedEquipmentCode.substring(3, 6);

        if (code1.contains("RP")) {
            code1 = encodeNotExact(code1, "P");
        }

        code2 = encodeNotExact(code2, "Y");

        if (code1.contains("R") || code1.contains("r") || code1.contains("n")) {
            code2 = encodeNotExact(code2, "R");
            code2 = encodeNotExact(code2, "r");
            code2 = encodeNotExact(code2, "n");
        } else {
            code2 = encodeExact(code2, "R");
            code2 = encodeExact(code2, "r");
            code2 = encodeExact(code2, "n");
        }

        if (code1.contains("P") || code1.contains("p") || code1.contains("m")) {
            code2 = encodeNotExact(code2, "P");
            code2 = encodeNotExact(code2, "p");
            code2 = encodeNotExact(code2, "m");
        } else {
            code2 = encodeExact(code2, "P");
            code2 = encodeExact(code2, "p");
            code2 = encodeExact(code2, "m");
        }

        code2 = encodeExact(code2, "x");

        String evalModel = code1 + code2;

        if (containsConversionMarks(evalModel)) {
            return evalModel;
        }
        return "ok";
    }

    private String conversionCodeXfmr3(String detectedEquipmentCode) {
        String model1 = detectedEquipmentCode.substring(0, 6);
        String model11 = model1.substring(0, 3);
        String model12 = model1.substring(3, 6);
        String model2 = detectedEquipmentCode.substring(7, 13);
        String model21 = model2.substring(0, 3);
        String model22 = model2.substring(3, 6);
        String model3 = detectedEquipmentCode.substring(14, 20);
        String model31 = model3.substring(0, 3);
        String model32 = model3.substring(3, 6);

        model11 = encodeNotExact(model11, "Y");
        model21 = encodeExact(model21, "Y");
        model22 = encodeNotExact(model22, "Y");
        model31 = encodeExact(model31, "Y");
        model32 = encodeNotExact(model32, "Y");

        model11 = encodeNotExact(model11, "P");
        model11 = encodeNotExact(model11, "p");
        model11 = encodeNotExact(model11, "m");
        model12 = encodeNotExact(model12, "P");
        model12 = encodeNotExact(model12, "p");
        model12 = encodeNotExact(model12, "m");
        model21 = encodeNotExact(model21, "P");
        model21 = encodeNotExact(model21, "p");
        model21 = encodeNotExact(model21, "m");
        model22 = encodeNotExact(model22, "P");
        model22 = encodeNotExact(model22, "p");
        model22 = encodeNotExact(model22, "m");
        model31 = encodeNotExact(model31, "P");
        model31 = encodeNotExact(model31, "p");
        model31 = encodeNotExact(model31, "m");
        model32 = encodeNotExact(model32, "P");
        model32 = encodeNotExact(model32, "p");
        model32 = encodeNotExact(model32, "m");

        model22 = encodeExact(model22, "R");
        model22 = encodeExact(model22, "r");
        model22 = encodeExact(model22, "n");
        model32 = encodeExact(model32, "R");
        model32 = encodeExact(model32, "r");
        model32 = encodeExact(model32, "n");

        if ((model2.contains("R") || model2.contains("r") || model2.contains("n")) &&
            (model3.contains("R") || model3.contains("r") || model3.contains("n"))) {
            model11 = encodeNotExact(model11, "R");
            model11 = encodeNotExact(model11, "r");
            model11 = encodeNotExact(model11, "n");
            model12 = encodeNotExact(model12, "R");
            model12 = encodeNotExact(model12, "r");
            model12 = encodeNotExact(model12, "n");
        } else {
            model11 = encodeExact(model11, "R");
            model11 = encodeExact(model11, "r");
            model11 = encodeExact(model11, "n");
            model12 = encodeExact(model12, "R");
            model12 = encodeExact(model12, "r");
            model12 = encodeExact(model12, "n");
        }
        model11 = encodeExact(model11, "x");
        model12 = encodeExact(model12, "x");
        model22 = encodeExact(model22, "x");
        model32 = encodeExact(model32, "x");

        String evalModel = model11 + model12 + "." + model21 + model22 + "." + model31 + model32;
        if (containsConversionMarks(evalModel)) {
            return evalModel;
        }
        return "ok";
    }

    public List<DetectedBranchModel> detectedBranchModels() {
        return detectedBranchModels;
    }

    public int total() {
        return total;
    }

    public int calculated() {
        return calculated;
    }

    public int ok() {
        return ok;
    }

    public void incTotal(int n) {
        total += n;
    }

    public void incCalculated(int n) {
        calculated += n;
    }

    public void incOk(int n) {
        ok += n;
    }

    private final List<DetectedBranchModel> detectedBranchModels;
    private int total;
    private int calculated;
    private int ok;
}
