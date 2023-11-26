#!/bin/bash

#SBATCH -A naumann-mt2023isapitz
#SBATCH -p magic
#SBATCH --constraint="ARCH:X86"
#SBATCH -c 120
#SBATCH --mem=100G
#SBATCH --time 2-00:00:00

srun \
	--container-name=my-ubuntu \
	--container-mount-home \
	--container-writable \
	--container-mounts=/hpi/fs00/share/fg-naumann/sebastian.apitz/src/:/home/project/ \
	--container-workdir=/home/project/data/ \
	/home/project/java_matcher/package.sh
