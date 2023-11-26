#! /bin/bash

# This script packages the output archives of process.sh into one big archive.

for file in $(find archives/*.7z)
do
  echo "======== packaging $file ========";

  re="(enwiki-([0-9]{8})-[p0-9]+-table-row-matching).7z"
  if [[ $file =~ $re ]];
  then 

    archive=${BASH_REMATCH[1]};
    dump_date=${BASH_REMATCH[2]};

    7z e $file -so | 7z a -t7z -y -si$archive.json -mx=9 enwiki-$dump_date-table-row-matching.7z;
  
  else
    
    echo "WARNING: Unknown file $file";
    
  fi
done

echo "======== DONE ========";