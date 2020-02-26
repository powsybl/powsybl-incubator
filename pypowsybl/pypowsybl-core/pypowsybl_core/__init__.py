# Copyright (c) 2019, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

name = "pypowsybl_core"
PLUGIN_PACKAGES_PREFIX = "pypowsybl_"


import os
import jpype
mypath = os.path.dirname(__file__)
POWSYBL_CORE_LIBRARIES_PATH = os.path.join(mypath, 'jars')
POWSYBL_CORE_LIBRARIES = "{}/*".format(POWSYBL_CORE_LIBRARIES_PATH)
jpype.addClassPath(POWSYBL_CORE_LIBRARIES)

import importlib
import pkgutil

pypowsybl_plugins = {
    name: importlib.import_module(name)
    for finder, name, ispkg
    in pkgutil.iter_modules()
    if name.startswith(PLUGIN_PACKAGES_PREFIX) and name != __name__
}

print("pypowsybl plugins: {}".format(pypowsybl_plugins))
for plug in pypowsybl_plugins:
	importlib.import_module(plug)