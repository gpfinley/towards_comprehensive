#!/bin/bash

wget http://conservancy.umn.edu/bitstream/handle/11299/137703/AnonymizedClinicalAbbreviationsAndAcronymsDataSet.txt 

python kfold_casi.py

mkdir models

for classifier in cos nb lr svm ri bsc
do
    for i in `seq 0 9`
    do
        java -jar towards_comprehensive.jar $classifier models/model$classifier$i.ser CV_data/kfold_train_$i.txt acronymExpansions.txt acronymSenseIds.txt
    done
    echo "Results for classifier $classifier"
    for i in `seq 0 9`
    do
    java -jar towards_comprehensive.jar test models/model$classifier$i.ser CV_data/kfold_test_$i.txt
    done
done
