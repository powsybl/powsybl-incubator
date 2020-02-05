# Copyright (c) 2019, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

name = "pypowsybl_single_line_diagram"

import os
import jpype
mypath = os.path.dirname(__file__)
POWSYBL_SINGLE_LINE_DIAGRAM_LIBRARIES_PATH = os.path.join(mypath, 'jars')
POWSYBL_SINGLE_LINE_DIAGRAM_LIBRARIES = "{}/*".format(POWSYBL_SINGLE_LINE_DIAGRAM_LIBRARIES_PATH)
jpype.addClassPath(POWSYBL_SINGLE_LINE_DIAGRAM_LIBRARIES)
