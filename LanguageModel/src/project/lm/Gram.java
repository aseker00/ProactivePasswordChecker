package project.lm;

public class Gram {
	private char c;
	public static Gram START = new Gram((char)1);
	public static Gram STOP = new Gram((char)2);
	public static Gram OTHER = new Gram((char)3);
	public static Gram RARE = new Gram((char)4);
	public Gram(char c) {
		this.c = c;
	}
	public char c() {
		return c;
	}
	@Override
	public int hashCode() {
		return (int)c;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (!(other instanceof Gram))
			return false;
		Gram that = (Gram)other;
		return this.c() == that.c();
	}
	
	@Override
	public String toString() {
		String s = new String();
		s += c;
		return s;
	}
}
