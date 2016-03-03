package acronym;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Simple class for performing gradient descent on a matrix of weights for logistic regression/maxent, svms, etc.
 * This class's 'trainOne' method needs to be called several times for each training example
 *
 * Works with DoubleVector objects
 *
 * Created by ANONYMOUS on 11/10/15.
 */
public class GradientDescentModel implements Serializable {

    // Parameters of the model
    private double theta[][];

    // Total number of features (will add one for the intercept)
    private final int numFeatures;

    // Number of class categories
    private final int numClasses;

    // "logistic" or "svm"
    private final String modelType;
    private final String SVM = "svm";
    private final String LOGISTIC = "logistic";

    // regularization term; leave at zero for no regularization
    // this term should already be adjusted for training set size (this = lambda/m in typical usage)
    private double lambda = 0;

    // Whether or not to use an intercept term (effectively a prior probability)
    private final boolean usingIntercept;

    // The learning rate. Set rather high here, assuming the use of this Class with word vectors or similar data
    private double alpha = 100;

    public GradientDescentModel(int numFeatures, int numClasses, String modelType) {
        this(numFeatures, numClasses, modelType, false);
    }

    public GradientDescentModel(int numFeatures, int numClasses, String modelType, boolean usingIntercept) {
        if(numClasses < 2) {
            System.out.println("Trying to train classifier with fewer than two classes!");
        }
        this.numClasses = numClasses;
        this.numFeatures = numFeatures + 1;
        this.usingIntercept = usingIntercept;
        this.modelType = modelType;
        theta = new double[numClasses][this.numFeatures];
    }

    public void setLearningRate(double learningRate) {
        alpha = learningRate;
    }

    public double getLearningRate() {
        return alpha;
    }

    public void setRegularization(double regularization) {
        lambda = regularization;
    }

    public double getRegularization() {
        return lambda;
    }

    public int classify(DoubleVector vector) {

        double maxScore = -Double.MAX_VALUE;
        int winningClass = -1;
        for(int c=0; c<numClasses; c++) {
            double score = linearHypothesis(c, vector);
            if(score > maxScore) {
                maxScore = score;
                winningClass = c;
            }
        }
        return winningClass;
    }

    /**
     * Will perform several passes on all data, training the model in one step
     * Data needs to be organized as a List<List<DoubleVector>>:
     *      numClasses<numExamplesOfThisClass<example>>
     *
     * This has the advantage over using trainOne that loss function statistics can be more easily reported
     *
     * @param allData see above
     * @param numIterations number of iterations of gradient descent to perform
     */
    public double[][] trainAll(List<List<DoubleVector>> allData, int numIterations) {

        int numExamples = 0;
        for(List<DoubleVector> l : allData) {
            numExamples += l.size();
        }

        // Keep track of the loss function for all one-v-all models for all iterations
        double[][] loss = new double[numIterations][numClasses];
        int[] trainingCorrect = new int[numIterations];
        for(int r=0; r<numIterations; r++) {

            // Calculate the loss function over all examples BEFORE training the model
            for(int c=0; c<numClasses; c++) {
                for(DoubleVector example : allData.get(c)) {
                    double[] lossesThisExample = lossForExample(example, c);
                    for(int c2=0; c2<numClasses; c2++) {
                        loss[r][c2] += lossesThisExample[c2];
                    }
                    if(classify(example) == c) {
                        trainingCorrect[r]++;
                    }
                }
            }

            // should this be randomized (don't do all of each class in a chunk), or is it pretty much equivalent?
            for(int c=0; c<numClasses; c++) {
                // Go ahead and divide the loss functions here (can't do it in the other loop, where they're being added)
                loss[r][c] /= numExamples;
                for(DoubleVector example : allData.get(c)) {
                    trainOne(example, c);
                }
            }
        }
        return loss;
    }

    public void trainOne(DoubleVector example, int classLabel) {
        if(modelType.equals(LOGISTIC)) {
            trainLogistic(example, classLabel);
        }
        else if(modelType.equals(SVM)) {
            trainSvm(example, classLabel);
        }
        else {
            System.out.println("Unsupported model type! Not training.");
        }
    }

    // Returns the loss for all classes for this example
    public double[] lossForExample(DoubleVector example, int classLabel) {

        double[] loss = new double[numClasses];
        // log-odds loss
        if(modelType.equals(LOGISTIC)) {
            for(int c=0; c<numClasses; c++) {
                if(c == classLabel) {
                    loss[c] = -Math.log(logisticHypothesis(c, example));
                }
                else {
                    loss[c] = -Math.log(1.0 - logisticHypothesis(c, example));
                }
            }
        }

        // hinge loss
        if(modelType.equals(SVM)) {
            for(int c=0; c<numClasses; c++) {
                double hingeLoss;
                if(c == classLabel) {
                    hingeLoss = 1 - linearHypothesis(c, example);
                }
                else {
                    hingeLoss = 1 + linearHypothesis(c, example);
                }
                if(hingeLoss > 0) loss[c] = hingeLoss;
            }
        }
        return loss;
    }

    // Perform one round of gradient descent for all classes for a single example
    private void trainLogistic(DoubleVector example, int classLabel) {
        int[] y = new int[numClasses];
        y[classLabel] = 1;
        for(int setClass = 0; setClass < numClasses; setClass++) {
            double hyp = logisticHypothesis(setClass, example);
            // Not quite the partial derivative--this ignores which feature we're on, but it's quicker to not recalculate for every feature
            double partialDeriv = ((double)y[setClass] - hyp);
            for(int j : example.getKeySet()) {
                // NOW multiply in the value of the example
                theta[setClass][j] += example.get(j) * alpha * partialDeriv + lambda * theta[setClass][j];
            }
            if(usingIntercept) {
                theta[setClass][numFeatures - 1] += alpha * partialDeriv;
            }
        }
    }

    // Perform one round of gradient descent for all classes for a single example
    private void trainSvm(DoubleVector example, int classLabel) {
        int[] y = new int[numClasses];
        Arrays.fill(y, -1);
        y[classLabel] = 1;

        for(int setClass = 0; setClass < numClasses; setClass++) {
            double hyp = linearHypothesis(setClass, example);
            // hingeLoss might be less than zero, but we won't continue if it is
            double hingeLoss = 1 - (y[setClass] * hyp);
            if(hingeLoss > 0) {
                for(int j : example.getKeySet()) {
                    // This is the update rule used for logistic regression; does it apply the same way here?
                    // It seems the derivative is MUCH greater
                    theta[setClass][j] += alpha * example.get(j) * y[setClass] + lambda * theta[setClass][j];
                }
                if(usingIntercept) {
                }
            }
        }
    }

    // hypothesis for logistic regression
    private double logisticHypothesis(int whichClass, DoubleVector example) {
        double x = 1.0 / (1.0 + Math.exp(-linearHypothesis(whichClass, example)));
        if(x < 0 || x > 1)
            System.out.println("PROBLEM WITH LOGISTIC HYPOTHESIS FUNCTION");
        return x;
    }

    // Take the dot product of theta and a vector for a given hypothesized class
    private double linearHypothesis(int whichClass, DoubleVector v2) {
        // initialize sum to just be the intercept term
        double sum = theta[whichClass][numFeatures-1];
        for(int key : v2.getKeySet()) {
            sum += v2.get(key) * theta[whichClass][key];
        }
        return sum;
    }

}
