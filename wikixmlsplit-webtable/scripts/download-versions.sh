#!/usr/bin/env bash

input=$1

jq -r '.[] | [.[1],.[2]] | join(",")' $input | tail -n +2 | awk -F , -v input="$input" '{print "http://web.archive.org/web/" $1 "/" $2 " -o " input "-" NR ".html"}' | xargs curl
