#!/bin/bash

serverProcessString=$1

pids=`ps -o pid,command ax | grep "$serverProcessString" | awk '!/awk/ && !/grep/ && !/bash/ {print $1}'`

if [ "${pids}" != "" ] || [ "${#pids[@]}" -ne 0 ]
then
    for pid in ${pids[@]}
    do
       echo "Killing process ${pid}"
       kill -9 ${pid}
    done
fi
