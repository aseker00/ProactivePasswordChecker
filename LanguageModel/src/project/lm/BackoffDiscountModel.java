package project.lm;

import java.util.Iterator;

public class BackoffDiscountModel extends LanguageModel {
	private double alphaTotal;
	private double discountValue;
	private FrequencyMatrix counts;
	private FrequencyMatrix countsStar;
	private FrequencyMatrix alpha;
	
	public BackoffDiscountModel(int o, double val) {
		super(o);
		this.discountValue = val;
		counts = new FrequencyMatrix();
		countsStar = new FrequencyMatrix();
		alpha = new FrequencyMatrix();
		alphaTotal = 1.0;
	}
	
	@Override
	protected double getTransitionProbability(NGram ngram) {
		return getBackoffTransitionProbability(ngram);
	}
	
	@Override
	protected FrequencyMatrix estimateTransitionProbabilities(FrequencyMatrix fm) {
		this.counts = fm;
		this.countsStar = discount(discountValue);
		this.alpha = calculateMissingProbabilityMass();
		return backoff();
	}
	
	private FrequencyMatrix discount(double dv) {
		FrequencyMatrix dfm = new FrequencyMatrix();
		Iterator<NGram> countsIter = this.counts.iterator();
		while (countsIter.hasNext()) {
			NGram ngram = countsIter.next();
			double val = this.counts.ngramFrequency(ngram);
			double cstar = val - dv;
			dfm.ngramFrequency(ngram, cstar);
		}
		return dfm;
	}
	
	private FrequencyMatrix calculateMissingProbabilityMass() {
		FrequencyMatrix	a = new FrequencyMatrix();
		Iterator<NGram> countsIter = this.counts.iterator();
		while (countsIter.hasNext()) {
			NGram ngram = countsIter.next();
			double countStarVal = this.countsStar.ngramFrequency(ngram);
			if (ngram.length() == 3) {
				NGram ngram2 = ngram.sub(0, 2);
				double countVal2 = this.counts.ngramFrequency(ngram2);
				Double aVal2 = a.ngramFrequency(ngram2);
				if (aVal2 == 0.0)
					aVal2 = 1.0;
				aVal2 -= countStarVal/countVal2;
				a.ngramFrequency(ngram2, aVal2);
			}
			else if (ngram.length() == 2) {
				NGram ngram1 = ngram.sub(0, 1);
				double countVal1 = this.counts.ngramFrequency(ngram1);
				Double aVal1 = a.ngramFrequency(ngram1);
				if (aVal1 == 0.0)
					aVal1 = 1.0;
				aVal1 -= countStarVal/countVal1;
				a.ngramFrequency(ngram1, aVal1);
			}
			else {
				double countVal0 = this.totalUnigramCount;
				alphaTotal -= countStarVal/countVal0;
			}
		}
		return a;
	}
	
	private FrequencyMatrix backoff() {
		FrequencyMatrix tm = new FrequencyMatrix();
		Iterator<NGram> countsIter = this.counts.iterator();
		while (countsIter.hasNext()) {
			NGram ngram = countsIter.next();
			Double q = getBackoffTransitionProbability(ngram);
			tm.ngramFrequency(ngram, q);
		}
		return tm;
	}
	
	private Double getBackoffTransitionProbability(NGram ngram) {
		Double q = null;
		double countVal = this.counts.ngramFrequency(ngram);
		NGram ngrami = ngram.sub(0, ngram.length()-1);
		double countiVal = this.counts.ngramFrequency(ngrami);
		if (countVal > 0) {
			double countStarVal = this.countsStar.ngramFrequency(ngram);
			q = countStarVal/countiVal;
		}
		else if (countiVal > 0) {
			double a = this.alpha.ngramFrequency(ngrami);
			Double ngQval;
			NGram ng = ngram.sub(1, ngram.length()-1);
			if (ng.length() > 1)
				ngQval = getBackoffTransitionProbability(ng);
			else
				ngQval = this.counts.ngramFrequency(ng);
			double sum = sumBackoffTransitionProbability(ngrami);
			q = a * ngQval/sum;
		}
		else {
			NGram ng = ngram.sub(1, ngram.length()-1);
			return getBackoffTransitionProbability(ng);
		}
		return q;
	}
	
	private double sumBackoffTransitionProbability(NGram ngram) {
		double sum = 0;
		Iterator<Gram> vocabIter = this.V.iterator();
		while (vocabIter.hasNext()) {
			Gram g = vocabIter.next();
			NGram ng = new NGram(ngram.length()+1);
			for (int i = 0; i < ngram.length(); i++) {
				ng.gram(i, ngram.gram(i));
			}
			ng.gram(ngram.length(), g);
			if (this.counts.ngramFrequency(ng) == 0.0) {
				ng = new NGram(ngram.length());
				for (int i = 1; i < ngram.length(); i++) {
					ng.gram(i-1, ngram.gram(i));
				}
				ng.gram(ngram.length()-1, g);
				if (ng.length() > 1)
					sum += getBackoffTransitionProbability(ng);
				else
					sum += this.counts.ngramFrequency(ng);
			}
		}
		return sum;
	}
}