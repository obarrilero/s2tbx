# -*- coding: utf-8 -*-

import sys
import glob
import os
from ToolAdapterGenerator import run_ToolAdapterGenerator

if __name__ == '__main__':
    outputDirectory = os.path.join(os.path.dirname(__file__), '..')
    directoryContainingProcessingXML = os.path.join(os.path.dirname(__file__), 'xml')
    createAdapter = True
    listXmlFiles = glob.glob(os.path.join(directoryContainingProcessingXML, "*.xml"))
    print listXmlFiles
    for xmlProcessing in listXmlFiles:
        run_ToolAdapterGenerator(outputDirectory, xmlProcessing, createAdapter)
