
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.System;

/*We are currently assuming there is only one ROOT rule. Need to see and change later*/

/*Back Pointer Logic
 * Every rule  has 2 backpointers. 
 * For Predict rules,  they both are NULL.
 * For scan Rule, one will be itself/null and other will be the rule that  led  to scan
 * For attach , it would be that if Z -> a. has led X -> Y.Z to  become X->YZ., then X->YZ. will point to Z->a. and X-> Y.Z
 */

public class Earley {
	static ArrayList<ArrayList<DotRule>> cols; // each column is an arrayList
	static ArrayList<HashMap<DotRule, Integer>> hashIndexes; // to store indices for each column
	static HashMap<String, ArrayList<ArrayList<Integer>>> hashIndexes2;  //stores indices for attach rules 
	static HashMap<String, ArrayList<Rule>> rules; // stores rules from grammar
													// files
	static HashMap<String, ArrayList<Rule>> prefixTable;
	static HashMap<String, ArrayList<String>> leftParent;
	static HashMap<String, ArrayList<String>> leftAncestor;
	static int attach_nullCount = 0;

	/* Checks if a given token is a terminal or non-terminal */
	public static boolean isNonTerminal(HashMap<String, ArrayList<Rule>> rules, String token) {
		if (rules.containsKey(token))
			return true;
		else
			return false;
	}

	public static ArrayList<String> getSentences(String fileName) throws Exception {
		ArrayList<String> sentences = new ArrayList<String>();
		File inputFile = new File(fileName);
		BufferedReader br = new BufferedReader(new FileReader(inputFile));
		String curr = null;

		while ((curr = br.readLine()) != null) {
			if (curr.length() == 0) /* Skip empty sentences */
				continue;

			sentences.add(curr);
		}
		br.close();
		return sentences;

	}

	public static HashMap<String, ArrayList<Rule>> getRules(String fileName) throws Exception {
		rules = new HashMap<String, ArrayList<Rule>>();
		prefixTable = new HashMap<String, ArrayList<Rule>>();
		leftParent = new HashMap<String, ArrayList<String>>();

		File inputFile = new File(fileName);
		BufferedReader br = new BufferedReader(new FileReader(inputFile));
		String curr = null;

		while ((curr = br.readLine()) != null) {
			if (curr.length() == 0) /* Dont stop at a blank line */
				continue;
			Rule r = new Rule(curr);

			if (!prefixTable.containsKey(r.parts.get(0) + " " + r.parts.get(1))) //if rule is A->BC.. key is (A" "B)
			{
				ArrayList<Rule> ruleArr = new ArrayList<Rule>();
				ruleArr.add(r);
				prefixTable.put(r.parts.get(0) + " " + r.parts.get(1), ruleArr);

				if (!leftParent.containsKey(r.parts.get(1))) {
					ArrayList<String> ar = new ArrayList<String>();
					ar.add(r.parts.get(0));
					leftParent.put(r.parts.get(1), ar);
				} else {
					leftParent.get(r.parts.get(1)).add(r.getHead());
				}

			}

			else {
				prefixTable.get(r.parts.get(0) + " " + r.parts.get(1)).add(r);
			}

			if (rules.containsKey(r.getHead())) {
				rules.get(r.getHead()).add(r);
			} else {
				ArrayList<Rule> ar = new ArrayList<Rule>();
				ar.add(r);
				rules.put(r.getHead(), ar);
			}
		}
		br.close();
		return rules;

	}

	/*
	 * Adds a rule to the specified column and also updates the respective
	 * hashtable with its index
	 */

	public static void addRule(int colIdx, DotRule dotRule) {
		cols.get(colIdx).add(dotRule); // add rule to column
		hashIndexes.get(colIdx).put(dotRule, cols.get(colIdx).size() - 1); // add the index of the rule to hashtable

		if (!dotRule.isFinished()) {
			int ruleIndex = cols.get(colIdx).size() - 1;
			String nextToken = dotRule.nextToken();

			if (!hashIndexes2.containsKey(nextToken)) {
				ArrayList<ArrayList<Integer>> arr = new ArrayList<ArrayList<Integer>>();

				for (int i = 0; i < cols.size(); i++) {
					arr.add(new ArrayList<Integer>());
				}
				arr.get(colIdx).add(ruleIndex);
				hashIndexes2.put(dotRule.nextToken(), arr);
			} else {
				hashIndexes2.get(dotRule.nextToken()).get(colIdx).add(ruleIndex);
			}
		}
	}

	/*
	 * Adds predict rules to a column . Checks if they are there before adding
	 */

	public static void predict(String nextToken, int colIdx, HashMap<String, ArrayList<Rule>> rules) {
		ArrayList<String> selectNT = leftAncestor.get(nextToken);

		// ArrayList<Rule> ruleSet = rules.get(nextToken); //check for all rules
		// that start with that token
		ArrayList<Rule> ruleSet = new ArrayList<Rule>();
		if (selectNT != null) {
			for (String s : selectNT) {
				for (Rule r : prefixTable.get(nextToken + " " + s)) {
					ruleSet.add(r);
				}
			}
		}

		HashMap<DotRule, Integer> hashIndex = hashIndexes.get(colIdx);

		if (ruleSet.size() > 0) {
			Rule r1 = ruleSet.get(0);
			DotRule dr1 = new DotRule(r1, 0, colIdx);

			if (!hashIndex.containsKey(dr1)) //check if any one rule exists , if no, then add all rules
			{
				for (Rule r : ruleSet) // for each such rule
				{
					DotRule dr = new DotRule(r, 0, colIdx); //put DOT at 0th position and give current column number.
					addRule(colIdx, dr); // add rule to that column
				}
			}
		}
	}

	public static boolean scan(DotRule currRule, String currWord, int colIdx) {
		HashMap<DotRule, Integer> hashIndex = hashIndexes.get(colIdx + 1);
		assert (!currRule.isFinished()); // sanity check
		String token = currRule.nextToken(); // look at the terminal of the rule
		if (token.equals(currWord)) // check if the terminal is equal to the next input sequence
		{
			DotRule newRule = new DotRule(currRule); // create a deep copy of current rule , but increase the DOT position by 1
			newRule.dotPos += 1;

			newRule.bckPointer1 = currRule; // point to rule that led to scan
			newRule.bckPointer2 = null; // 2nd bck-pointer for scan is null

			if (!hashIndex.containsKey(newRule)) // only add Rule if it isn't
													// already there
			{
				addRule(colIdx + 1, newRule); // add the new rule in next column
				return true;
			} else // if there is already a duplicate scan rule
			{
				DotRule oldRule = cols.get(colIdx + 1).get(hashIndex.get(newRule));

				if (newRule.weight < oldRule.weight) // replace it only if newer
														// rule is better
				{
					cols.get(colIdx + 1).set(hashIndex.get(oldRule), null); 
					addRule(colIdx + 1, newRule); // add new Rule
					return true;
				} else {
					/*
					 * Typically a newer scan rule should always be lesser
					 * weight than its previous duplicate. Thats the reason we
					 * are getting duplicates. Just checking this condition
					 */
					assert (false);
				}

				return false;
			}
		} else // scan failure
		{
			return false;
		}
	}

	public static void attach(DotRule dr, int colIdx) {
		HashMap<DotRule, Integer> hashIndex = hashIndexes.get(colIdx);
		String token = dr.rule.getHead(); // find the LHS of completed Rule (Let it be A)
		double drWeight = dr.weight; // get Weight of completed rule to add to new rules being attached
		ArrayList<DotRule> prevCol = cols.get(dr.colNum);
		ArrayList<DotRule> currCol = cols.get(colIdx);
		if (hashIndexes2.containsKey(token)) {

			ArrayList<Integer> iter = hashIndexes2.get(token).get(dr.colNum);
			for (int i : iter) // for all rules in that column
			{
				DotRule tmp = prevCol.get(i);

				if (tmp == null) // if its null skip it
					continue;

				DotRule newRule = new DotRule(tmp); // make new Rule of form "A
													// ."
				newRule.dotPos += 1;
				newRule.weight += drWeight; // add weight of completed rule

				newRule.bckPointer1 = tmp;
				newRule.bckPointer2 = dr;
				if (!hashIndex.containsKey(newRule)) // add it to current column if not already there
				{
					addRule(colIdx, newRule);
				}

				else // if the rule already exists
				{
					DotRule drOld = currCol.get(hashIndex.get(newRule)); 
					assert (hashIndex.get(drOld) == hashIndex.get(newRule)); // sanity check

					if (newRule.weight < drOld.weight) // compare weights of two rules
					{
						attach_nullCount++;
						currCol.set(hashIndex.get(drOld), null);
						addRule(colIdx, newRule); 
					}

				}

			}
		}
	}

	public static void printTrace(ArrayList<Rule> finalRules, DotRule dr) {
		if (dr != null) {
			if (dr.isFinished()) {
				finalRules.add(dr.rule);
			}

			printTrace(finalRules, dr.bckPointer1);
			printTrace(finalRules, dr.bckPointer2);
		}

	}

	static int j = 0;

	public static void printParse(ArrayList<Rule> finalRules, int index) {

		j++;
		Rule curr = finalRules.get(index);

		if (index == finalRules.size() - 1) {
			System.out.print("(" + curr.getHead());
			for (int i = 1; i < curr.parts.size(); i++) {
				System.out.print(" " + curr.parts.get(i));
			}
			System.out.print(" )");
			return;
		}
		System.out.print("(" + curr.getHead() + " ");

		for (int i = 1; i < curr.parts.size(); i++) {
			if (!isNonTerminal(rules, curr.parts.get(i))) {
				System.out.print(curr.parts.get(i) + " ");
			}

			else if (curr.parts.get(i).equals(finalRules.get(j).getHead())) {
				printParse(finalRules, j);
			}

		}
		System.out.print(")");
	}

	public static void fillLeftAncestor(String token) {
		if (leftParent.get(token) == null)
			return;

		for (String s : leftParent.get(token)) {
			if (!leftAncestor.containsKey(s)) {
				ArrayList<String> ar = new ArrayList<String>();
				ar.add(token);
				leftAncestor.put(s, ar);
				fillLeftAncestor(s);
			} else {
				if (!leftAncestor.get(s).contains(token)) {
					leftAncestor.get(s).add(token);
				}
			}
		}
	}

	public static void main(String args[]) throws Exception {
		final String gramFile = args[0];
		final String senFile = args[1];
		rules = getRules(gramFile);
		ArrayList<String> sentences = getSentences(senFile);
		double predict_time = 0.0, attach_time = 0.0, scan_time = 0.0;
		leftAncestor = new HashMap<String, ArrayList<String>>();

		for (String s : sentences) {
			ArrayList<Rule> finalRules = new ArrayList<Rule>(); // stores the final rules of each parse

			String[] words = s.split("\\s+");
			int len = words.length;

			cols = new ArrayList<ArrayList<DotRule>>(); // initializing cols
			for (int i = 0; i < len + 1; i++) {
				ArrayList<DotRule> tmp = new ArrayList<DotRule>();
				cols.add(tmp);
			}

			hashIndexes = new ArrayList<HashMap<DotRule, Integer>>(); // initializing hashIndexes
			for (int i = 0; i < len + 1; i++) {
				HashMap<DotRule, Integer> tmp = new HashMap<DotRule, Integer>();
				hashIndexes.add(tmp);
			}

			hashIndexes2 = new HashMap<String, ArrayList<ArrayList<Integer>>>();

			for (Rule rootRule : rules.get("ROOT")) // adding all Rules that start with ROOT
			{
				DotRule rootDotRule = new DotRule(rootRule, 0, 0); 
				/* i.e the rule "0 ROOT ->.S". Both backPointers are null */
				addRule(0, rootDotRule); // adding the "0 ROOT -> .S" to Column 0

			}
			for (int i = 0; i <= words.length; i++)
			{

				if (i < words.length) {
					leftAncestor.clear();
					fillLeftAncestor(words[i]);
					// System.out.println(words[i]);
					// for(String s1 : leftAncestor.keySet())
					// {
					// for (String ss : leftAncestor.get(s1))
					// {
					// System.out.println(s1+"--"+ss);
					// }
					// }
					// System.out.println("*************************");
				}

				ArrayList<DotRule> currCol = cols.get(i);
				for (int j = 0; j < currCol.size(); j++) {

					DotRule currRule = currCol.get(j);
					if (currRule == null) // if its null, skip it
					{
						continue;
					}
					if (!currRule.isFinished()) // if rule is not yet done
					{
						String nextToken = currRule.nextToken();

						if (isNonTerminal(rules, nextToken)) {
							// double p1 = System.nanoTime();
							predict(nextToken, i, rules);

							// double p2 = System.nanoTime();

							// predict_time += (p2-p1);
						} else {
							// double s1 = System.nanoTime();

							if (i != words.length) // don't scan for the last
													// column as there is no
													// more input left
								scan(currRule, words[i], i);

							// double s2 = System.nanoTime();
							// scan_time += (s2-s1);
						}
					} else {
						// double a1 = System.nanoTime();
						attach(currRule, i);
						// double a2 = System.nanoTime();

						// attach_time += (a2-a1);
					}
				}
			}

			DotRule finalRule = null;
			int pass = 0;
			for (Rule rootRule : rules.get("ROOT")) {
				finalRule = new DotRule(rootRule, rootRule.parts.size() - 1, 0); //checks for ROOT -> S. in final column
				HashMap<DotRule, Integer> hashIndex = hashIndexes.get(words.length);

				if (hashIndex.containsKey(finalRule)) {
					printTrace(finalRules, cols.get(words.length).get(hashIndex.get(finalRule)));
					j = 0;
					printParse(finalRules, 0);

					System.out.println();

					System.out.println(cols.get(words.length).get(hashIndex.get(finalRule)).weight);
					pass = 1;
					break;
				}

			}
			if (pass == 0)
				System.out.println("NONE");

		}
	}
}
