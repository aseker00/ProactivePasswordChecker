package project.lm;

import java.util.Iterator;

public class KatzBackoffModel extends LanguageModel {
	private double alphaTotal;
	private double discountValue;
	private FrequencyMatrix counts;
	private FrequencyMatrix countsStar;
	private FrequencyMatrix alpha;
	
	public KatzBackoffModel(int o, double val) {
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
		alphaTotal = 1.0;
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
				alphaTotal -= countStarVal/this.totalUnigramCount;
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
	
	private double getBackoffTransitionProbability(NGram ngram) {
		double p = 0.0;
		double c = this.counts.ngramFrequency(ngram);
		if (c > 0) {
			p = getDiscountedProbability(ngram);
		}
		else {
			if (ngram.length() == 1) {
				double sum = 0.0;
				Iterator<Gram> vocabIter = this.V.iterator();
				while (vocabIter.hasNext()) {
					Gram g = vocabIter.next();
					NGram ng = new NGram(1);
					ng.gram(0, g);
					if (this.counts.ngramFrequency(ng) == 0.0)
						sum++;
				}
				p = this.alphaTotal/sum;
			}
			else if (ngram.length() == 2) {
				NGram ngrami = ngram.sub(0, ngram.length()-1);
				double a = this.alpha.ngramFrequency(ngrami);
				NGram ng = ngram.sub(1, ngram.length()-1);
				double pp = this.counts.ngramFrequency(ng);
				if (pp == 0.0) {
					pp = 1.0/this.V.size();
				}
				double sum = getSumBackoffs(ngrami);
				p = a == 0.0 ? this.alphaTotal*pp/sum : a*pp/sum;
			}
			else {
				NGram ngrami = ngram.sub(0, ngram.length()-1);
				double a = this.alpha.ngramFrequency(ngrami);
				NGram ng = ngram.sub(1, ngram.length()-1);
				double pp = getBackoffTransitionProbability(ng);
				double sum = getSumBackoffs(ngrami);
				p = a == 0.0 ? pp/sum : a*pp/sum;
			}
		}
		return p;
	}
	
	private double getDiscountedProbability(NGram ngram) {
		double c = 0.0;
		double cstar = this.countsStar.ngramFrequency(ngram);
		if (ngram.length() == 1)
			c = this.totalUnigramCount;
		else
			c = this.counts.ngramFrequency(ngram.sub(0, ngram.length()-1));
		return cstar/c;
	}
	
	private double getSumBackoffs(NGram ngram) {
		double sum = 0.0;
		Iterator<Gram> vocabIter = this.V.iterator();
		while (vocabIter.hasNext()) {
			Gram g = vocabIter.next();
			NGram ng = new NGram(ngram.length()+1);
			for (int i = 0; i < ngram.length(); i++) {
				ng.gram(i, ngram.gram(i));
			}
			ng.gram(ng.length()-1, g);
			if (this.countsStar.ngramFrequency(ng) == 0.0) {
				ng = ng.sub(1, ng.length()-1);
				if (ng.length() == 1)
					sum += this.counts.ngramFrequency(ng);
				else
					sum += getBackoffTransitionProbability(ng);
			}
		}
		return sum;
	}
	
//	private Double getBackoffTransitionProbability(NGram ngram) {
//		if (ngram == null) {
//			double sum = 0;
//			Iterator<Gram> vocabIter = this.V.iterator();
//			while (vocabIter.hasNext()) {
//				Gram g = vocabIter.next();
//				NGram ng = new NGram(1);
//				ng.gram(0, g);
//				sum += this.countsStar.ngramFrequency(ngram);
//			}
//			return sum/this.V.size();
//		}
//		Double q = null;
//		double countStarVal = this.countsStar.ngramFrequency(ngram);
//		if (ngram.length() == 1) {
//			if (countStarVal > 0) {
//				q = countStarVal/this.totalUnigramCount;
//			}
//			else {
//				//q = this.counts.ngramFrequency(ngram);
//				double ngVal = getBackoffTransitionProbability(null);
//				double sum = sumBackoffTransitionProbability(null);
//				q = alphaTotal*ngVal/sum;
//			}
//		}
//		else {
//			NGram ngrami = ngram.sub(0, ngram.length()-1);
//			if (countStarVal > 0) {
//				ngrami = ngram.sub(0, ngram.length()-1);
//				double countiVal = this.counts.ngramFrequency(ngrami);
//				q = countStarVal/countiVal;
//			}
//			else {
//				double a = this.alpha.ngramFrequency(ngrami);
//				NGram ng = ngram.sub(1, ngram.length()-1);
//				double ngVal = getBackoffTransitionProbability(ng);
//				double sum = sumBackoffTransitionProbability(ngrami);
//				q = a*ngVal/sum;
//			}
//		}
////		else if (countiVal > 0) {
////			double a = this.alpha.ngramFrequency(ngrami);
////			Double ngQval;
////			NGram ng = ngram.sub(1, ngram.length()-1);
////			if (ng.length() > 1)
////				ngQval = getBackoffTransitionProbability(ng);
////			else
////				ngQval = this.counts.ngramFrequency(ng);
////			double sum = sumBackoffTransitionProbability(ngrami);
////			q = a * ngQval/sum;
////		}
////		else {
////			NGram ng = ngram.sub(1, ngram.length()-1);
////			return getBackoffTransitionProbability(ng);
////		}
//		return q;
//	}
//	
//	private double sumBackoffTransitionProbability(NGram ngram) {
//		double sum = 0;
//		Iterator<Gram> vocabIter = this.V.iterator();
//		while (vocabIter.hasNext()) {
//			Gram g = vocabIter.next();
//			NGram ng = ngram == null ? new NGram(1) : new NGram(ngram.length()+1);
////			if (ngram != null)
////			NGram ng = new NGram(ngram.length()+1);
//			for (int i = 0; i < ng.length()-1; i++) {
//				ng.gram(i, ngram.gram(i));
//			}
//			ng.gram(ng.length()-1, g);
//			if (this.countsStar.ngramFrequency(ng) == 0.0) {
//				ng = ng.sub(1, ng.length()-1);
////				ng = new NGram(ngram.length());
////				for (int i = 1; i < ngram.length(); i++) {
////					ng.gram(i-1, ngram.gram(i));
////				}
////				ng.gram(ngram.length()-1, g);
////				if (ng.length() > 1)
////					sum += getBackoffTransitionProbability(ng);
////				else
////					sum += this.counts.ngramFrequency(ng);
//				sum += getBackoffTransitionProbability(ng);
//			}
//		}
//		return sum;
//	}
}