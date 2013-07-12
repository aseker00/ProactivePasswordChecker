package project.lm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

public class LanguageModel {
	protected int order;			// ngrams
	protected double totalUnigramCount;
	protected FrequencyMatrix T;	// transition probability matrix
	protected HashSet<Gram> V;		// vocabulary
	protected double mu;			// mean
	protected double sigma;			// standard deviation
	
	public LanguageModel(int o) {
		this.order = o;
		this.totalUnigramCount = 0;
		this.T = new FrequencyMatrix();
		this.V = new HashSet<Gram>();
	}
	
	public void vocabulary(HashSet<Gram> v) {
		this.V = v;
	}
	
	public double test(String s) {
		Vector<NGram> ngrams = toNGrams(s);
		if (ngrams == null)
			return 0.0;
		double ll = logLikelihood(ngrams);
		double p = (ll/ngrams.size()-mu)/sigma;
		return p;
	}
	
	private double logLikelihood(Vector<NGram> ngrams) {
		double ll = 0.0;
		Iterator<NGram> ngramsIter = ngrams.iterator();
		while (ngramsIter.hasNext()) {
			NGram ngram = ngramsIter.next();
			double p = getTransitionProbability(ngram);
			ll += Math.log(p);
		}
		return ll;
	}
	
	protected double getTransitionProbability(NGram ngram) {
		return T.ngramFrequency(ngram);
	}
	
	public void trainingSet(File f) throws IOException {
		Vector<Vector<NGram>> ngrams = toNGrams(f);
		FrequencyMatrix counts = calculateNGramCounts(ngrams);
		//counts.unitTest(null);
		this.totalUnigramCount = getTotalUnigramCounts(counts);
		this.T = estimateTransitionProbabilities(counts);
		this.mu = calculateMean(ngrams);
		this.sigma = calculateStandardDeviation(ngrams);
	}
	
	private double calculateMean(Vector<Vector<NGram>> ngs) {
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
	
	private double calculateStandardDeviation(Vector<Vector<NGram>> ngs) {
		double value = 0.0;
		Iterator<Vector<NGram>> ngsIter = ngs.iterator();
		while (ngsIter.hasNext()) {
			Vector<NGram> ngrams = ngsIter.next();
			double ll = logLikelihood(ngrams);
			value += Math.pow(ll/ngrams.size()-mu,2);
		}
		return Math.sqrt(value/ngs.size());
	}
	
	public void crossValidationSet(File f) throws IOException {
	}
	
	protected FrequencyMatrix estimateTransitionProbabilities(FrequencyMatrix counts) {
		return maximumLikelihoodEstimate(counts);
	}
	
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
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
		String line = null;
		while ((line = br.readLine()) != null) {
			Vector<NGram> ngs = toNGrams(line);
			if (ngs != null)
				ngrams.add(ngs);
		}
		br.close();
		return ngrams;
	}
	
	private Vector<NGram> toNGrams(String x) {
		Vector<NGram> ngrams = new Vector<NGram>();
		NGram ngram3 = null;
		for (int i = 0; i < x.length(); ++i) {
			ngram3 = new NGram(3);
			if (i == 0) {
				ngram3.gram(0, Gram.START);
				ngram3.gram(1, Gram.START);
				ngram3.gram(2, new Gram(x.charAt(i)));
			}
			else if (i == 1) {
				ngram3.gram(0, Gram.START);
				ngram3.gram(1, new Gram(x.charAt(i-1)));
				ngram3.gram(2, new Gram(x.charAt(i)));
			}
			else {
				ngram3.gram(0, new Gram(x.charAt(i-2)));
				ngram3.gram(1, new Gram(x.charAt(i-1)));
				ngram3.gram(2, new Gram(x.charAt(i)));
			}
			ngrams.add(ngram3);
		}
		if (ngram3 != null) {
			NGram ngram3fin = new NGram(3);
			ngram3fin.gram(0, ngram3.gram(1));
			ngram3fin.gram(1, ngram3.gram(2));
			ngram3fin.gram(2, Gram.STOP);
			ngrams.add(ngram3fin);
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