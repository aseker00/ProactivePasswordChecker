package project.lm;

import java.util.HashMap;
import java.util.Iterator;

/*
 * A language model that implements the Good Turing smoothing technique
 * to handle unseen n-grams during test
 */
public class GoodTuringModel extends LanguageModel {
	
	private FrequencyMatrix counts;		// adjusted counts
	private HashMap<Double, Integer> N;	// save the number of n-grams that appear a certain amount of times.
										// for example, N[5] represents the number of n-grams that appear 5 times
										// in the training set
	private double zeroProbability;		// the remaining probability mass after adjusting the counts - to be used
										// by unseen n-grams.

	public GoodTuringModel(int o) {
		super(o);
		this.N = new HashMap<Double, Integer>();
	}
	
	public void setZero(double p) {
		this.zeroProbability = p;
	}
	
	public double getZero() {
		return this.zeroProbability;
	}
	
	@Override
	protected double getTransitionProbability(NGram ngram) throws Exception {
		double p = super.getTransitionProbability(ngram);
		if (p > 0.0)
			return p;
		return this.zeroProbability;
	}
	
	/*
	 * Generate the frequency count matrix
	 * Calculate the zero mass probability according to the frequency count of
	 * the n-grams that occurred 1 time.
	 * Recalculate the counts based on the redistribution of probability.
	 * Finally re-estimate the the transition probabilities based on the new counts. 
	 */
	@Override
	protected FrequencyMatrix estimateTransitionProbabilities(FrequencyMatrix fm) throws Exception {
		this.counts = fm;
		this.N = calculateFrequencyCounts();
		this.zeroProbability = calculateZeroCount();
		this.counts = recalculateNGramFrequencies();
		return super.estimateTransitionProbabilities(this.counts);
	}
	
	private HashMap<Double, Integer> calculateFrequencyCounts() {
		HashMap<Double, Integer> fcounts = new HashMap<Double, Integer>();
		Iterator<NGram> countsIter = this.counts.iterator();
		while (countsIter.hasNext()) {
			NGram ngram = countsIter.next();
			if (ngram.length() == this.order) {
				double val = this.counts.ngramFrequency(ngram);
				Integer num = fcounts.get(val);
				if (num == null)
					num = 0;
				num++;
				fcounts.put(val, num);
			}
		}
		return fcounts;
	}
	
	/*
	 * C0 = N[1]/N where N is the total number of n-grams 
	 */
	private double calculateZeroCount() {
		double total = 0;
		Iterator<Double> NIter = this.N.keySet().iterator();
		while (NIter.hasNext()) {
			double num = NIter.next();
			total += num*this.N.get(num);
		}
		int n1 = this.N.get(1.0);
		return n1/total;
	}
	
	/*
	 * For each n-gram adjust its counts (as well as all the sub n-grams)
	 */
	private FrequencyMatrix recalculateNGramFrequencies() {
		FrequencyMatrix freq = new FrequencyMatrix();
		Iterator<NGram> countsIter = this.counts.iterator();
		while (countsIter.hasNext()) {
			NGram ngram = countsIter.next();
			if (ngram.length() == this.order) {
				double val = this.counts.ngramFrequency(ngram);
				
				// Based on Katz recommendation treat n-gram that occur once as unseen
				if (val > 1.0) {
					val = getAdjustedCount(ngram);
					freq.ngramFrequency(ngram, val);
					for (int i = 0; i < ngram.length(); i++) {
						NGram ngrami = ngram.sub(0, ngram.length()-i);
						double vali = freq.ngramFrequency(ngrami);
						vali += val;
						freq.ngramFrequency(ngrami, vali);
					}
				}
			}
		}
		return freq;
	}
	
	/*
	 * C* = (C+1) * (Nc+1/Nc)
	 */
	private double getAdjustedCount(NGram ngram) {
		double val = this.counts.ngramFrequency(ngram);
		
		// Based on Katz recommendation, only adjust counts for n-grams that occur 5 or less times.
		if (val <= 5.0) {
			int n1 = this.N.get(val+1.0);
			int n = this.N.get(val);
			val = (val+1.0)*n1/n;
		}
		return val;
	}
}