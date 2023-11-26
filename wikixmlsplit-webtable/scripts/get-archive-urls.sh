#!/usr/bin/env bash

curl https://web.archive.org/cdx/search/cdx -d url=$1 -d 'output=json' -d 'collapse=digest' -d 'limit=100' --create-dirs -o $2
