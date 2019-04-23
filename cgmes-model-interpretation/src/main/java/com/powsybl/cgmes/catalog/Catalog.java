/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.catalog;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

import com.powsybl.cgmes.conversion.CgmesImport;
import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.CgmesModelFactory;
import com.powsybl.cgmes.model.triplestore.CgmesModelTripleStore;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.datasource.FileDataSource;
import com.powsybl.commons.datasource.ZipFileDataSource;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBags;
import com.powsybl.triplestore.api.TripleStoreFactory;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class Catalog {

    public Catalog(CatalogLocation location) {
        Objects.requireNonNull(location);
        this.location = location;
    }

    public CgmesModel cgmes(Path rpath) {
        String impl = TripleStoreFactory.defaultImplementation();
        if (location.boundary() == null) {
            return CgmesModelFactory.create(
                dataSource(location.dataRoot().resolve(rpath)),
                impl);
        } else {
            return CgmesModelFactory.create(
                dataSource(location.dataRoot().resolve(rpath)),
                dataSource(location.boundary()),
                impl);
        }
    }

    public Network convert(String rpath) {
        return convert(location.dataRoot().resolve(rpath), null, location.boundary());
    }

    public Network convert(String rpath, Properties params) {
        return convert(location.dataRoot().resolve(rpath), params);
    }

    public Network convert(Path path) {
        return convert(path, null, location.boundary());
    }

    public Network convert(Path path, Properties params) {
        return convert(path, params, location.boundary());
    }

    public Network convert(Path path, Properties params0, Path boundary) {
        Properties params = new Properties();
        if (params0 != null) {
            params.putAll(params0);
        }
        if (boundary != null) {
            params.put(CgmesImport.BOUNDARY_LOCATION, boundary.toString());
        }
        return Importers.importData("CGMES", dataSource(path), params);
    }

    public DataSource dataSource(Path path) {
        if (!path.toFile().exists()) {
            return null;
        }
        String spath = path.toString();
        if (path.toFile().isDirectory()) {
            String basename = spath.substring(spath.lastIndexOf('/') + 1);
            return new FileDataSource(path, basename);
        } else if (path.toFile().isFile() && spath.endsWith(".zip")) {
            return new ZipFileDataSource(path);
        }
        return null;
    }

    public String modelName(Path p) {
        // Identify the model using the portion of path relative to data root
        return p.subpath(location.dataRoot().getNameCount(), p.getNameCount()).toString();
    }

    public String tsoName(Path p) {
        return tsoNameFromPathname(p.toString());
    }

    public static String tsoNameFromPathname(String sp) {
        int i = sp.indexOf("_1D_") + 4;
        if (sp.indexOf("_1D_") == -1) {
            i = sp.indexOf("_2D_") + 4;
        }
        int j = sp.indexOf('_', i);
        if (sp.indexOf('_', i) > sp.indexOf('\\', i)) {
            j = sp.indexOf('\\', i);
        }
        if (j > i) {
            return sp.substring(i, j);
        } else {
            return sp.substring(i);
        }
    }

    public String country(Path p) {
        String sp = p.toString();
        int i = sp.lastIndexOf('_');
        return sp.substring(i + 1, i + 3);
    }

    protected String mas(CgmesModel cgmes) {
        PropertyBags models = ((CgmesModelTripleStore) cgmes).namedQuery("modelIds");
        return models.stream()
            .filter(m -> m.containsKey("modelingAuthoritySet"))
            .map(m -> m.get("modelingAuthoritySet"))
            .filter(mas -> !mas.contains("tscnet.eu"))
            .findFirst()
            .orElse("-");
    }

    protected final CatalogLocation location;
}
