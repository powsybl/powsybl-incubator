Copyright (c) 2019, RTE (http://www.rte-france.com)
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at http://mozilla.org/MPL/2.0/.

# pypowsybl-core

This package uses the python-java bridge [JPype](https://github.com/jpype-project/jpype) (Apache-2.0 licensed) to interface python and java -in the specifics [powsybl](http://www.powsybl.org/) libraries.

 
### Runtime requirements, build and installation:

If behind a proxy, set https_proxy for pip install

To create an isolated virtual python v3.6 environment, on CentOS v7

    python3 -m venv penv
    . ./penv/bin/activate
    pip install --upgrade pip
    pip install wheel

To build and install the pypowsybl-core package 

    cd pypowsybl-core
    mvn clean package
    pip install ./dist/pypowsybl_core-3.0.0_SNAPSHOT-py3-none-any.whl


Other pypowsybl packages can be created and installed in the same way

    cd PYPOWSYBL-PACKAGE
    mvn clean package
    pip install ./dist/PYPOWSYBL-PACKAGE.whl 

Notes: 
* the  .whl package is to be found in the *dist* directory, after a 'mvn clean package'
* 'mvn clean package' executes python, during its execution: make sure to run it from your virtual python environment (ref. above)
* Jpype1 package is automatically installed as a pypowsybl-core dependency.
* In version 0.7.1 of JPype1, JPype requires numpy but numpy is not automatically installed by JPype. You could have to do "pip install numpy"



### Demo
The python code in demo/demo.py uses powsybl libraries to:

 * load a network from an XIIDM file
 * run a loadflow on this file
 * read and print flows on all lines
 * save the resulting network to a new XIIDM file
 
To run it

    cd demo
    python demo.py

Demo/demo_pandas.py provides a similar demo and demostrates an integration with pandas libraries.

JDK is expected to be available in the $PATH.

A loadflow integration package (pypowsybl-hades2 or pypowsybl-open-loadflow) must be installed.
The corresponding loadflow must be configured for the demo to run: please ref. to powsybl's documentation to learn how to configure it (e.g. with a $HOME/.itools/config.yml file).



### To be investigated
 * performances/memory with multiple, large networks/data ? 
 * conflicts with other Python libraries ? 
