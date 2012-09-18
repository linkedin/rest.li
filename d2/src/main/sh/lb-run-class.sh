#!/bin/sh

if [ $# -lt 1 ]; then
	echo $0 java-class-name [options]
	exit 1
fi

base_dir=$(dirname $0)
base_dir=`cd $base_dir; pwd`

for file in $base_dir/*.jar;
do
	CLASSPATH=$CLASSPATH:$file
done

export CLASSPATH
java -Dlog4j.configuration=file://$base_dir/log4j.xml -cp $CLASSPATH $@
