#!/usr/bin/env bash

echo 2000000 > /proc/sys/fs/nr_open
echo 2000000 > /proc/sys/fs/file-max
ulimit -n 2000000