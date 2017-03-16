*Bad Character Deletion* (BCD) Supertrees is a *java* library and command line tool providing the
bad character deletion supertree algorithm for rooted input trees.
It provides several strategies to weight the character that have to be removed during the micut phase.
Bad Character Deletion (BCD) Supertrees can use the *Greedy Strict Consensus Merger* (GSCM) algorithm
to preprocess the input trees. For the GSCM algorithm it provides several scoring functions to determine
in which oder the input trees get merged. Combining different scorings is also implemented as well as a
randomized version of the algorithm. For more detailed information about the algorithm see the Literature.

### Literature

[1] Markus Fleischauer and Sebastian Böcker,
**BCD Supertrees**
_under review_

[2] Markus Fleischauer and Sebastian Böcker,
**Collecting reliable clades using the Greedy Strict Consensus Merger**
_PeerJ (2016) 4:e2172_ https://doi.org/10.7717/peerj.2172.


Download Links
============
BCD Supertrees commandline tool v1.0
* for [Windows](https://bio.informatik.uni-jena.de/repository/dist-release-local/de/unijena/bioinf/phylo/bcd-cli/bcdSupertrees-1.0-Win.zip)
* for [Linux/Unix/Mac](https://bio.informatik.uni-jena.de/repository/dist-release-local/de/unijena/bioinf/phylo/bcd-cli/bcdSupertrees-1.0-Nix.zip)
* as [jar file](https://bio.informatik.uni-jena.de/repository/dist-release-local/de/unijena/bioinf/phylo/bcd-cli/bcdSupertrees-1.0-Jar.zip)

Installation
============

Windows
-------

The bcdSupertrees.exe should hopefully work out of
the box. To execute BCD from every location you have to add the
location of the *bcdSupertrees.exe* to your **PATH** environment variable.


Linux and MacOSX
----------------
To start BCD Supertrees you just have to start the *bcd* start script from the command line:

    `/path/to/bcd/bcdVM LIST_OF_BCD_ARGS`

To execute BCD Supertrees from every location you have to add the location of
the BCD start script to your **PATH** variable. Open the file `~/.profile`
in an editor and add the following line (replacing the placeholder
path):

   `export PATH-$PATH:/path/to/bcd`
   
The BCD Supertrees directory contains another start script named bcdVM. This script allows you to run 
BCD Supertrees with specific JVM (Java Virtual Machine) arguments.
The command to run the bcdVM start script type:
    `/path/to/bcd/bcdVM "LIST_OF_JVM_ARGS" "LIST_OF_BCD_ARGS"`


Jar (any OS)
----------------

Alternatively, you can run the jar file using java with the
command:

   `java -jar /path/to/bcdSupertrees/bcdSupertrees.jar`

You can always use the "--help" option to get a documentation about
the available commands and options.


Using the BCD Supertrees command line tool
============================

Generally you only need to specify the input trees as input.
If your input data contains bootstrap values we recommend the *BOOTSTRAP_VALUES*
weighting

Supported Filtypes
------------------

The BCD Supertrees command line tool handles trees in **NEWICK** and **NEXUS** format.
For an automatic file format detection use the common file extension
for **NEWICK** *(tree|TREE|tre|TRE|phy|PHY|nwk|NWK)* and **NEXUS** *(nex|NEX|ne|NE|nexus|NEXUS)*.
Per default the output tree format equals the input format. To specify a different
output format you can use the option `--outFileType` or the short form`-d`.

Supported Commands
==================

Usage:
------
```
bcd [options...] INPUT_TREE_FILE
    The only required argument is the input tree file in newick format

bcd [options...] INPUT_TREE_FILE GUIDE_TREE_FILE
    Additionally, a guide tree can be specified. Otherwise SCM tree gets calculated as default guide tree

```

General options:
----------------
```
 General options:
  PATH                                   : Path of the file containing the input
                                           data
  PATH                                   : Path of the file containing the guide
                                           tree
  -H (--HELP)                            : Full usage message including
                                           nonofficial Options (default: false)
  -O (--fullOutput) PATH                 : Output file containing full output
  -V (--VERBOSE)                         : many console output
  -b (--bootstrapThreshold) N            : Minimal bootstrap value of a
                                           tree-node to be considered during the
                                           supertree calculation (default: 0)
  -d (--outFileType) [NEXUS | NEWICK |   : Output file type (default: AUTO)
  AUTO]
  -f (--fileType) [NEXUS | NEWICK |      : Type of input files and if not
  AUTO]                                    specified otherwise also of the
                                           output file (default: AUTO)
  -h (--help)                            : usage message (default: false)
  -o (--outputPath) PATH                 : Output file
  -p (--workingDir) PATH                 : Path of the working directory. All
                                           relative paths will be rooted here.
                                           Absolute paths are not effected
  -s (--scm) VALUE                       : Use SCM-tree as guide tree (default:
                                           true)
  -v (--verbose)                         : some more console output
  -w (--weighting) [UNIT_WEIGHT |        : Weighting strategy
  TREE_WEIGHT | BRANCH_LENGTH |
  BOOTSTRAP_VALUES | LEVEL |
  BRANCH_AND_LEVEL | BOOTSTRAP_AND_LEVEL
  ]
  -t (--threads) N                       : Set a positive number of Threads that
                                           should be used
  -T (--singleThreaded)                  : starts in single threaded mode, equal
                                           to "-t 1"
  -B (--disableProgressbar)              : Disables progress bar (cluster/backgro
                                           und mode)


 Example:
 bcd  -H (--HELP) -O (--fullOutput) PATH -V (--VERBOSE) -b (--bootstrapThreshold) N
    -d (--outFileType) [NEXUS | NEWICK | AUTO] -f (--fileType) [NEXUS | NEWICK | AUTO]
    -h (--help) -o (--outputPath) PATH -p (--workingDir) PATH -s (--scm) VALUE -v (--verbose)
    -w (--weighting) [UNIT_WEIGHT | TREE_WEIGHT | BRANCH_LENGTH | BOOTSTRAP_VALUES | LEVEL | BRANCH_AND_LEVEL | BOOTSTRAP_AND_LEVEL]
    -t (--threads) N -T (--singleThreaded) -B (--disableProgressbar)
```

BCD Java Library
=================

You can integrate the BCD library in your java project, either by
using Maven [1] or by including the jar file directly. The latter is
not recommended, as the BCD jar contains also dependencies to other
external libraries.


Maven Integration
-----------------

Add the following repository to your pom file:

```xml
   <distributionManagement>
     <repository>
         <id>bioinf-jena</id>
         <name>bioinf-jena-releases</name>
         <url>http://bio.informatik.uni-jena.de/repository/libs-releases-local</url>
     </repository>
   </distributionManagement>
```
Now you can integrate BCD in your project by adding the following
dependency:

Library containing all algorithms
```xml
   <dependency>
     <groupId>de.unijena.bioinf.phylo</groupId>
     <artifactId>flipcut-lib</artifactId>
     <version>1.0</version>
   </dependency>
```
Whole project containing the algorithm (bcd-lib) and the command line interface (bcd-cli)
```xml
   <dependency>
     <groupId>de.unijena.bioinf.phylo</groupId>
     <artifactId>flipcut</artifactId>
     <version>1.0</version>
   </dependency>
```

Main API (WIP)
--------

The main class in the BCD library is *phylo.tree.algorithm.flipcut.AbstractFlipCut*.
It specifies the main API of all provided algorithm implementation. To run the algorithm you
just have to specify the input trees.

There are currently 1 implementation of *phylo.tree.algorithm.flipcut.AbstractFlipCut*:

### Algorithm Implemetation(s):

```phylo.tree.algorithm.flipcut.FlipCutSingleCutSimpleWeight```

   This class provides the basic Bad Character Deletion algorithm.

   Parameters:
   * **input** -- List of rooted input trees.
   * **weight** -- character weighting to use

   Returns:
          The bcd supertree


### Character Weightings:

The interface *phylo.tree.algorithm.flipcut.costComputer.FlipCutWeights* provides
different weightings. The package *phylo.tree.algorithm.flipcut.costComputer* contains implementations of these weightings.
They all implement the *FlipCutWeights* interface.

```
  UNIT_WEIGHT
  TREE_WEIGHT
  BRANCH_LENGTH
  BOOTSTRAP_VALUES
  LEVEL
  BRANCH_AND_LEVEL
  BOOTSTRAP_AND_LEVEL
```

The in Fleischauer et al. presented scorings are:
```
  UNIT_WEIGHT
  BRANCH_LENGTH
  BOOTSTRAP_VALUES
```

Changelog
=========

1.0
   * release version