# Py-powsybl - how-to use the powsybl APIs from Python

This module uses the python-java bridge [JPype](https://github.com/jpype-project/jpype) (Apache-2.0 licensed) to interface python and java.
 
### Runtime requirements and installation:
JPype is a Python module that allows Python code to make use of Java libraries. The 'magic' is achieved through JNI (shared memory based approach) at the native level.

JPype documentation states that it has been tested with CPython 2.6 and later, including the 3 series and it has been used with Java versions from Java 1.7 to Java 11.


To install JPype libraries on CentOS v7, in an isolated virtual python v3.6 environment:

    python3 -m venv penv
    . ./penv/bin/activate
    pip install --upgrade pip
    pip install JPype1

Note: detailed jpype installation information in the guide, [here](https://jpype.readthedocs.io/en/latest/install.html) - other OSes are covered, too.    


### Demo
The python code in Demo1.py uses powsybl libraries to:

 * load a network from an XIIDM file
 * run a loadflow on this file
 * read and print flows on all lines
 * save the resulting network to a new XIIDM file
 

JDK is expected to be available in the $PATH.
The environment variable *POWSYBL_HOME* is expected to point to a powsybl-core distribution directory (i.e. a structure with a bin, share/java, etc. sub-directories, ref [powsybl-core installation](https://github.com/powsybl/powsybl-core)).
The demo is configured (ref. jpype.yml) to use RTE's [Hades2](https://github.com/rte-france/hades2-distribution), as the loadflow engine. Hades2 is expected to be already installed in $HOME/hades2LF.
Also, Hades2 java integration .jars are supposed to be in powsybl share/java directory (ref. [Hades2 Integration in powsybl](https://rte-france.github.io/hades2/index.html))

To execute Demo1.py:

    python ./Demo1.py


### To be investigated
 * performances/memory with multiple, large networks/data ? 
 * conflicts with other Python libraries ? 
