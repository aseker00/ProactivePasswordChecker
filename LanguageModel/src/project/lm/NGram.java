package project.lm;

public class NGram {
	
	private Gram[] grams;
	public NGram(int n) {
		grams = new Gram[n];
	}
	public void gram(int i, Gram g) {
		grams[i] = g;
	}
	public int length() {
		return grams.length;
	}
	public Gram gram(int i) {
		return grams[i];
	}
	
	public NGram sub(int offset, int len) {
		if (offset+len > grams.length)
			return null;
		NGram ng = new NGram(len);
		for (int i = 0; i < len; i++) {
			ng.gram(i, grams[offset+i]);
		}
		return ng;
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (!(other instanceof NGram))
			return false;
		NGram that = (NGram)other;
		return this.toString().equals(that.toString());
	}
	
	@Override
	public String toString() {
		String s = new String();
		for (int i = 0; i < grams.length; i++) {
			s += grams[i].toString();
		}
		return s;
	}
}
