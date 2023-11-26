#! /bin/bash

# This script processes the downloaded 7z archives in parallel.
# 1. It extracts the archives
# 2. It extracts the wikipedia pages
# 3. It matches the tables within the articles.
# 4. It matches the rows within the matched tables
# 5. It zipps the results and places them in archive/.

# CAUTION: This script may run for days and needs a lot of memory to function corectly!

# Specify environment var 'MAX_JAVA_MEM_PER_TASK' (in GB) to give the different java stages more or less max. memory.
# Default is 8GB.

# Specify environment var 'MIN_JAVA_MEM_PER_TASK' (in GB) to give the different java stages more or less min. memory.
# Default is 4GB.

# create folders
echo "========== creating folders ==========";
mkdir -p archives;
mkdir -p tmp/dumps;
mkdir -p tmp/stage1;  # DirectParseMain
mkdir -p tmp/stage2;  # JsonOutputMain
mkdir -p tmp/stage3;  # RowMatch
mkdir -p tmp/stage4;  # subject column detection using TableMiner+

process_archive() {
  MAX_MEM="${MAX_JAVA_MEM_PER_TASK:-8}";
  MIN_MEM="${MIN_JAVA_MEM_PER_TASK:-4}";

  echo "========== Processing $1 =========="
  re="((enwiki-([0-9]{8})-pages-meta-history[0-9]+.xml-([p0-9]+)).7z)"
  if [[ $1 =~ $re ]];
  then 

    archive=${BASH_REMATCH[1]};
    target_file=${BASH_REMATCH[2]};
    dump_date=${BASH_REMATCH[3]};
    pages=${BASH_REMATCH[4]};

    result_file_name="enwiki-$dump_date-$pages-table-row-matching.7z"

    if [ -e archives/$result_file_name ]
    then

      echo INFO: archive $archive already exists;
      return;

    fi

    # unzip
    echo "========== unzipping $archive ==========";
    7z x -otmp/dumps -y $1;
    # rm $1;

    # java stage 1 (extraction)
    input=$PWD/tmp/dumps/$target_file;
    output=$PWD/tmp/stage1/$target_file;

    mkdir $output;
    echo "========== starting stage 1 (DirectParseMain) for $target_file ($MAX_MEM GB max. mem) ==========";
    java -Xms${MIN_MEM}G -Xmx${MAX_MEM}G -Djdk.xml.totalEntitySizeLimit=0 -DentityExpansionLimit=0 -cp wikixmlsplit-parser/target/wikixmlsplit-parser-0.0.1-SNAPSHOT-jar-with-dependencies.jar wikixmlsplit.parser.DirectParseMain -output $output -input $input -type TABLE -timeLimit 20;
    echo "Processed $(ls $output | wc -l) files"
    rm $input;

    # start java stage 2 (table matching)
    input=$output;
    output=$PWD/tmp/stage2/$target_file;

    mkdir $output;
    echo "========== starting stage 2 (JsonOutputTable) for $target_file ($MAX_MEM GB max. mem) ==========";
    java -Xms${MIN_MEM}G -Xmx${MAX_MEM}G -Djdk.xml.totalEntitySizeLimit=0 -DentityExpansionLimit=0 -cp wikixmlsplit-output/target/wikixmlsplit-output-0.0.1-SNAPSHOT-jar-with-dependencies.jar wikixmlsplit.output.json.JsonOutputTable -output $output -input $input;
    rm -r $input;

    # start java stage 3 (row matching)
    input=$output;
    output=$PWD/tmp/stage3/$target_file;

    mkdir $output;
    echo "========== starting stage 3 (JsonOutputRowMatcher) for $target_file ($MAX_MEM GB max. mem) ==========";
    java -Xms${MIN_MEM}G -Xmx${MAX_MEM}G -Djdk.xml.totalEntitySizeLimit=0 -DentityExpansionLimit=0 -cp wikixmlsplit-output/target/wikixmlsplit-output-0.0.1-SNAPSHOT-jar-with-dependencies.jar wikixmlsplit.output.json.JsonOutputRowMatcher -output $output -input $input;
    rm -r $input;

    # start java stage 4 (subject col detection)
    input=$output;
    output=$PWD/tmp/stage4/$target_file;

    mkdir $output;
    echo "========== starting stage 4 (subject column detextion) for $target_file ==========";
    java -cp tableminerplus/sti-main-1.0alpha-jar-with-dependencies.jar uk.ac.shef.dcs.sti.experiment.WikiTableMinerPlusBatch $input $output $PWD/tableminerplus/config/sti.properties
    rm -r $input;

    # zip results
    echo "========== zipping results  =========="
    7z a -t7z -y $PWD/archives/enwiki-$dump_date-$pages-table-row-matching.7z $output/*.json;
    rm -r $output;

  else

    echo WARNING: Unknown file: $1;
  
  fi

}

export -f process_archive;

# use only 25% of the available cpus in parallel, so that we have some resources left for threading within the tasks.
find $1*.7z | parallel -j 25% process_archive {};

echo "========= DONE ========";
