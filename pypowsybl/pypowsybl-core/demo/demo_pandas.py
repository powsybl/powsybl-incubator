# Copyright (c) 2019, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

# Note: this code requires pandas (pip install pandas)

import pypowsybl_core

import jpype
import jpype.imports
from jpype.types import*


jpype.startJVM(jpype.getDefaultJVMPath(), "-ea", convertStrings=False)

# imports powsybl classes (java)
from java.nio.file import Path, Paths
from com.powsybl.loadflow import LoadFlow, LoadFlowResult, LoadFlowParameters
from com.powsybl.iidm.export import Exporters
from java.io import File
pimport = jpype.JPackage('com.powsybl.iidm.import_')

#simple dump network flows function
import pandas as pd
pd.set_option('display.max_columns', None)
pd.set_option('display.width', 200)
def dumpLinesFlowPandas(network, headline):
    print("\n{}".format(headline))
    lines = network.getLines()
    linesData=[]
    for line in lines:
        linesData.append([str(line.getId()), str(line.getName()), line.getR(), line.getX(), line.getB1(), line.getG1(), line.getB2(), line.getG2(), line.getTerminal1().getI(), line.getTerminal2().getI()])
    idx = [x[0] for x in linesData]
    data = [x[1:] for x in linesData]
    df1 = pd.DataFrame(data, columns =['name', 'R', 'X', 'B1', 'G1', 'B2', 'G2', 'terminal1.I', 'terminal2.I'], index = idx)
    return df1


xiidmExample = "./eurostag-tutorial-example1.xml"

# load a simple test network
print("\nLoading network file {}".format(xiidmExample))
network = pimport.Importers.loadNetwork(Paths.get(xiidmExample))
print(dumpLinesFlowPandas(network, "Lines' data and flow for network: {} - PRE-LOADFLOW".format(network.getId())))

# run a loadflow
# no references to any LF implementation is defined here, the LF to use is either a default one or declared in the configuration
print("\nRunning a LF, network {}".format(network.getId()))
result = LoadFlow.run(network, LoadFlowParameters.load())
print("Loadflow result = isOK: {}, metrics: {}\n".format(result.isOk(), result.getMetrics()))

# dump the network's lines flow, after running a loadflow
print(dumpLinesFlowPandas(network, "Lines' data and flow for network: {} - POST-LOADFLOW".format(network.getId())))

##
# Shuts down the JVM.
# Note: due to JPype's limitations, it is not possible to restart the JVM after being terminated
jpype.shutdownJVM()


