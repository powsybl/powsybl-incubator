/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ieeecdf.model;

import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class IeeeCdfWriter {

    private final IeeeCdfModel model;

    public IeeeCdfWriter(IeeeCdfModel model) {
        this.model = Objects.requireNonNull(model);
    }

    public void write(BufferedWriter writer) throws IOException {
        FixedFormatManager manager = new FixedFormatManagerImpl();

        writer.write(manager.export(model.getTitle()));
        writer.newLine();

        writer.write(String.format("BUS DATA FOLLOWS                            %d ITEMS", model.getBuses().size()));
        writer.newLine();
        for (IeeeCdfBus bus : model.getBuses()) {
            writer.write(manager.export(bus));
            writer.newLine();
        }
        writer.write("-999");
        writer.newLine();

        writer.write(String.format("BRANCH DATA FOLLOWS                         %d ITEMS", model.getBranches().size()));
        writer.newLine();
        for (IeeeCdfBranch branch : model.getBranches()) {
            writer.write(manager.export(branch));
            writer.newLine();
        }
        writer.write("-999");
        writer.newLine();

        writer.write(String.format("LOSS ZONES FOLLOWS                     %d ITEMS", model.getLossZones().size()));
        writer.newLine();
        for (IeeeCdfLossZone lossZone : model.getLossZones()) {
            writer.write(manager.export(lossZone));
            writer.newLine();
        }
        writer.write("-99");
        writer.newLine();

        writer.write(String.format("INTERCHANGE DATA FOLLOWS                 %d ITEMS", model.getInterchangeData().size()));
        writer.newLine();
        for (IeeeCdfInterchangeData interchangeData : model.getInterchangeData()) {
            writer.write(manager.export(interchangeData));
            writer.newLine();
        }
        writer.write("-9");
        writer.newLine();

        writer.write(String.format("TIE LINES FOLLOWS                     %d ITEMS", model.getTieLines().size()));
        writer.newLine();
        for (IeeeCdfTieLine tieLine : model.getTieLines()) {
            writer.write(manager.export(tieLine));
            writer.newLine();
        }
        writer.write("-999");
        writer.newLine();

        writer.write("END OF DATA");
        writer.newLine();
    }
}
