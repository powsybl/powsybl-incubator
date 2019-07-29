# Copyright (c) 2019, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
#   file, You can obtain one at http://mozilla.org/MPL/2.0/.

# @author Christian Biasuzzi <christian.biasuzzi@techrain.eu>

#
# using powsybl java libraries from python
# via JPype's CPython-java bridge ( https://github.com/jpype-project/jpype , Apache-2.0 licensed)
#


# Import jpype module
import jpype

# Enable Java imports
import jpype.imports

# Pull in types
from jpype.types import*

import os

##
## POWSYBL_HOME must point to a powsybl-core installation's jars directory (e.g. /home/user1/powsybl/share/java)
if 'POWSYBL_HOME' in os.environ:
    powsyblHome = os.environ['POWSYBL_HOME']
    powsyblJars = "{}/share/java/*".format(powsyblHome)
else:
    print("please define the environment variable POWSYBL_HOME and make it point to a powsybl installation directory")
    exit(1)


powsyblConfigDir="."
powsyblConfigName="jpype"

xiidmExample = "./networksfiles/eurostag-tutorial-example1.xml"


# adds powsybl's jars to JVM classpath
jpype.addClassPath(powsyblJars)

######################
# Launch the JVM
jpype.startJVM(jpype.getDefaultJVMPath(),
               "-ea",
               "-Ditools.config.dir={}".format(powsyblConfigDir),
               "-Ditools.config.name={}".format(powsyblConfigName),
               convertStrings=False)


# prints the classpath, for debugging/reference
#from java.lang import System
#print(System.getProperty("java.class.path"))


#simple dump network flows function
def dumpLinesFlow(network, headline):
    print("\n{}".format(headline))
    lines = network.getLines()
    print("line id;terminal1.I;terminal2.I")
    for line in lines:
        print("{};{};{}".format(line.getId(), line.getTerminal1().getI(), line.getTerminal2().getI()))


#
# imports powsybl classes
from java.util import Properties
from java.nio.file import Path, Paths
from com.powsybl.loadflow import LoadFlow, LoadFlowFactory, LoadFlowResult, LoadFlowParameters
from com.powsybl.commons.config import ComponentDefaultConfig
from com.powsybl.computation.local import LocalComputationManager
from com.powsybl.iidm.export import Exporters
from java.io import File
## the underscore char in package's name breaks the import mechanism: let's use JPype's JPackage, instead
#  from com.powsybl.iidm.import_ import Importers, ImportConfig
pimport = jpype.JPackage('com.powsybl.iidm.import_')

# load the powsyb test configuration (jpype.yml) and instantiate a computation manager (default LocalCOmputationManager)
defaultConfig = ComponentDefaultConfig.load()
computationManager = LocalComputationManager.getDefault()

# load a simple test network
print("\nLoading network file {}".format(xiidmExample))
network = pimport.Importers.loadNetwork(Paths.get(xiidmExample),
                                         computationManager,
                                         pimport.ImportConfig(),
                                         Properties())

# dump the network's lines flow, before running a loadflow
dumpLinesFlow(network,"Flow on lines for network: {} - PRE-LOADFLOW".format(network.getId()))

# run a loadflow
# no references to any LF implementation is defined here, the LF to use is declared in the configuration
print("\nRunning a LF, network {}".format(network.getId))
loadFlow = defaultConfig.newFactoryImpl(LoadFlowFactory).create(network, computationManager, 0)
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
