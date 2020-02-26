Copyright (c) 2020, RTE (http://www.rte-france.com)
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at http://mozilla.org/MPL/2.0/.

# pypowsybl

These packages use the python-java bridge [JPype](https://github.com/jpype-project/jpype) (Apache-2.0 licensed) to interface python and java -in the specifics [powsybl](http://www.powsybl.org/) and related libraries.

 
## Quick-start:

If behind a proxy, set https_proxy for pip install

To create an isolated virtual python v3.6 environment, on CentOS v7

    python3 -m venv penv
    . ./penv/bin/activate
    pip install --upgrade pip
    pip install wheel
    pip install numpy

To build pypowsybl packages 

    mvn clean package
    pip install ./pypowsybl-core/dist/pypowsybl_core-3.0.0-py3-none-any.whl
    pip install ./pypowsybl-open-loadflow/dist/pypowsybl_open_loadflow-0.1.0-py3-none-any.whl
    pip install ./pypowsybl-single-line-diagram/dist/pypowsybl_single_line_diagram-1.1.0-py3-none-any.whl

### Demo
The python code in demo/demo.py uses powsybl libraries to:

 * load a network from an XIIDM file
 * run a loadflow (using [powsybl-open-loadflow](https://github.com/powsybl/powsybl-open-loadflow) on this network
 * read and print flows on all the network's lines
 * save the resulting network to a new XIIDM file
 
To run it

    cd pypowsybl-core/demo
    python demo.py


demo/demo_pandas.py provides a similar demo and demostrates an integration with pandas libraries (pandas libraries are needed)

    cd pypowsybl-core/demo
    pip install pandas
    python demo_pandas.py


### Demo with [jupyter](https://jupyter.org/)
This demo shows the results of the loadflow and the network's single line diagram in a jupyter notebook.

To run it

    pip install jupyter jupyterlab 
    cd  ./pypowsybl-single-line-diagram/demo
    jupyter-lab  (this will open a browser with the jupyter web-GUI) 
    open with the jupyter web-GUI the DemoLoadflowAndGraph.ipynb
    run all notebook cells (menu /Run/Run All Cells)
   

Notes: 
* Jpype1 package is automatically installed as a pypowsybl-core dependency.
* JDK is expected to be available in the $PATH.
