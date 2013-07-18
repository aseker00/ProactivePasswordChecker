package project.lm;

import java.util.HashSet;
import java.util.Iterator;

public class Vocabulary {
	HashSet<Gram> grams;
	HashSet<Gram> mappedGrams;
	
	public Vocabulary() {
		this.grams = new HashSet<Gram>();
		int base = ' ';
		this.mappedGrams = new HashSet<Gram>();
		for (int i = 0; i < 95; i++) {
			add(new Gram((char)(base+i)));
		}
	}
	
	public void add(Gram g) {
		grams.add(g);
		mappedGrams.add(mapGram(g));
	}
	
	public Gram get(Gram g) {
		return mapGram(g);
	}
	
	public Iterator<Gram> iterator() {
		return this.mappedGrams.iterator();
	}
	
	public int size() {
		return mappedGrams.size();
	}
	
	private Gram mapGram(Gram g) {
		if (Character.isLetter(g.c()))
			return new Gram(Character.toLowerCase(g.c()));
		if (g.c() == ' ')
			return g;
		return Gram.OTHER;
	}
}