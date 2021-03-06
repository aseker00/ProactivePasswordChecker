package project.lm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.Iterator;
import java.util.Vector;

/*
 * Main interface:
 * 	train - given a training set, produce the transition probability matrix associated with the 
 * maximum likelihood counts.
 * 	test - given a test set or a single password produce the probability of them/it being generated
 * from the language model.
 */
public class LanguageModel {
	protected int order;			// ngrams
	protected double totalUnigramCount;
	protected FrequencyMatrix T;	// transition probability matrix
	protected Vocabulary V;			// vocabulary
	protected double mu;			// mean
	protected double sigma;			// standard deviation
	
	public LanguageModel(int o) {
		this.order = o;
		this.totalUnigramCount = 0;
		this.T = new FrequencyMatrix();
		this.V = new Vocabulary();
	}
	
	public void vocabulary(Vocabulary v) {
		this.V = v;
	}
	
	public void setMu(double mu) {
		this.mu = mu;
	}
	
	public void setSigma(double sigma) {
		this.sigma = sigma;
	}
	
	public double getMu() {
		return this.mu;
	}
	
	public double getSigma() {
		return this.sigma;
	}
	
	public FrequencyMatrix getTransitionProbabilityMatrix() {
		return this.T;
	}
	
	public int getOrder() {
		return this.order;
	}
	
	public void setTransitionProbabilityMatrix(FrequencyMatrix tm) {
		this.T = tm;
	}
	
	/*
	 * Compute llf = sum of log likelihood of each ngram
	 * Transrom the log likelihood estimate into the final test, need to scale, center and normalize
	 */
	public double test(String s) throws Exception {
		Vector<NGram> ngrams = toNGrams(s);
		if (ngrams == null)
			return 0.0;
		double ll = logLikelihood(ngrams);
		double p = (ll/ngrams.size()-mu)/sigma;
		return p;
	}
	
	private double logLikelihood(Vector<NGram> ngrams) throws Exception {
		double ll = 0.0;
		Iterator<NGram> ngramsIter = ngrams.iterator();
		while (ngramsIter.hasNext()) {
			NGram ngram = ngramsIter.next();
			double p = getTransitionProbability(ngram);
			ll += Math.log(p);
		}
		return ll;
	}
	
	protected double getTransitionProbability(NGram ngram) throws Exception {
		return T.ngramFrequency(ngram);
	}
	
	/*
	 * Generate the counts based on the training data
	 * Generate the transition probability matrix based on the maximum likelihood estimation
	 * Generate the mean and standard deviation
	 */
	public void train(File f) throws Exception {
		Vector<Vector<NGram>> ngrams = toNGrams(f);
		FrequencyMatrix counts = calculateNGramCounts(ngrams);
		//counts.unitTest(null);
		this.totalUnigramCount = getTotalUnigramCounts(counts); // use this as fall-back in case some symbol was not seen in the training data
		this.T = estimateTransitionProbabilities(counts);
		this.mu = calculateMean(ngrams);
		this.sigma = calculateStandardDeviation(ngrams);
	}
	
	private double calculateMean(Vector<Vector<NGram>> ngs) throws Exception {
		double value = 0.0;
		Vector<NGram> ngrams = null;
		Iterator<Vector<NGram>> ngsIter = ngs.iterator();
		while (ngsIter.hasNext()) {
			ngrams = ngsIter.next();
			double ll = logLikelihood(ngrams);
			value += ll/ngrams.size();
		}
		return value/ngs.size();
	}
	
	private double calculateStandardDeviation(Vector<Vector<NGram>> ngs) throws Exception {
		double value = 0.0;
		Iterator<Vector<NGram>> ngsIter = ngs.iterator();
		while (ngsIter.hasNext()) {
			Vector<NGram> ngrams = ngsIter.next();
			double ll = logLikelihood(ngrams);
			value += Math.pow(ll/ngrams.size()-mu,2);
		}
		return Math.sqrt(value/ngs.size());
	}
	
	/*
	 * TBD: would be used by LinearInterpolcation
	 */
	public void crossValidationSet(File f) throws IOException {
	}
	
	protected FrequencyMatrix estimateTransitionProbabilities(FrequencyMatrix counts) throws Exception {
		return maximumLikelihoodEstimate(counts);
	}
	
	/*
	 * q = Count(Wi-2,Wi-1,W)/Count(Wi-2,Wi-1)
	 */
	private FrequencyMatrix maximumLikelihoodEstimate(FrequencyMatrix counts) {
		FrequencyMatrix tm = new FrequencyMatrix();
		Iterator<NGram> countsIter = counts.iterator();
		while (countsIter.hasNext()) {
			NGram ngram = countsIter.next();
			double val = counts.ngramFrequency(ngram);
			double vali;
			if(ngram.length() > 1) {
				NGram ngrami = ngram.sub(0, ngram.length()-1);
				vali = counts.ngramFrequency(ngrami);
			}
			else {
				vali = this.totalUnigramCount;
			}
			double q = val/vali;
			tm.ngramFrequency(ngram, q);
		}
		return tm;
	}
	
	private Vector<Vector<NGram>> toNGrams(File f) throws IOException {
		Vector<Vector<NGram>> ngrams = new Vector<Vector<NGram>>();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
		String line = null;
		while ((line = br.readLine()) != null) {
			Vector<NGram> ngs = toNGrams(line);
			if (ngs != null)
				ngrams.add(ngs);
		}
		br.close();
		return ngrams;
	}
	
	/*
	 * Transform a password into a list of n-grams.
	 * Remember that the first two n-grams are:
	 * <START,START,W1>, <START,W1,W2>
	 * and the last n-gram is:
	 * <Wn-1, Wn,STOP>
	 * in order to account for the length of the password being a random variable 
	 */
	private Vector<NGram> toNGrams(String x) {
		Vector<NGram> ngrams = new Vector<NGram>();
		NGram ngram = null;
		for (int i = 0; i < x.length(); ++i) {
			ngram = new NGram(3);
			if (i == 0) {
				ngram.gram(0, Gram.START);
				ngram.gram(1, Gram.START);
				ngram.gram(2, this.V.get(new Gram(x.charAt(i))));
			}
			else if (i == 1) {
				ngram.gram(0, Gram.START);
				ngram.gram(1, this.V.get(new Gram(x.charAt(i-1))));
				ngram.gram(2, this.V.get(new Gram(x.charAt(i))));
			}
			else {
				ngram.gram(0, this.V.get(new Gram(x.charAt(i-2))));
				ngram.gram(1, this.V.get(new Gram(x.charAt(i-1))));
				ngram.gram(2, this.V.get(new Gram(x.charAt(i))));
			}
			ngrams.add(ngram);
		}
		if (ngram != null) {
			NGram ngramfin = new NGram(3);
			ngramfin.gram(0, ngram.gram(1));
			ngramfin.gram(1, ngram.gram(2));
			ngramfin.gram(2, Gram.STOP);
			ngrams.add(ngramfin);
		}
		if (ngrams.isEmpty())
			return null;
		return ngrams;
	}
	
	private double getTotalUnigramCounts(FrequencyMatrix counts) {
		double total = 0;
		Iterator<NGram> valuesIter = counts.iterator();
		while (valuesIter.hasNext()) {
			NGram ng = valuesIter.next();
			if (ng.length() == 1) {
				double val = counts.ngramFrequency(ng);
				total += val;
			}
		}
		return total;
	}
	
	/*
	 * For each n-gram, increment its value in the Frequency Matrix
	 */
	private FrequencyMatrix calculateNGramCounts(Vector<Vector<NGram>> ngs) {
		FrequencyMatrix counts = new FrequencyMatrix();
		Iterator<Vector<NGram>> ngsIter = ngs.iterator();
		while (ngsIter.hasNext()) {
			Vector<NGram> ngrams = ngsIter.next();
			NGram ngram = null;
			Iterator<NGram> ngramsIter = ngrams.iterator();
			while (ngramsIter.hasNext()) {
				ngram = ngramsIter.next();
				for (int i = 0; i < ngram.length(); i++) {
					NGram ngrami = ngram.sub(0, ngram.length()-i);
					double val = counts.ngramFrequency(ngrami);
					val++;
					counts.ngramFrequency(ngrami, val);
				}
			}
		}
		return counts;
	}
}