package project.lm;

import java.util.Iterator;

/*
 * A language model that implements the Katz Back-off smoothing technique
 * to handle unseen n-grams during test.
 * Unlike the Good Turing method, this is a discounting method where each count
 * is subtracted some discounting value and the missing probability mass is 
 * used to account for unseen n-grams 
 */
public class KatzBackoffModel extends LanguageModel {
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
	}
	
	/*
	 * If the n-gram was unseen in the training data, give it the unseen backed-off
	 * transition probability.
	 */
	@Override
	protected double getTransitionProbability(NGram ngram) throws Exception {
		double p = super.getTransitionProbability(ngram);
		if (p == 0.0)
			return getBackoffTransitionProbability(ngram);
		return p;
	}
	
	/*
	 * Discount the current counts.
	 * Calculate the missing probability.
	 * Finally compute the backed-off transition probability matrix.
	 */
	@Override
	protected FrequencyMatrix estimateTransitionProbabilities(FrequencyMatrix fm) throws Exception {
		this.counts = fm;
		this.countsStar = discount(discountValue);
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
	
	/*
	 * Subtract the discount value from all the counts
	 */
	private FrequencyMatrix discount(double dv) {
		FrequencyMatrix fm = new FrequencyMatrix();
		Iterator<NGram> countsIter = this.counts.iterator();
		while (countsIter.hasNext()) {
			
			// This is the count discounting part
			NGram ngram = countsIter.next();
			double val = this.counts.ngramFrequency(ngram);
			double cstar = val - dv;
			fm.ngramFrequency(ngram, cstar);
			
			// This part I am not sure about - account for the suffix bi-grams
			// in the last tri-gram.
			if (ngram.gram(ngram.length()-1).equals(Gram.STOP)) {
				NGram ng = ngram.sub(1, ngram.length()-1);
				val = fm.ngramFrequency(ng);
				val++;
				fm.ngramFrequency(ng, val);
			}
		}
		
		// This part is related to the part I am not sure about
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
	
	/*
	 * Product the alpha matrix.
	 * a = 1 - sum(Count(Wi-2,Wi-1,W)/Count(Wi-2,Wi-1)) for all W that Count(Wi-2,Wi-1,W) > 0
	 */
	private FrequencyMatrix calculateMissingProbabilityMass() throws Exception {
		FrequencyMatrix	a = new FrequencyMatrix();
		Iterator<NGram> countsIter = this.counts.iterator();
		while (countsIter.hasNext()) {
			NGram ngram = countsIter.next();
			if (ngram.length() == 1)
				continue;
			double countStarVal = this.countsStar.ngramFrequency(ngram);
			NGram ngrami = ngram.sub(0, ngram.length()-1);
			double countVali = this.counts.ngramFrequency(ngrami);
			Double aVali = a.ngramFrequency(ngrami);
			if (aVali == 0.0)
				aVali = 1.0;
			aVali -= countStarVal/countVali;
			a.ngramFrequency(ngrami, aVali);
		}
		return a;
	}
	
	/*
	 * Return the adjusted transition probability matrix after the
	 * adjusted counts are calculated
	 */
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
	
	/*
	 * if the n-gram was seen in the training data then
	 * q = C*(Wi-2,Wi-1,W)/Count
	 * else
	 * q = a(Wi-2,Wi-1) * q(Wi-1,W)/sum(q(Wi-1,W)) for all W such Count(Wi-2,Wi-1,W) = 0
	 */
	private double getBackoffTransitionProbability(NGram ngram) throws Exception {
		double p = 0.0;
		double c = this.countsStar.ngramFrequency(ngram);
		if (c > 0) {
			p = getDiscountedProbability(ngram);
		}
		else {
			if (ngram.length() == 1)
				throw new Exception("invalid ngram length: " + ngram);
			if (ngram.length() == 2) {
				NGram ngrami = ngram.sub(0, ngram.length()-1);
				double a = this.alpha.ngramFrequency(ngrami);
				if (a == 0.0)
					a = 1.0;
				NGram ng = ngram.sub(1, ngram.length()-1);
				double cc = this.counts.ngramFrequency(ng);
				if (cc == 0.0) {
					cc = 1.0/Math.pow(this.V.size(), 2);
				}
				double sum = getSumBackoffs(ngrami);
				p = a*cc/sum;
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
	
	/*
	 * The discounted probability is C*(Wi-2,Wi-1,W)/Count
	 */
	private double getDiscountedProbability(NGram ngram) {
		double c = 0.0;
		double cstar = this.countsStar.ngramFrequency(ngram);
		if (ngram.length() == 1)
			c = this.totalUnigramCount;
		else
			c = this.counts.ngramFrequency(ngram.sub(0, ngram.length()-1));
		return cstar/c;
	}
	
	/*
	 * sum the probabilities of all the n-grams that were not seen in the training set
	 */
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