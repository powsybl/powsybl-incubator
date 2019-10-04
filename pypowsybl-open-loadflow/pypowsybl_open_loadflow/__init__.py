# Copyright (c) 2019, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

name = "pypowsybl_open_loadflow"

import os
import jpype
mypath = os.path.dirname(__file__)
POWSYBL_OPENLOADFLOW_LIBRARIES_PATH = os.path.join(mypath, 'jars')
POWSYBL_OPENLOADFLOW_LIBRARIES = "{}/*".format(POWSYBL_OPENLOADFLOW_LIBRARIES_PATH)
jpype.addClassPath(POWSYBL_OPENLOADFLOW_LIBRARIES)
