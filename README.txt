I. INTRODUCTION

This archive contains Java code for the classifiers used in the experiments described in the 2016 AMIA Annual Symposium submission entitled 'Towards comprehensive clinical abbreviation disambiguation using machine-labeled training data'. The provided Java app (towards_comprehensive.jar) and source code (src) contain classes to train abbreviation disambiguation models and serialize them to .ser files, as well as to test serialized models.

Not included in this distribution are the data sources used. The paper describes two corpora:

    Corpus A is derivable from a publicly annotated data set by Moon et al. (2014), the Clinical Abbreviation Sense Inventory (CASI), available at: http://conservancy.umn.edu/handle/11299//137703

    Corpus B is derived from a clinical data repository and cannot currently be shared due to patient confidentiality concerns. We are currently looking into automated data de-identification measures that will enable us to share models trained on these data.



II. SIMPLE TEST CASE: cross-validation tests on publicly available data

Although Corpus B cannot be shared at this time, it is possible to replicate the cross-validation experiments on Corpus A as presented in the paper. (Note that performance statistics may differ very slightly from those reported in the paper, as a lemmatizer was used in those experiments but is not distributed here.) Running the Bash script provided:

./10-fold_train_test.sh

will download the necessary file, build 10-fold training and test sets, and report accuracy for all six included classifiers. (Results are sent to standard out.) This process takes about an hour for all classifiers (training the binary spatter code classifier is the slowest step).

As an alternative to running all tests as the script does, you can run any individual test (or train any individual model) with the following command syntax:

    (for training; java app takes five arguments)
java -jar towards_comprehensive.jar [type_of_classifier] [path_to_output_model] [path_to_training_data] [path_to_acronym_expansions_file] [path_to_sense_IDs_file]

    (for test; java app takes 'test' plus two other aguments)
java -jar towards_comprehensive.jar test [path_to_model] [path_to_test_data]

The following classifier types are available, as described in the paper:
    cos     vector space model with cosine distance metric
    nb      vector space model with Naive Bayes
    lr      vector space model with multinomial logistic regression
    svm     vector space model with support vector machines
    ri      random indexing
    bsc     binary spatter code



III. EXTENDING TO OTHER DATA

The user may also wish to try other data sources for training and/or test. This can be easily done provided that the data sets meet the following requirements:

Training:

    Text data must be presented in a single file, with all training examples replaced with a string unique for their sense. For example, all occurrences of 'ankle-brachial' (for the abbreviation 'AB') in the training data could be replaced with 'SenseID1', and all occurrences of 'blood group in ABO system' with 'SenseID2' (as in the data produced by the included Python script).

    Two other files must also be provided:

    1) A bar-delimited text file mapping the unique strings to the senses they represent. One sense per line. Continuing from the example above, this file would look like:

    SenseID1|ankle-brachial
    SenseID2|blood group in ABO system

    2) A bar-delimited text file mapping abbreviations to all their possible senses, with a bar between each sense. One abbreviation per line. For example:

    AB|ankle-brachial|blood group in ABO system

Test:

    Test samples should follow the format of the CASI data set, with bar-delimited lines of the format:

    abbreviation|sense|<unimportant>|index_of_first_character_of_abbr|index_of_last_character_of_abbr|<unimportant>|sample_text_containing_abbr


Training and testing on any data set can be accomplished using the command syntax given at the end of (II) above.

Building the source code from scratch will require downloading and linking the ICU4J library, available at: http://site.icu-project.org/download
