# Copyright (c) 2019, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

import pypowsybl_core

import jpype
import jpype.imports
from jpype.types import*


jpype.startJVM(jpype.getDefaultJVMPath(), "-ea", convertStrings=False)

# imports powsybl classes (java)
from java.util import Properties
from java.nio.file import Path, Paths
from com.powsybl.loadflow import LoadFlow, LoadFlowFactory, LoadFlowResult, LoadFlowParameters
from com.powsybl.commons.config import ComponentDefaultConfig
from com.powsybl.computation.local import LocalComputationManager
from com.powsybl.iidm.export import Exporters
from java.io import File
pimport = jpype.JPackage('com.powsybl.iidm.import_')

#simple dump network flows function
def dumpLinesFlow(network, headline):
    print("\n{}".format(headline))
    lines = network.getLines()
    print("line id;terminal1.I;terminal2.I")
    for line in lines:
        print("{};{};{}".format(line.getId(), line.getTerminal1().getI(), line.getTerminal2().getI()))

xiidmExample = "./eurostag-tutorial-example1.xml"

# load a simple test network
print("\nLoading network file {}".format(xiidmExample))
network = pimport.Importers.loadNetwork(Paths.get(xiidmExample))

# dump the network's lines flow, before running a loadflow
dumpLinesFlow(network,"Flow on lines for network: {} - PRE-LOADFLOW".format(network.getId()))

# run a loadflow
# no references to any LF implementation is defined here, the LF to use is declared in the configuration
print("\nRunning a LF, network {}".format(network.getId))
loadFlow = ComponentDefaultConfig.load().newFactoryImpl(LoadFlowFactory).create(network, LocalComputationManager.getDefault(), 0)
result = loadFlow.run(network.getVariantManager().getWorkingVariantId(), LoadFlowParameters.load()).join()
print("Loadflow result = isOK: {}, metrics: {}\n".format(result.isOk(), result.getMetrics()))

# dump the network's lines flow, after running a loadflow
dumpLinesFlow(network,"Flow on lines for network: {} - POST-LOADFLOW".format(network.getId()))

# save the post-flow network to a xiidm file
Exporters.export("XIIDM", network, None,  File('/tmp/export1').toPath())

##
# Shuts down the JVM.
# Note: due to JPype's limitations, it is not possible to restart the JVM after being terminated
jpype.shutdownJVM()


