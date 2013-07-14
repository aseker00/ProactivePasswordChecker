package project.lm;

import java.util.HashMap;
import java.util.Iterator;

public class GoodTuringModel extends LanguageModel {
	
	private FrequencyMatrix counts;
	private HashMap<Double, Integer> N;
	private double zeroProbability;

	public GoodTuringModel(int o) {
		super(o);
		this.N = new HashMap<Double, Integer>();
	}
	
	@Override
	protected double getTransitionProbability(NGram ngram) throws Exception {
		double p = super.getTransitionProbability(ngram);
		if (p > 0.0)
			return p;
		return this.zeroProbability;
	}
	
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
	
	private FrequencyMatrix recalculateNGramFrequencies() {
		FrequencyMatrix freq = new FrequencyMatrix();
		Iterator<NGram> countsIter = this.counts.iterator();
		while (countsIter.hasNext()) {
			NGram ngram = countsIter.next();
			if (ngram.length() == this.order) {
				double val = this.counts.ngramFrequency(ngram);
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
	
	private double getAdjustedCount(NGram ngram) {
		double val = this.counts.ngramFrequency(ngram);
		if (val <= 5.0) {
			int n1 = this.N.get(val+1.0);
			int n = this.N.get(val);
			val = (val+1.0)*n1/n;
		}
		return val;
	}
}