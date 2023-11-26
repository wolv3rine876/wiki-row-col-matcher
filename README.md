# Row and column matcher

This repository extends the source-code of the paper [Structured Object Matching across Web Page Revisions](https://hpi.de/naumann/projects/data-profiling-and-analytics/change-exploration/Document/import_isg/Structured%20Object%20Matching%20Across%20Web%20Page%20Revisions.pdf/248c309432f5867ffdddaa8f9ed716a6.html?tx_extbibsonomycsl_publicationlist%5Baction%5D=download&cHash=0f9debd089867809ee41330de9e5b2d0) [1] and adds another stage to match not only tables, infoboxes and lists, but also the rows ans columns of a table.

## Summary

In short, the code and scripts allow to:

 1. download [dumps](https://dumps.wikimedia.org/) of the **englisch** Wikipedia automatically
 2. extract the histories of tables, infoboxes and lists across the different Wikipedia revisions
 3. **NEW**: extract the history for each row in a table history

## Caution

Running the code on a complete dump of the englisch Wikipedia requires a lot of time, processing power and storage capacity. Therefore, check the parent repository, which provides a fully processed dataset based on a [dump generated at the 1st of June 2023](https://dumps.wikimedia.org/enwiki/20230601/).
In addition, the here referenced Wikipedia dump might be unavailable at the time of reading. In that case, use a newer one **including the full Wikipedia history** from [here](https://dumps.wikimedia.org/).

## Prerequisites

To compile the source-code, [maven](https://maven.apache.org/) is needed (tested with version 3.6.3).

```shell
mvn package -P distro
```

The bash-script install.sh installs all necessary apt-packages to run the code (super-user rights might be required).

```shell
./install.sh
```

Furthermore, a file specifying all archives that compose the full wikipedia dump is needed. Each line should contain the sha1 checksum and the filename devided by a tab. [20230601_archives.txt](20230601_archives.txt) can be used for the  [dump generated at the 1st of June 2023](https://dumps.wikimedia.org/enwiki/20230601/). Alternatively, Wikipedia provides the archivenames and checksums in a file like [this](https://dumps.wikimedia.org/enwiki/20230601/enwiki-20230601-sha1sums.txt). The **.7z** files of interest can simply be pasted in a text file like [20230601_archives.txt](20230601_archives.txt).

## Running

Running the code is split into two steps. First, the archives are downloaded. Second, the matching is done.
All commands have to be **run from the root of this repository.**

### Downloading

Wikipedia limits the number of connections per IP address. Therefore, the download can only be done sequentially. By executing download.sh and passing it the previously specified file, all archives are downloaded to tmp/dump_archives.

```shell
./download.sh 20230601_archives.txt
```

### Matching

This steps extracts the archives and does the matching in multiple stages.

 1. Extract dumps to tmp/dumps.
 2. Extract Wikipedia pages to tmp/stage1. This corresponds to ```wikixmlsplit.parser.DirectParseMain``` in the original source code.
 3. Match tables across revisions and put them in tmp/stage2. This corresponds to ```wikixmlsplit.output.JsonOutputTable``` in the original source code.
 4. Match rows and columns for each matched table and put them in tmp/stage3
 5. Detect the subject column using TableMiner+ [2]
 6. Zip the results and store them in archives/

Set the environment variables MAX_JAVA_MEM_PER_TASK and MIN_JAVA_MEM_PER_TASK to specify the corresponding memory (the more the better).

```shell
export MAX_JAVA_MEM_PER_TASK=20; # in GB
export MIN_JAVA_MEM_PER_TASK=12; # in GB
./process.sh
```

### Packaging (optional)

The output of matching are many small 7z archives. To combine this archives to one big .7z file, run the provided package.sh script.

```shell
./package.sh
```

# Bibliography

[1]     T. Bleifuß, L. Bornemann, D. V. Kalashnikov, F. Naumann, and D. Srivastava, “Structured object matching across web page revisions”, in Proceedings - International Conference on Data Engineering, vol. 2021-April, 2021. doi: 10 . 1109 / ICDE51399 . 2021 . 00115. [Online]. Available: [https://ieeexplore.ieee.org/abstract/document/9458804](https://ieeexplore.ieee.org/abstract/document/9458804)

[2]     Z. Zhang, “Effective and efficient Semantic Table Interpretation using
TableMiner+”, Semantic Web, vol. 8, no. 6, 2017, issn: 22104968. doi: 10.
3233/SW-160242. [Online]. Available: [https://content.iospress.com/articles/semantic-web/sw242](https://content.iospress.com/articles/semantic-web/sw242)