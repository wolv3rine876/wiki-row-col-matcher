#!/usr/bin/env bash

jq '.url' $1 | sort | uniq -c | sort -nr
