package project.lm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class Perplexity {
	
	private LanguageModel lm;
	
	public Perplexity(LanguageModel lm) {
		this.lm = lm;
	}
	
	public double test(File f) throws IOException {
		double l = 0.0;
		int M = 0;
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
		String line = null;
		while ((line = br.readLine()) != null) {
			try {
				l += lm.test(line);
				M++;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		br.close();
		l /= M;
		return Math.pow(2, -l);
	}
}
