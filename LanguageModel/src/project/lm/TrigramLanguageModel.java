package project.lm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

public class TrigramLanguageModel {

	private HashSet<Gram> V;
	private HashMap<NGram, Double> counts;
	private HashMap<NGram, Double> q;
	private int smoothingMethod;
	private Double[] lambda;
	private HashMap<NGram, Double> alpha;
	public static final int SMOOTH_NONE = 0;
	public static final int SMOOTH_LINEAR_INTERPOLATION = 1;
	public static final int SMOOTH_BACKOFF = 2;
	public static final int SMOOTH_GOOD_TURING = 3;
	public NGram t;
	
	public void setVocab(File f) throws IOException {
		this.V = new HashSet<Gram>();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
		String line = null;
		while ((line = br.readLine()) != null) {
			for (int i = 0; i < line.length(); i++) {
				Gram g = new Gram(line.charAt(i));
				this.V.add(g);
			}
		}
		br.close();
	}
	
	public void setSmoothingMethod(int method) {
		this.smoothingMethod = method;
	}

	public double prob(String x) {
		double sum = 1;
		Vector<NGram> ngrams = toNGrams(x);
		Iterator<NGram> ngramsIter = ngrams.iterator();
		while (ngramsIter.hasNext()) {
			NGram ngram3 = ngramsIter.next();
			sum *= getProb(ngram3);
		}
		return sum;
	}
	
	public void train(File f) throws IOException {
		this.counts = count(f);
		double totalCount = 0;
		Iterator<NGram> countsIter = this.counts.keySet().iterator();
		while (countsIter.hasNext()) {
			NGram ngram1 = countsIter.next();
			if (ngram1.length() == 1) {
				Double val1 = this.counts.get(ngram1);
				totalCount += val1;
			}
		}
		this.counts.put(null, totalCount);
		HashMap<NGram, Double> cstar = null;
		switch (smoothingMethod) {
		case SMOOTH_BACKOFF:
			this.q = maximumLikelihoodEstimation(this.counts);
			cstar = discount(0.001, 0.01, 0.1);
			this.alpha = missingProbMass(cstar);
			this.q = backoff(cstar);
			break;
		case SMOOTH_GOOD_TURING:
			t = new NGram(2);
			t.gram(0, new Gram('p'));
			t.gram(1, new Gram('j'));
			Double v = this.counts.get(t);
			cstar = goodTuring(this.counts, 1);
			v = cstar.get(t);
			cstar = goodTuring(cstar, 2);
			v = cstar.get(t);
			this.q = maximumLikelihoodEstimation(cstar);
			break;
		}
	}

	public void crossValidate(File f) throws IOException {
		switch (smoothingMethod) {
		case SMOOTH_LINEAR_INTERPOLATION:
			this.lambda = linearInterpolation(f, this.q, 0.01);
			break;
		default:
			break;		
		}
	}
	
	public void test(File f) throws IOException {
		
	}
	
	private HashMap<NGram, Double> count(File f) throws IOException {
		HashMap<NGram, Double> C = new HashMap<NGram, Double>();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
		String line = null;
		while ((line = br.readLine()) != null) {
			Vector<NGram> ngrams = toNGrams(line);
			NGram ngram3 = null;
			Iterator<NGram> ngramsIter = ngrams.iterator();
			while (ngramsIter.hasNext()) {
				ngram3 = ngramsIter.next();
				NGram ngram2 = ngram3.sub(0, 2);
				NGram ngram1 = ngram2.sub(0, 1);
				Double val3 = C.get(ngram3);
				if (val3 == null)
					val3 = 0.0;
				val3++;
				C.put(ngram3, val3);
				Double val2 = C.get(ngram2);
				if (val2 == null)
					val2 = 0.0;
				val2++;
				C.put(ngram2, val2);
				Double val1 = C.get(ngram1);
				if (val1 == null)
					val1 = 0.0;
				val1++;
				C.put(ngram1, val1);
			}
		}
		br.close();
		return C;
	}
	
	private double getProb(NGram ngram3) {
		double p = 0;
		switch (smoothingMethod) {
		case SMOOTH_NONE:
		case SMOOTH_GOOD_TURING:
			p = q.get(ngram3);
			break;
		case SMOOTH_LINEAR_INTERPOLATION:
			if (ngram3.length() == 3) {
				NGram ngram2 = ngram3.sub(0, 2);
				NGram ngram1 = ngram2.sub(0, 1);
				double q3 = q.get(ngram3) == null ? 0 : q.get(ngram3);
				double q2 = q.get(ngram2) == null ? 0 : q.get(ngram2);
				double q1 = q.get(ngram1) == null ? 0 : q.get(ngram1);
				p = lambda[0]*q3 + lambda[1]*q2 + lambda[2]*q1;
			}
			break;
		case SMOOTH_BACKOFF:
			p = getBOParam(ngram3, null);
			break;
		default:
			break;
		}
		return p;
	}
	
	private HashMap<NGram, Double> maximumLikelihoodEstimation(HashMap<NGram, Double> C) {
		HashMap<NGram, Double> params = new HashMap<NGram, Double>();
		Iterator<NGram> countsIter = C.keySet().iterator();
		while (countsIter.hasNext()) {
			NGram ngram = countsIter.next();
			if (ngram == null)
				continue;
			Double val = C.get(ngram);
			double q = 0;
			if (ngram.length() == 3) {
				NGram ngram2 = ngram.sub(0, 2);
				Double val2 = C.get(ngram2);
				q = (double)val/val2;
			}
			else if (ngram.length() == 2) {
				NGram ngram1 = ngram.sub(0, 1);
				Double val1 = C.get(ngram1);
				q = (double)val/val1;
			}
			else if (ngram.length() == 1) {
				Double val0 = C.get(null);
				q = (double)val/val0;
			}
			params.put(ngram, q);
		}
		return params;
	}
	
	private HashMap<NGram, Double> discount(double discountValue1, double discountValue2, double discountValue3) throws IOException {
		HashMap<NGram, Double> countstar = new HashMap<NGram, Double>();
		Iterator<NGram> countsIter = this.counts.keySet().iterator();
		while (countsIter.hasNext()) {
			NGram ngram = countsIter.next();
			if (ngram == null)
				continue;
			Double val = this.counts.get(ngram);
			double cstar = 0;
			if (ngram.length() == 3) {
				cstar = val - discountValue1;
			}
			else if (ngram.length() == 2) {
				cstar = val - discountValue2;
			}
			else if (ngram.length() == 1) {
				cstar = val - discountValue3;
			}
			else {
				System.out.println("invalid ngram = " + ngram);
			}
			if (cstar > 0)
				countstar.put(ngram, cstar);
			else
				System.out.println("cstar = 0");
		}
		return countstar;
	}
	
	private HashMap<NGram, Double> missingProbMass(HashMap<NGram, Double> cstar) {
		HashMap<NGram, Double> a = new HashMap<NGram, Double>();
		Iterator<NGram> cstarIter = cstar.keySet().iterator();
		while (cstarIter.hasNext()) {
			NGram ngram = cstarIter.next();
			double cvalstar = cstar.get(ngram);
			if (ngram.length() == 3) {
				NGram ngram2 = ngram.sub(0, 2);
				Double cval2 = this.counts.get(ngram2);
				Double aval2 = a.get(ngram2);
				if (aval2 == null)
					aval2 = 1.0;
				aval2 -= cvalstar/cval2;
				a.put(ngram2, aval2);
			}
			else if (ngram.length() == 2) {
				NGram ngram1 = ngram.sub(0, 1);
				Double cval1 = this.counts.get(ngram1);
				Double aval1 = a.get(ngram1);
				if (aval1 == null)
					aval1 = 1.0;
				aval1 -= cvalstar/cval1;
				a.put(ngram1, aval1);
			}
			else if (ngram.length() == 1) {
				Double cval0 = this.counts.get(null);
				Double aval = a.get(null);
				if (aval == null)
					aval = 1.0;
				aval -= cvalstar/cval0;
				a.put(null, aval);
			}
			else {
				System.out.println("invalid ngram = " + ngram);
			}
		}
		return a;
	}
	
	private HashMap<NGram, Double> backoff(HashMap<NGram, Double> cstar) {
		HashMap<NGram, Double> params = new HashMap<NGram, Double>();
		Iterator<NGram> cstarIter = cstar.keySet().iterator();
		while (cstarIter.hasNext()) {
			NGram ngram = cstarIter.next();
			double qBO = getBOParam(ngram, cstar);
			params.put(ngram, qBO);
		}
		return params;
	}
	
	private double getBOParam(NGram ngram, HashMap<NGram, Double> cstar) {
		Double val = null;
		Double cval = this.counts.get(ngram);
		if (ngram.length() == 3) {
			if (cval != null) {
				if (cstar != null) {
					double cvalstar = cstar.get(ngram);
					NGram ngram2 = ngram.sub(0, 2);
					Double cval2 = this.counts.get(ngram2);
					val = cvalstar/cval2;
				}
				else {
					val = this.q.get(ngram);
				}
			}
			else {
				NGram ngram2 = ngram.sub(0, 2);
				NGram ngram0 = ngram.sub(1, 2);
				Double qval0 = getBOParam(ngram0, cstar);
				Double a2 = this.alpha.get(ngram2);
				if (a2 != null) {
					double sum = 0;
					Iterator<Gram> vocabIter = V.iterator();
					while (vocabIter.hasNext()) {
						Gram g = vocabIter.next();
						NGram ng = new NGram(3);
						ng.gram(0, ngram2.gram(0));
						ng.gram(1, ngram2.gram(1));
						ng.gram(2, g);
						Double cvalng = this.counts.get(ng);
						if (cvalng == null) {
							NGram ng0 = ng.sub(1, 2);
							Double qvalg = getBOParam(ng0, cstar);
							sum += qvalg;
						}
					}
					val = a2 * qval0/sum;
				}
				else {
					val = qval0;
				}
			}
		}
		else if (ngram.length() == 2) {
			if (cval != null) {
				if (cstar != null) {
					double cvalstar = cstar.get(ngram);
					NGram ngram1 = ngram.sub(0, 1);
					Double cval1 = this.counts.get(ngram1);
					val = cvalstar/cval1;
				}
				else {
					val = this.q.get(ngram);
				}
			}
			else {
				NGram ngram1 = ngram.sub(0, 1);
				NGram ngram0 = ngram.sub(1, 1);
				Double qval0 = getBOParam(ngram0, cstar);
				Double a1 = this.alpha.get(ngram1);
				if (a1 != null) {
					double sum = 0;
					Iterator<Gram> vocabIter = V.iterator();
					while (vocabIter.hasNext()) {
						Gram g = vocabIter.next();
						NGram ng = new NGram(2);
						ng.gram(0, ngram1.gram(0));
						ng.gram(1, g);
						Double cvalng = this.counts.get(ng);
						if (cvalng == null) {
							NGram ng0 = ng.sub(1, 1);
							Double qvalg = getBOParam(ng0, cstar);
							sum += qvalg;
						}
					}
					val = a1 * qval0/sum;
				}
				else {
					val = qval0;
				}
			}
		}
		else if (ngram.length() == 1) {
			val = this.q.get(ngram);
			if (val == null)
				val = 0.0;
		}
		else {
			System.out.println("invalid ngram = " + ngram);
		}
		return val;
	}
	
	private Double[] linearInterpolation(File f, HashMap<NGram, Double> q, double epsilon) throws IOException {
		HashMap<NGram, Double> counts = count(f);
		double c1, c2, c3;
		double lambda1, lambda2, lambda3;
		double lambda1_prev, lambda2_prev, lambda3_prev;
		
		Random rand = new Random();
		lambda1_prev = lambda1 = rand.nextDouble();
		lambda2_prev = lambda2 = rand.nextDouble();
		lambda3_prev = lambda3 = rand.nextDouble();
		while (true) {
			c1 = c2 = c3 = 0;
			Iterator<NGram> countsIter = q.keySet().iterator();
			while (countsIter.hasNext()) {
				NGram ngram3 = countsIter.next();
				if (ngram3.length() == 3) {
					NGram ngram2 = ngram3.sub(1, 2);
					NGram ngram1 = ngram2.sub(1, 1);
					double ctag = counts.get(ngram3) == null ? 0 : counts.get(ngram3);
					double val3 = q.get(ngram3);
					double val2 = q.get(ngram2);
					double val1 = q.get(ngram1);
					double denominator = lambda3*val3 + lambda2*val2 + lambda1*val1;
					c3 += (ctag*lambda3*val3)/denominator;
					c2 += (ctag*lambda2*val2)/denominator;
					c1 += (ctag*lambda1*val1)/denominator;
				}
			}
			
			lambda3 = c3/(c1+c2+c3);
			lambda2 = c2/(c1+c2+c3);
			lambda1 = c1/(c1+c2+c3);
			
			if (Math.abs(lambda3 - lambda3_prev) < epsilon &&
				Math.abs(lambda2 - lambda2_prev) < epsilon &&
				Math.abs(lambda1 - lambda1_prev) < epsilon)
				break;
			lambda3_prev = lambda3;
			lambda2_prev = lambda2;
			lambda1_prev = lambda1;
		}
		Double[] lambdas = {lambda3, lambda2, lambda1};
		return lambdas;
	}
	
	private HashMap<NGram, Double> goodTuring(HashMap<NGram, Double> C, int order) {
		HashMap<NGram, Double> params = new HashMap<NGram, Double>();
		HashMap<Double, Integer> N = new HashMap<Double, Integer>();
		Iterator<NGram> countsIter = C.keySet().iterator();
		while (countsIter.hasNext()) {
			NGram ngram = countsIter.next();
			if (ngram == null)
				continue;
			if (ngram.length() == order) {
				Iterator<Gram> vocabIter = this.V.iterator();
				while (vocabIter.hasNext()) {
					Gram g = vocabIter.next();
					NGram ng = new NGram(order+1);
					for (int i = 0; i < order; i++) {
						ng.gram(i, ngram.gram(i));
					}
					ng.gram(order, g);
					Double cvalng = C.get(ng);
					if (cvalng == null)
						cvalng = 0.0;
					Integer n = N.get(cvalng);
					if (n == null)
						n = 0;
					n++;
					N.put(cvalng, n);
				}
			}
		}
		
		countsIter = C.keySet().iterator();
		while (countsIter.hasNext()) {
			NGram ngram = countsIter.next();
			if (ngram == null) {
				params.put(ngram, C.get(ngram));
			}
			else if (ngram.length() == order) {
				Iterator<Gram> vocabIter = V.iterator();
				while (vocabIter.hasNext()) {
					Gram g = vocabIter.next();
					NGram ng = new NGram(order+1);
					for (int i = 0; i < order; i++) {
						ng.gram(i, ngram.gram(i));
					}
					ng.gram(order, g);
					Double cvalng = C.get(ng);
					if (cvalng == null || cvalng == 1.0)
						cvalng = 0.0;
					if (cvalng <= 5.0) {
						Integer n = N.get(cvalng);
						Integer n1 = N.get(cvalng+1);
						if (n1 == null)
							n1 = 0;
						double val = (cvalng+1)*n1/n;
						params.put(ng, val);
					}
					else {
						params.put(ng, cvalng);
					}
				}
			}
			params.put(ngram, C.get(ngram));
		}
		return params;
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
	
	
	
	public static void main(String[] args) {
		File vocabularyFile = new File(args[0]);
		File trainingSetFile = new File(args[1]);
		File crossValidationFile = new File(args[2]);
		TrigramLanguageModel tlm = new TrigramLanguageModel();
		tlm.setSmoothingMethod(TrigramLanguageModel.SMOOTH_BACKOFF);
		//tlm.setSmoothingMethod(TrigramLanguageModel.SMOOTH_GOOD_TURING);
		try {
			tlm.setVocab(vocabularyFile);
			tlm.train(trainingSetFile);
			tlm.crossValidate(crossValidationFile);
			double p1 = tlm.prob("aaaa");
			double p2 = tlm.prob("a%mT");
			System.out.println("p1 = " + p1);
			System.out.println("p2 = " + p2);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}