package project.lm;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

public class FrequencyMatrix {
	private HashMap<NGram, Double> values;
	
	public FrequencyMatrix() {
		this.values = new HashMap<NGram, Double>();
	}

	public void unitTest(File f) throws IOException {
		HashMap<NGram, Double> unigramCount = new HashMap<NGram, Double>();
		HashMap<NGram, Double> bigramCount = new HashMap<NGram, Double>();
		HashMap<NGram, Double> trigramCount = new HashMap<NGram, Double>();
		HashMap<NGram, Double> unigramTotalCount = new HashMap<NGram, Double>();
		HashMap<NGram, Double> bigramTotalCount = new HashMap<NGram, Double>();
		PrintStream ps = f == null ? System.out : new PrintStream(f); 
		Iterator<NGram> valuesIter = this.values.keySet().iterator();
		while (valuesIter.hasNext()) {
			NGram ngram = valuesIter.next();
			Double val = this.values.get(ngram);
			if (val == null || val.equals(0.0))
				ps.println("Error: values[" + ngram + "] = " + val);
			HashMap<NGram, Double> counti = ngram.length() == 3 ? trigramCount : (ngram.length() == 2 ? bigramCount : unigramCount);
			counti.put(ngram, val);
			HashMap<NGram, Double> totalCounti = ngram.length() == 3 ? bigramTotalCount : (ngram.length() == 2 ? unigramTotalCount : null);
			if (totalCounti != null) {
				NGram ngrami = ngram.sub(0, ngram.length()-1);
				Double vali = totalCounti.get(ngrami);
				if (vali == null)
					vali = 0.0;
				vali += val;
				totalCounti.put(ngrami, vali);
			}
		}
		ps.println("bigram check ...");
		valuesIter = bigramCount.keySet().iterator();
		while (valuesIter.hasNext()) {
			NGram ngram = valuesIter.next();
			if (!ngram.gram(ngram.length()-1).equals(Gram.STOP)) {
				Double val = bigramCount.get(ngram);
				Double total = bigramTotalCount.get(ngram);
				if (total == null)
					ps.println("missing bigram total: " + ngram);
				else if (!val.equals(total)) {
					ps.println("total count mismatch: count[" + ngram + "]=" + val + ", total[" + ngram + "]=" + total);
				}
			}
		}
		ps.println("bigram check done.");
		ps.println("unigram check ...");
		valuesIter = unigramCount.keySet().iterator();
		while (valuesIter.hasNext()) {
			NGram ngram = valuesIter.next();
			if (!ngram.gram(ngram.length()-1).equals(Gram.STOP)) {
				Double val = unigramCount.get(ngram);
				Double total = unigramTotalCount.get(ngram);
				if (total == null)
					ps.println("missing unigram total: " + ngram);
				else if (!val.equals(total))
					ps.println("totcal count mismatch: count[" + ngram + "]=" + val + ", total[" + ngram + "]=" + total);
			}
			
		}
		ps.println("unigram check done.");
		if (f != null)
			ps.close();
	}

	public void ngramFrequency(NGram ng, double val) {
		if (ng == null)
			throw new NullPointerException();
		//if (val == 0.0)
		//	this.values.remove(ng);
		else
			this.values.put(ng, val);
	}
	public double ngramFrequency(NGram ng) {
		Double value = values.get(ng);
		if (value == null)
			return 0;
		return value;
	}
	public Iterator<NGram> iterator() {
		return values.keySet().iterator();
	}
}