#!/bin/sh

. /etc/profile

cd /home/marko/bdval/data
ant -Dsave-data-dir=$TMPDIR/foo -Dsave-data-tag=foo -Dtag-description="hi mom" -f prostate-example.xml
