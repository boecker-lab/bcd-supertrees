The *GSCM Project* is a *java* library and command line tool providing the
greedy strict consensus merger supertree algorithm for rooted input trees.
It provides several scoring functions to determine in which oder the input trees get merged. 
Combining different scorings is also implemented as well as a randomized version of the algorithm. 
For more detailed information about the algorithm see the Literature.

### Literature

[1] Markus Fleischauer and Sebastian Böcker,
**Collecting reliable clades using the Greedy Strict Consensus Merger.**
_PeerJ (2016) 4:e2172_ https://doi.org/10.7717/peerj.2172

[2] Markus Fleischauer and Sebastian Böcker,
**Collecting reliable clades using Greedy Strict Consensus Merger.**
_Proc. of German Conference on Bioinformatics (GCB 2015)_, volume 3 of PeerJ PrePrints, pages e1595. PeerJ Inc. San Francisco, USA, 2015.


Download Links
============
GSCM commandline tool v1.0.1
* for [Windows](https://bio.informatik.uni-jena.de/repository/dist/de/unijena/bioinf/phylo/gscm-cli/gscm-cli-1.0.1-Win.zip)
* for [Linux/Unix](https://bio.informatik.uni-jena.de/repository/dist/de/unijena/bioinf/phylo/gscm-cli/gscm-cli-1.0.1-Linux.zip)
* as [jar file](https://bio.informatik.uni-jena.de/repository/dist/de/unijena/bioinf/phylo/gscm-cli/gscm-cli-1.0.1.zip)

GSCM commandline tool v1.0
* for [Windows](https://bio.informatik.uni-jena.de/repository/dist/de/unijena/bioinf/phylo/gscm-cli/gscm-cli-1.0-Win.zip)
* for [Linux/Unix](https://bio.informatik.uni-jena.de/repository/dist/de/unijena/bioinf/phylo/gscm-cli/gscm-cli-1.0-Linux.zip)
* as [jar file](https://bio.informatik.uni-jena.de/repository/dist/de/unijena/bioinf/phylo/gscm-cli/gscm-cli-1.0.zip)

Installation
============

Windows
-------
The gscm.exe should hopefully work out of
the box. To execute GSCM from every location you have to add the
location of the `gscm.exe` to your **PATH** environment variable.


Linux and MacOSX
----------------
To execute GSCM from every location you have to add the location of
the gscm executable to your **PATH** variable. Open the file `~/.bashrc`
in an editor and add the following line (replacing the placeholder path):

	export PATH-$PATH:/path/to/gscm

Jar (any OS)
----------------
Alternatively, you can run the gscm.jar jar file using java with the
command:

	java -jar /path/to/gscm.jar


Using GSCM command line tool
============================
You can always use the `--help` option to get a documentation about
the available commands and options.

Generally you only need to specify the input trees as input.
Other options are listet below or be see via `--help` option

Supported Filtypes
------------------
The GSCM command line tool handles trees in **NEWICK** and **NEXUS** format.
For an automatic file format detection use the common file extension
for **NEWICK** `(tree|TREE|tre|TRE|phy|PHY|nwk|NWK)` and **NEXUS** `(nex|NEX|ne|NE|nexus|NEXUS)`.
Per default the output tree format equals the input format. To specify a different
output format you can use the option `--outFileType` or the short form `-d`.

Supported Commands
==================

Usage:
------

```
gscm [options...] INPUT_TREES_FILE

INPUT_TREES_FILE                       		: Path of the file containing the input data
``` 

General options:
----------------

```
 -H (--HELP)                            	: Full usage message including
                                                  nonofficial Options (default: false)
 -O (--fullOutput) PATH                 	: Appends the unmerged trees of all
                                                  scorers and random iterations to the
                                                  output file
 -R (--randomIterations) N              	: Enables randomization and specifies
                                                  the number of iterations per scoring
 -V (--VERBOSE)                         	: many console output
 -d (--outFileType) [NEXUS | NEWICK | AUTO]  	: Output file type (default: AUTO)
                                     
 -f (--fileType) [NEXUS | NEWICK | AUTO]     	: Type of input files and if not
                                     	          specified otherwise also of the
                                                  output file (default: AUTO)
 -h (--help)                            	: usage message (default: false)
 -o (--outputPath) PATH                 	: Output file
 -p (--workingDir) PATH                 	: Path of the working directory. All
                                                  relative paths will be rooted here.
                                                  Absolute paths are not effected
 -r (--randomized)                      	: Enables randomization (standard
                                                  iterations are numberOfTrees^2 per scoring)
 -s (--scorer) [UNIQUE_TAXA |           	: set of scores that should be used.
 UNIQUE_TAXA_ORIG | OVERLAP |                     standard scm can use only one
 OVERLAP_ORIG | CLADE_NUMBER |                    (default: UNIQUE_CLADES_LOST)
 RESOLUTION | COLLISION_SUBTREES 
 | COLLISION_POINT | UNIQUE_CLADE_NUMBER
 | UNIQUE_CLADE_RATE | UNIQUE_CLADES_LOST 
 | UNIQUE_CLADES_REMAINING]
 
 -v (--verbose)                         	: some more console output
 -t (--threads) N                       	: Set a positive number of Threads that should be used
 -T (--singleThreaded)                  	: starts in single threaded mode, equal to "-t 1"
 -B (--disableProgressbar)              	: Disables progress bar (cluster/background mode)
```

GSCM Java Library
=================

You can integrate the GSCM library in your java project, either by
using Maven [1] or by including the jar file directly. The latter is
not recommended, as the GSCM jar contains also dependencies to other
external libraries.


Maven Integration
-----------------

Add the following repository to your pom file:

```xml
   <distributionManagement>
     <repository>
         <id>bioinf-jena</id>
         <name>bioinf-jena-releases</name>
         <url>https://bio.informatik.uni-jena.de/repository/libs-releases-local</url>
     </repository>
   </distributionManagement>
```

Now you can integrate GSCM in your project by adding the following
dependency:

Library containing all algorithms

```xml
   <dependency>
     <groupId>de.unijena.bioinf</groupId>
     <artifactId>gscm-lib</artifactId>
     <version>1.0.1</version>
   </dependency>
```

Whole project containing the algorithm (gscm-lib) and the command line interface (gscm-cli)

```xml
   <dependency>
     <groupId>de.unijena.bioinf</groupId>
     <artifactId>gscm</artifactId>
     <version>1.0.1</version>
   </dependency>
```

Main API
--------

The main class in the GSCM library is `de.unijena.bioinf.SCMAlgorithm`.
It specifies the main API of all provided algorithm implementation. To run a algorithm
just have to specify scorer(s) and input trees. 

There are currently 3 implemetations of `de.unijena.bioinf.gscm.algorithm.GSCMAlgorithm`:

### Algorithm Implemetation(s):

`public class GreedySCMAlgorithm`	

This class provides the basic greedy strict consensus merger algorithm.
Parameters:
* **input** -- List of rooted input trees.
* **scorer** -- scoring function that should be used

Returns:
   The greedy strict consensus merger supertree


`public class MultiGreedySCMAlgorithm`

This class provides a greedy strict consensus merger algorithm that combines
the results of different scoring functions into one supertree. 

Parameters:
   * **input** -- List of rooted input trees.
   * **scorer** -- List of scoring functions that should be used

Returns:
   List of all generaated gscm supertrees
   The combined supertree


`public class RandomizedSCMAlgorithm`

This class provides a randomized greedy strict consensus merger algorithm that combines
the results of multiple radmomized runs. It handles also multiple scoring functions. 

Parameters:
   * **input** -- List of rooted input trees.
   * **scorer** -- List of scoring functions that should be used
   * **numberOfIterations** -- number of random iterations

Returns:
   List of all generaated gscm supertrees
   The combined supertree


### Scorer Implemetations:

The class TreeScorers is a factory class
that provides recommended scorers and scorer combinations:

```
  UNIQUE_TAXA,
  UNIQUE_TAXA_ORIG,
  OVERLAP,
  OVERLAP_ORIG,
  CLADE_NUMBER,
  RESOLUTION,
  COLLISION_SUBTREES,
  COLLISION_POINT,
  UNIQUE_CLADE_NUMBER,
  UNIQUE_CLADE_RATE,
  UNIQUE_CLADES_LOST,
  UNIQUE_CLADES_REMAINING
```

The in Fleischauer et al. presented scorings are:

```
  UNIQUE_TAXA_ORIG,
  OVERLAP_ORIG,
  RESOLUTION,
  COLLISION_SUBTREES,
  UNIQUE_CLADE_NUMBER,
  UNIQUE_CLADES_LOST,
```

Changelog
=========

1.0.1
   * less memory consumption
   * bug fix for simple/fast scorings such as Overlap -> large speed up

1.0
* release version