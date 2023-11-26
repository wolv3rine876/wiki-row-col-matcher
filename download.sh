#! /bin/bash

# This script downloads a set of 7z wiki dump files to tmp/dump_archives.
# 1. create a file containing all the archives you want with the following format for each line: "<sha1checksum>\t<archivename>".
#    Basically you can paste the checksums wikidump provides (e.g. https://dumps.wikimedia.org/enwiki/20230601/enwiki-20230601-sha1sums.txt)
# 2. To execute the script, pass it the file as first argument './download.sh myfile.txt'

# create folders
mkdir -p tmp/dump_archives;

while read line; do

  re="([a-z0-9]{40})\s\s((enwiki-([0-9]{8})-pages-meta-history[0-9]+.xml-[p0-9]+).7z)"
  if [[ $line =~ $re ]];
  then 
    target_hash=${BASH_REMATCH[1]}
    archive=${BASH_REMATCH[2]};
    target_file=${BASH_REMATCH[3]};
    dump_date=${BASH_REMATCH[4]};
    pages=${BASH_REMATCH[5]};

    if [ -e tmp/dump_archives/$archive ]
    then

      echo INFO: archive $archive already exists;
      continue;

    fi    

    # Download
    echo "========== Downloading $archive =========="
    wget -A".7z" -P tmp/dump_archives https://dumps.wikimedia.org/enwiki/${dump_date}/${archive}
    
    # Verify hash
    file_hash=$(sha1sum tmp/dump_archives/${archive} | head -c 40);
    if [ "$file_hash" != "$target_hash" ];
    then

      echo WARNING: Wrong checksum for file: $archive;
      rm -f $archive;
    
    fi
  else

    echo WARNING: Unknown file: $line;
  
  fi

done <$1