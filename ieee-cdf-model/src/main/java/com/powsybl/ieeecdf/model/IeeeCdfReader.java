/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ieeecdf.model;

import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class IeeeCdfReader {

    enum IeeeCdfSection {
        BUS,
        BRANCH,
        LOSS_ZONES,
        INTERCHANGE_DATA,
        TIE_LINES
    }

    public IeeeCdfModel read(BufferedReader reader) throws IOException {
        String line = reader.readLine();

        FixedFormatManager manager = new FixedFormatManagerImpl();

        IeeeCdfTitle title = manager.load(IeeeCdfTitle.class, line);
        IeeeCdfModel model = new IeeeCdfModel(title);

        IeeeCdfSection section = null;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("BUS DATA FOLLOWS")) {
                section = IeeeCdfSection.BUS;
            } else if (line.startsWith("BRANCH DATA FOLLOWS")) {
                section = IeeeCdfSection.BRANCH;
            } else if (line.startsWith("LOSS ZONES FOLLOWS")) {
                section = IeeeCdfSection.LOSS_ZONES;
            } else if (line.startsWith("INTERCHANGE DATA FOLLOWS")) {
                section = IeeeCdfSection.INTERCHANGE_DATA;
            } else if (line.startsWith("TIE LINES FOLLOWS ")) {
                section = IeeeCdfSection.TIE_LINES;
            } else if (line.startsWith("-9")) {
                section = null;
            } else {
                if (section != null) {
                    readSection(line, manager, model, section);
                }
            }
        }

        return model;
    }

    private void readSection(String line, FixedFormatManager manager, IeeeCdfModel model, IeeeCdfSection section) {
        switch (section) {
            case BUS:
                model.getBuses().add(manager.load(IeeeCdfBus.class, line));
                break;
            case BRANCH:
                model.getBranches().add(manager.load(IeeeCdfBranch.class, line));
                break;
            case LOSS_ZONES:
                model.getLossZones().add(manager.load(IeeeCdfLossZone.class, line));
                break;
            case INTERCHANGE_DATA:
                model.getInterchangeData().add(manager.load(IeeeCdfInterchangeData.class, line));
                break;
            case TIE_LINES:
                model.getTieLines().add(manager.load(IeeeCdfTieLine.class, line));
                break;
            default:
                throw new AssertionError();
        }
    }
}
