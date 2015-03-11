#!/bin/sh

filename=$1
extent=$2

gm mogrify -gravity center \
    -background transparent \
    -extent $extent \
    $filename
pngout $filename

