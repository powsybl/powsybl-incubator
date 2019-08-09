# Copyright (c) 2019, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

from __future__ import print_function
import glob
import os
import sys
from setuptools import setup, find_packages
import xml.etree.ElementTree as xml

def getVersionFromPom(pompath, elementtag):
    from xml.etree import ElementTree as et
    ns = "http://maven.apache.org/POM/4.0.0"
    et.register_namespace('', ns)
    tree = et.ElementTree()
    tree.parse(pompath)
    p = tree.getroot().findall("*//{%s}%s" % (ns, elementtag))
    if len(p) == 0:
        print("could not find element '%s' in file %s" % (elementtag, pompath), file=sys.stderr)
        sys.exit(-1)
    return p[0].text

# Provide guidance about how to use setup.py
incorrect_invocation_message = """
You must first build pypowsybl-core with maven: in pypowsybl-core directory, run
    mvn clean package
  Then, to build the python binary dist, run
    python setup.py bdist_wheel"""


PROJECT_HOME = os.path.abspath("../")
MODULE_HOME = os.path.abspath("./")

VERSION = getVersionFromPom(os.path.join(PROJECT_HOME, 'pom.xml'), "powsyblcore.version")

# Figure out where the jars are we need to package with PySpark.
JARS = "target/powsybl/share/java/"
JARS_PATH = glob.glob(os.path.join(MODULE_HOME, JARS))

if not os.path.exists(JARS):
    print(incorrect_invocation_message, file=sys.stderr)
    sys.exit(-1)



###########################################
with open("README.md", "r") as fh:
    long_description = fh.read()

setup(
    name="pypowsybl_core",
    version=VERSION,
    author="Christian Biasuzzi",
    author_email="christian.biasuzzi@techrain.eu",
    description="pypowsybl-core python integration",
    long_description=long_description,
    long_description_content_type="text/markdown",
    license="Mozilla Public License 2.0 (MPL 2.0)",
    url="https://github.com/powsybl/powsybl-core",
    python_requires='>3.0.0',
    install_requires=['JPype1'],
    include_package_data=True,
    packages=['pypowsybl_core',
              'pypowsybl_core.jars'],
    package_dir={
        'pypowsybl_core.jars': JARS
    },
    package_data={
        'pypowsybl_core.jars': ['*.jar']
    },
    classifiers=[
        "Development Status :: 4 - Beta",
        "License :: OSI Approved :: Mozilla Public License 2.0 (MPL 2.0)",
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: Implementation :: CPython"
    ],
)
