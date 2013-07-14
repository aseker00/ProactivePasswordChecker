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
	protected double getTransitionProbability(NGram ngram) throws Exception {
		double p = super.getTransitionProbability(ngram);
		if (p == 0.0)
			return getBackoffTransitionProbability(ngram);
		return p;
	}
	
	@Override
	protected FrequencyMatrix estimateTransitionProbabilities(FrequencyMatrix fm) throws Exception {
		this.counts = fm;
		this.countsStar = discount(discountValue);
		this.alphaTotal = 1.0;
		this.alpha = calculateMissingProbabilityMass();
		return backoff();
	}
	
	public FrequencyMatrix getAlpha() {
		return this.alpha;
	}
	
	public void setAlpha(FrequencyMatrix fm) {
		this.alpha = fm;
	}
	
	public FrequencyMatrix getCounts() {
		return this.counts;
	}
	
	public void setCounts(FrequencyMatrix fm) {
		this.counts = fm;
	}
	
	public FrequencyMatrix getCountsStar() {
		return this.countsStar;
	}
	
	public void setCountsStar(FrequencyMatrix fm) {
		this.countsStar = fm;
	}
	
	private FrequencyMatrix discount(double dv) {
		FrequencyMatrix fm = new FrequencyMatrix();
		Iterator<NGram> countsIter = this.counts.iterator();
		while (countsIter.hasNext()) {
			NGram ngram = countsIter.next();
			double val = this.counts.ngramFrequency(ngram);
			double cstar = val - dv;
			fm.ngramFrequency(ngram, cstar);
			if (ngram.gram(ngram.length()-1).equals(Gram.STOP)) {
				NGram ng = ngram.sub(1, ngram.length()-1);
				val = fm.ngramFrequency(ng);
				val++;
				fm.ngramFrequency(ng, val);
			}
		}
		countsIter = fm.iterator();
		while (countsIter.hasNext()) {
			NGram ngram = countsIter.next();
			if (ngram.gram(ngram.length()-1).equals(Gram.STOP)) {
				double val = fm.ngramFrequency(ngram);
				double cstar = val - dv;
				fm.ngramFrequency(ngram, cstar);
			}
		}
		return fm;
	}
	
	private FrequencyMatrix calculateMissingProbabilityMass() {
		FrequencyMatrix	a = new FrequencyMatrix();
		Iterator<NGram> countsIter = this.counts.iterator();
		while (countsIter.hasNext()) {
			NGram ngram = countsIter.next();
			double countStarVal = this.countsStar.ngramFrequency(ngram);
			if (ngram.length() > 1) {
				NGram ngrami = ngram.sub(0, ngram.length()-1);
				double countVali = this.counts.ngramFrequency(ngrami);
				Double aVali = a.ngramFrequency(ngrami);
				if (aVali == 0.0)
					aVali = 1.0;
				aVali -= countStarVal/countVali;
				a.ngramFrequency(ngrami, aVali);
			}
			else {
				alphaTotal -= countStarVal/this.totalUnigramCount;
			}
		}
		return a;
	}
	
	private FrequencyMatrix backoff() throws Exception {
		FrequencyMatrix tm = new FrequencyMatrix();
		Iterator<NGram> countsIter = this.counts.iterator();
		while (countsIter.hasNext()) {
			NGram ngram = countsIter.next();
			Double q = getBackoffTransitionProbability(ngram);
			tm.ngramFrequency(ngram, q);
		}
		return tm;
	}
	
	private double getBackoffTransitionProbability(NGram ngram) throws Exception {
		double p = 0.0;
		double c = this.countsStar.ngramFrequency(ngram);
		if (c > 0) {
			p = getDiscountedProbability(ngram);
		}
		else {
			if (ngram.length() == 1) {
				throw new Exception("invalid ngram length: " + ngram);
//				double sum = 0.0;
//				Iterator<Gram> vocabIter = this.V.iterator();
//				while (vocabIter.hasNext()) {
//					Gram g = vocabIter.next();
//					NGram ng = new NGram(1);
//					ng.gram(0, g);
//					if (this.counts.ngramFrequency(ng) == 0.0)
//						sum++;
//				}
//				p = this.alphaTotal/sum;
			}
			else if (ngram.length() == 2) {
				NGram ngrami = ngram.sub(0, ngram.length()-1);
				double a = this.alpha.ngramFrequency(ngrami);
				if (a == 0.0)
					throw new Exception("zero alpha for ngrami: " +  ngrami);
				NGram ng = ngram.sub(1, ngram.length()-1);
				double cc = this.counts.ngramFrequency(ng);
				if (cc == 0.0) {
					cc = 1.0/Math.pow(this.V.size(), 2);
					//throw new Exception("zero count for ngram: " +  ng);
				}
				double sum = getSumBackoffs(ngrami);
				//p = a == 0.0 ? this.alphaTotal*cc/sum : a*cc/sum;
				p = a*cc/sum;
			}
			else {
				NGram ngrami = ngram.sub(0, ngram.length()-1);
				double a = this.alpha.ngramFrequency(ngrami);
				//if (a == 0.0)
				//	throw new Exception("zero alpha for ngrami: " +  ngrami);
				NGram ng = ngram.sub(1, ngram.length()-1);
				double pp = getBackoffTransitionProbability(ng);
				double sum = getSumBackoffs(ngrami);
				p = a == 0.0 ? pp/sum : a*pp/sum;
				//p = a*pp/sum;
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
	
	private double getSumBackoffs(NGram ngram) throws Exception {
		double sum = 0.0;
		Iterator<Gram> vocabIter = this.V.iterator();
		while (vocabIter.hasNext()) {
			Gram g = vocabIter.next();
			NGram ng = new NGram(ngram.length()+1);
			for (int i = 0; i < ngram.length(); i++) {
				ng.gram(i, ngram.gram(i));
			}
			ng.gram(ng.length()-1, g);
			if (this.counts.ngramFrequency(ng) == 0.0) {
				ng = ng.sub(1, ng.length()-1);
				if (ng.length() == 1)
					sum += this.counts.ngramFrequency(ng);
				else
					sum += getBackoffTransitionProbability(ng);
			}
		}
		return sum;
	}
}