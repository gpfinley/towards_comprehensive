# Will read in the data set from the Clinical Abbreviation Sense Inventory and write out:
#   An acronym expansions file
#   A sense ID mapping file
#   Cross-validation training and testing sets


import random, subprocess

INFILE = 'AnonymizedClinicalAbbreviationsAndAcronymsDataSet.txt'
EXPANSIONSFILE = 'acronymExpansions.txt'
SENSEIDSFILE = 'acronymSenseIds.txt'
# Change for other numbers of folds
K = 10
# If true, make the big sets the test sets
inverted = False

excludeSenses = ["UNSURED SENSE"]
excludeAcronyms = ["ITP"]


f = open(INFILE)
rawLines = [line for line in f if line.split('|')[0] not in excludeAcronyms and line.split('|')[1] not in excludeSenses]
f.close()

uniqueIds = {}
acronymExpansions = {}

for line in rawLines:
    acronym, sense = line.split('|')[0], line.split('|')[1]
    if sense == "GENERAL ENGLISH":
        sense = acronym.lower()
    if acronym not in acronymExpansions:
        acronymExpansions[acronym] = set()
    acronymExpansions[acronym].add(sense)
    if sense not in uniqueIds:
        uniqueIds[sense] = "SenseID" + str(len(uniqueIds))

expansionsFile = open(EXPANSIONSFILE, 'w')
for acronym in acronymExpansions:
    expansionsFile.write(acronym)
    for sense in acronymExpansions[acronym]:
        expansionsFile.write('|' + sense)
    expansionsFile.write('\n')
expansionsFile.close()
senseIdsFile = open(SENSEIDSFILE, 'w')
for sense in uniqueIds:
    senseIdsFile.write(uniqueIds[sense] + '|' + sense + '\n')
senseIdsFile.close()

random.shuffle(rawLines)
n = len(rawLines)
#print n

subprocess.call(['mkdir', 'CV_data'])
for k in range(K):
    testStart = int( round(k*(float(n)/K)) )
    #print testStart
    testEnd = int( round( (k+1) * (float(n)/K) ) )
    #print testEnd
    testLines = rawLines[testStart:testEnd]
    #print len(testLines)
    trainLines = rawLines[:testStart] + rawLines[testEnd:]

    if inverted:
        tempLines = testLines
        testLines = trainLines
        trainLines = tempLines
        tempLines = None

    wTest = open('CV_data/kfold_test_' + str(k) + '.txt', 'w')
    wTrain = open('CV_data/kfold_train_' + str(k) + '.txt', 'w')
    for testLine in testLines:
        wTest.write(testLine)
    wTest.close()
    for trainLine in trainLines:
        l = trainLine.split('|')
        acronym, sense, start, end, text = l[0], l[1], l[3], l[4], l[6]
        start = int(start)
        end = int(end)

        if sense == "GENERAL ENGLISH":
            sense = acronym.lower()

        if sense in uniqueIds:
            text = text[:start] + uniqueIds[sense] + text[end+1:]
        else:
            print sense + " not present!"
        wTrain.write(text + "\n")
    wTrain.close()
