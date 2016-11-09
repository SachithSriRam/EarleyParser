import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

class Rule {
	ArrayList<String> parts;
	String aux;
	double size;
	double weight;

	String getHead() {
		return parts.get(0);
	}

	Rule(String line) {
		this.parts = new ArrayList<String>(); // init arrayList

		String[] tokens = line.split("\\s+");
		String[] auxs = line.split("\\s+", 2);
		this.aux = auxs[1];
		// System.out.println(this.aux);
		double probability = Double.parseDouble(tokens[0]);

		this.weight = -(Math.log(probability) / Math.log(2)); // weight is
																// -log2Prob

		for (int i = 1; i < tokens.length; i++) {
			parts.add(tokens[i]);
		}
		this.size = parts.size();

		assert (this.size == tokens.length - 1); // sanity check that all inputs
													// are read properly
	}

	@Override
	public boolean equals(Object otherRule) {

		if (!(otherRule instanceof Rule))
			return false;

		Rule other = (Rule) otherRule;

		for (int i = 0; i < this.parts.size(); i++) {
			if (!this.parts.get(i).equals(other.parts.get(i)))
				return false;
		}

		assert (this.weight == other.weight);
		return true;

	}

	/*
	 * @Override public int hashCode() //to set rule as key to hashTable { int
	 * result = 17; final int prime = 31; for(String s : this.parts) { result =
	 * result*prime + s.hashCode(); } return result; }
	 */
	@Override
	public int hashCode() // to set rule as key to hashTable
	{
		return this.aux.hashCode();
	}

	@Override
	public String toString() // debug
	{
		String s = this.weight + " " + this.getHead() + " --> ";
		for (int i = 1; i < parts.size(); i++) {
			s = s + parts.get(i) + " ";
		}
		return s;
	}

}

public class DotRule {
	Rule rule;
	int dotPos;
	int colNum;
	double weight; // what should be its value. Currently setting to rule.weight
	DotRule bckPointer1;
	DotRule bckPointer2;
	int identifier; //// switch this value to 1 even if one non-terminal on
					//// right i.e. predict done using this rule

	DotRule(Rule rule, int dotPos, int colNum) {
		this.rule = rule;
		this.dotPos = dotPos;
		this.colNum = colNum;
		this.weight = this.rule.weight; 
		/* initially weight of a rule is its own weight. This is our convention.
		 *  By this, newly predicted rules A-> . B C D have weight = -log2Prob of that rule */
		this.bckPointer1 = null; // set backpointers to null
		this.bckPointer2 = null; // set backpointers to null
		this.identifier = 0;
		assert (this.dotPos <= this.rule.size - 1);
	}

	/* Explicity specifying the copy constructor */
	DotRule(DotRule other) {
		this.rule = other.rule;
		this.dotPos = other.dotPos;
		this.colNum = other.colNum;
		this.weight = other.weight;
		this.bckPointer1 = null; // backpointers are not copied
		this.bckPointer2 = null; // backpointers are not copied
		this.identifier = other.identifier;

		assert (this.dotPos <= this.rule.size - 1);
	}

	boolean isFinished() // check if the DOT is at the end of the rule
	{
		if (dotPos == rule.parts.size() - 1)
			return true;
		else
			return false;
	}

	/* Returns the next token after DOT */
	String nextToken() {
		if (this.isFinished())
			return null;
		else
			return this.rule.parts.get(this.dotPos + 1);
	}

	@Override
	public boolean equals(Object otherRule) {
		if (!(otherRule instanceof DotRule))
			return false;

		DotRule other = (DotRule) otherRule;

		if (this.colNum != other.colNum)
			return false;

		if (this.dotPos != other.dotPos)
			return false;

		if (!this.rule.equals(other.rule))
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = 17;
		final int prime = 31;

		result = result * prime + this.colNum;
		result = result * prime + this.dotPos;
		result = result * prime + this.rule.hashCode();

		return result;
	}

	@Override
	public String toString() {
		String s = this.colNum + " " + this.rule.getHead() + " --> ";
		for (int i = 1; i < this.rule.parts.size(); i++) {
			if (this.dotPos == i - 1)
				s = s + ". ";

			s = s + this.rule.parts.get(i) + " ";
		}
		if (this.dotPos == this.rule.parts.size() - 1)
			s = s + ".";

		s = s + " (" + this.weight + ") "; // add Rule weight also

		return s;
	}

	
}
