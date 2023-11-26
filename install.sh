#! /bin/bash

# This script installs the necessary packages using apt.

apt update;

echo "========= installing wget ========";
apt install -y wget;

echo "========= 7zip ========";
apt install -y p7zip-full;

echo "========= java jre ========";
apt install -y default-jre;

echo "========= GNU parallel ========";
apt install -y parallel;
