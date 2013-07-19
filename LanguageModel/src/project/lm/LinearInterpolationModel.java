package project.lm;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

/*
 * Linear Interpolation smoothing
 * Not used!!
 */
public class LinearInterpolationModel extends LanguageModel {
	
	private FrequencyMatrix counts;

	public LinearInterpolationModel(int o) {
		super(o);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected double getTransitionProbability(NGram ngram) {
		return 0.0;
	}
	
	@Override
	protected FrequencyMatrix estimateTransitionProbabilities(FrequencyMatrix fm) {
		return null;
	}
	
	private Double[] linearInterpolation(File f, HashMap<NGram, Double> q, double epsilon) throws IOException {
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
					double ctag = this.counts.ngramFrequency(ngram3);
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
}