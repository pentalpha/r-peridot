![Project Logo](http://www.bioinformatics-brazil.org/r-peridot/img/logo1-no_background-black.png)

## This project, which only has a command line interface, is the foundation for [R-Peridot-GUI](https://github.com/pentalpha/r-peridot-gui)

# R-Peridot
Software for intuitively doing Differential Gene Expression (DGE) analysis on Windows and GNU\Linux, based on R packages.

[Project Page](http://www.bioinformatics-brazil.org/r-peridot)

## The Input
A plain text CSV/TSV table with genes/transcripts as rows and samples as columns, in which the values of the cells are read counts from RNA-Seq or Microarray experiments.

## The Idea
Abstracting the use of R packages by treating them as modules. Basically, the user only has to choose which modules he wants to use and R-Peridot takes care of the rest.

## What it can do
R-Peridot uses several Bioconductor packages for DGE analysis: DESeq, DESeq2, EBSeq, edgeR and sSeq. Each of these is use its own methodologies to find Differentially Expressed Genes (DEGs) among the ones in the input. 

The results of these is used to create differential expression consensus, with the intersection of all sets of DEGs. After that our software generates several graphs including Heat Maps, Dendrogram, PCA, Cluster Profiler and KEGG charts.

## Modules
The power of R-Peridot is only defined by its modules. Feel free to contribute with new modules that can create new results.

R-Peridot Modules Repository: [r-peridot-scripts](https://github.com/pentalpha/r-peridot-scripts)

## How to use

### First, clone this repository:

```sh
    $ git clone https://github.com/pentalpha/r-peridot.git
    $ cd r-peridot/
```

### Now, install the dependencies:
R-Peridot's main dependencies are:

- OpenJDK 1.8;
- R >= 3.4.1;

If you are using GNU\Linux, there are more dependencies. There are scripts to handle the dependency installing at some distros:

- For Ubuntu >= 17.04: jar/ubuntuDeps.sh;
- For Debian >= 9: jar/debianDeps.sh;
- For CentOS >= 7.2: jar/centosDeps.sh (recommended for other rpm-based distros);

### It's time to build
This project uses [Apache Ant](https://ant.apache.org/) as build system. To build our project, please install it.

```sh
    $ ant jar
```

That command will compile and package r-peridot. Now, download the modules from [r-peridot-scripts](https://github.com/pentalpha/r-peridot-scripts):

```sh
    $ cd jar
    $ git clone https://github.com/pentalpha/r-peridot-scripts
```

 Always make sure that the 'r-peridot-scripts' directory is at the same directory as 'r-peridot.jar'.

### Using it
R-Peridot can be executed by running the 'jar/r-peridot' script or the 'jar/r-peridot.jar' JAR file. You can move these to any place in your system, as long as you move them along with the 'r-peridot-scripts' directory.

When R-Peridot opens for the first time, it will ask you to choose a R environment. If that environment is missing packages, you have the option to install them (depending on where is your default R package library, you may need to run r-peridot with sudo)

To get help on using the command line interface:

```sh
    $ ./jar/r-peridot -h
```

-------------------------------------------------------------

## The following software was used to make R-Peridot

### OpenJDK 1.8
Available at: [http://openjdk.java.net/install/](http://openjdk.java.net/install/)

### R
Copyright (c) R Development Core Team, The R Foundation.
GNU GPL v2, full text: R-LICENSE.txt. 
[www.r-project.org](www.r-project.org)

### R-Peridot uses, and does not modifies in any form, the binaries of the following Java Libraries

#### Apache Commons: Commons IO and Commons Lang
Copyright (c)2017, The Apache Software Foundation.
Apache License Version 2.0, full text: APACHE-2-LICENSE.html
Available at: [http://commons.apache.org/](http://commons.apache.org/). Accessed on 17/08/2017;
