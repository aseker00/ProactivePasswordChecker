package project.lm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

public class LanguageModel {
	protected int order;
	protected double totalUnigramCount;
	protected FrequencyMatrix T;
	protected HashSet<Gram> V;
	
	public LanguageModel(int o) {
		this.order = o;
		this.totalUnigramCount = 0;
		this.T = new FrequencyMatrix();
		this.V = new HashSet<Gram>();
	}
	
	public void vocabulary(HashSet<Gram> v) {
		this.V = v;
	}
	
	public double stringProbability(String s) {
		double p = 1;
		Vector<NGram> ngrams = toNGrams(s);
		Iterator<NGram> ngramsIter = ngrams.iterator();
		while (ngramsIter.hasNext()) {
			NGram ngram = ngramsIter.next();
			p *= getTransitionProbability(ngram);
		}
		return p;
	}
	
	protected double getTransitionProbability(NGram ngram) {
		return T.ngramFrequency(ngram);
	}
	
	public void trainingSet(File f) throws IOException {
		FrequencyMatrix counts = calculateNGramCounts(toNGrams(f));
		//counts.unitTest(null);
		totalUnigramCount = getTotalUnigramCounts(counts);
		T = estimateTransitionProbabilities(counts);
	}
	
	public void crossValidationSet(File f) throws IOException {
	}
	
	public void testSet(File f) throws IOException {
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