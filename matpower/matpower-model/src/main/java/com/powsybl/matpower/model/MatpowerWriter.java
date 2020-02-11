/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.matpower.model;

import com.univocity.parsers.common.processor.BeanWriterProcessor;
import com.univocity.parsers.tsv.TsvWriter;
import com.univocity.parsers.tsv.TsvWriterSettings;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Objects;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.eu>
 */
public class MatpowerWriter {

    private final MatpowerModel model;

    public MatpowerWriter(MatpowerModel model) {
        this.model = Objects.requireNonNull(model);
    }

    private static <T> void writeRecords(Writer writer, List<T> beans, Class<T> aClass) {
        TsvWriterSettings settings = new TsvWriterSettings();
        settings.getFormat().setLineSeparator(";\n");
        BeanWriterProcessor<T> processor = new BeanWriterProcessor<>(aClass);
        settings.setRowWriterProcessor(processor);
        new TsvWriter(writer, settings).processRecords(beans);
    }

    public void write(BufferedWriter writer) throws IOException {
        writer.write(String.format("function mpc = %s", model.getCaseName()));
        writer.newLine();

        writer.write(String.format("mpc.version = %s", model.getVersion()));
        writer.newLine();

        writer.write("%% system MVA base");
        writer.newLine();
        writer.write(String.format("mpc.baseMVA = %f", model.getBaseMva()));
        writer.newLine();

        writer.newLine();
        writer.write("%% bus data");
        writer.newLine();
        writer.write("%       bus_i   type    Pd      Qd      Gs      Bs      area    Vm      Va      baseKV  zone    Vmax    Vmin");
        writer.newLine();
        writer.write("mpc.bus = [");
        writer.newLine();
        writeRecords(writer, model.getBuses(), MBus.class);
        writer.write(String.format("];"));
        writer.newLine();

        writer.write("%% generator data");
        writer.newLine();
        writer.write(" %       bus     Pg      Qg      Qmax    Qmin    Vg      mBase   status  Pmax    Pmin    Pc1     Pc2     Qc1min  Qc1max  Qc2min  Qc2max  ramp_agc        ramp_10 ramp_30 ramp_q  apf");
        writer.newLine();
        writer.write("mpc.gen = [");
        writer.newLine();
        writeRecords(writer, model.getGenerators(), MGen.class);
        writer.write(String.format("];"));
        writer.newLine();

        writer.newLine();
        writer.write("%% branch data");
        writer.newLine();
        writer.write("%       fbus    tbus    r       x       b       rateA   rateB   rateC   ratio   angle   status  angmin  angmax");
        writer.newLine();
        writer.write("mpc.branch = [");
        writer.newLine();
        writeRecords(writer, model.getBranches(), MBranch.class);
        writer.write(String.format("];"));
        writer.newLine();
    }
}
