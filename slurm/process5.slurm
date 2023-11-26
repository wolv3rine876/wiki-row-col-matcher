#!/bin/bash

#SBATCH -A naumann-mt2023isapitz
#SBATCH -p magic
#SBATCH -c 60
#SBATCH --mem=480G
#SBATCH --constraint="ARCH:X86"
#SBATCH --time 2-00:00:00

export MAX_JAVA_MEM_PER_TASK=28;
export MIN_JAVA_MEM_PER_TASK=16;

srun \
	--container-name=worker1 \
	--container-mount-home \
	--container-writable \
	--container-mounts=/hpi/fs00/share/fg-naumann/sebastian.apitz/src/:/home/project/ \
	--container-workdir=/home/project/data/ \
	/home/project/java_matcher/process.sh tmp/dump_archives/5/
