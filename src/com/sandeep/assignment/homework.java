package com.sandeep.assignment;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class homework {

	private int numberOfKbStatements = 0;
	private List<String> kbStrings = new ArrayList<>();
	private List<String> queries = new ArrayList<>();
	private int numOfQueries = 0;
	private List<String> solutions = new ArrayList<>();
	private Map<String, Partition> kb = new HashMap<>();

	public static void main(String[] args) {
		homework homework = new homework();
		homework.run();
		// homework.test();
	}

	private void run() {
		readInput();
		// printInput();
		createKb();
		// printKb();
		findSolution();
		processOutput();
	}

	private Map<String, Partition> deepCopy(Map<String, Partition> sourcekb) {

		Map<String, Partition> copiedKb = new HashMap<>();
		sourcekb.forEach((k, v) -> {
			copiedKb.put(k, new Partition(cloneCompoundSentenceList(v.getPositiveSentences()),
					cloneCompoundSentenceList(v.getNegativeSentences())));
		});
		return copiedKb;
	}

	private void createKb() {
		List<CompoundSentence> compoundSentenceList = new ArrayList<>();
		for (String sentence : kbStrings) {
			String[] predicates = sentence.split("\\|");
			CompoundSentence compoundSentence = processCompoundSentence(predicates, sentence);
			compoundSentenceList.add(compoundSentence);
		}
		createPartionedKb(compoundSentenceList);
	}

	private void createPartionedKb(List<CompoundSentence> compoundSentenceList) {

		for (CompoundSentence compoundSentence : compoundSentenceList) {
			List<Predicate> predicateList = compoundSentence.getCompoundSentence();
			for (Predicate predicate : predicateList) {
				if (kb.get(predicate.getFunctionName()) == null) {
					Partition partition = new Partition(new ArrayList<CompoundSentence>(),
							new ArrayList<CompoundSentence>());
					kb.put(predicate.getFunctionName(), partition);
				}
				if (predicate.isNegated()) {
					kb.get(predicate.getFunctionName()).getNegativeSentences().add(compoundSentence);
				} else {
					kb.get(predicate.getFunctionName()).getPositiveSentences().add(compoundSentence);
				}
			}
		}
	}

	private CompoundSentence processCompoundSentence(String[] predicates, String sentence) {
		CompoundSentence compoundSentence = new CompoundSentence();
		List<Predicate> predicateList = new ArrayList<>();
		for (String predicateString : predicates) {
			Predicate predicate = processPredicateString(predicateString);
			predicateList.add(predicate);
		}
		compoundSentence.setCompoundSentence(predicateList);
		return compoundSentence;
	}

	private Predicate processPredicateString(String predicateString) {
		boolean isNegated = true;
		int openingBraceLocation;
		int closingBraceLocation;
		String function;
		List<String> args = new ArrayList<>();

		predicateString = predicateString.trim();
		if (predicateString.charAt(0) == '~') {
			isNegated = true;
			predicateString = predicateString.substring(1, predicateString.length());
		} else {
			isNegated = false;
		}
		openingBraceLocation = predicateString.indexOf('(');
		closingBraceLocation = predicateString.indexOf(')');
		function = predicateString.substring(0, openingBraceLocation);

		args = new ArrayList<>(
				Arrays.asList(predicateString.substring(openingBraceLocation + 1, closingBraceLocation).split(",")));

		return new Predicate(isNegated, function, args);
	}

	private Predicate negate(Predicate predicate) {
		predicate.setNegated(!predicate.isNegated());
		return predicate;
	}

	private void findSolution() {
		// System.out.println("Before");
		// printKb();
		for (String query : queries) {
			Map<String, Partition> copiedKb = deepCopy(kb);
			// System.out.println("size of kb " + kb.size());
			// System.out.println("size of copied kb " + copiedKb.size());
			Predicate queryPredicate = processPredicateString(query);
			queryPredicate = negate(queryPredicate);
			// System.out.println("Query predicate is ");
			// printPredicate(queryPredicate);
			List<Predicate> queryPredicateList = new ArrayList<>();
			queryPredicateList.add(queryPredicate);
			CompoundSentence querySentence = new CompoundSentence(queryPredicateList);
			boolean solution = resolution(querySentence, copiedKb);
			solutions.add(solution ? "TRUE" : "FALSE");
			// System.out.println("solution is " + solution);
		}
		// System.out.println("After");
		// printKb();
		// System.out.println("size of kb after " + kb.size());
	}

	private Map<String, Partition> tellKb(Map<String, Partition> copiedKb, CompoundSentence query) {
		List<Predicate> predicateList = query.getCompoundSentence();
		for (Predicate predicate : predicateList) {
			if (copiedKb.get(predicate.getFunctionName()) == null) {
				Partition partition = new Partition(new ArrayList<CompoundSentence>(),
						new ArrayList<CompoundSentence>());
				copiedKb.put(predicate.getFunctionName(), partition);
			}
			if (predicate.isNegated()) {
				copiedKb.get(predicate.getFunctionName()).getNegativeSentences().add(query);
			} else {
				copiedKb.get(predicate.getFunctionName()).getPositiveSentences().add(query);
			}
		}
		return copiedKb;
	}

	private boolean resolution(CompoundSentence querySentence, Map<String, Partition> copiedKb) {

		boolean answer = false;
		Map<String, Boolean> resolvedList = new HashMap<>();
		resolvedList.put(querySentence.toString(), true);
		List<CompoundSentence> dfsList = new ArrayList<>();
		copiedKb = tellKb(copiedKb, querySentence);
		dfsList.add(querySentence);
		int iteration = 0;
		while (!dfsList.isEmpty() && iteration < 1000) {
			iteration++;
			// System.out.println("Iteration is" + iteration);
			int index = pickOneWithLeastNumberOfPredicates(dfsList);
			CompoundSentence currentSentence = dfsList.remove(index);
			List<Predicate> sortedPredicateList = currentSentence.getCompoundSentence();
			for (int i = 0; i < sortedPredicateList.size(); i++) {
				Predicate predicate = sortedPredicateList.get(i);
				List<Predicate> remainingPredicateListinQuery = getRemainingPredicateList(sortedPredicateList, i);
				List<CompoundSentence> matchedSentences = getMatchedSentences(predicate, copiedKb);
				for (CompoundSentence compoundSentence : matchedSentences) {
					int unifiedListIndex = getUnifiedPredicateFromSentence(predicate, compoundSentence);
					Predicate matchedPredicate = compoundSentence.getCompoundSentence().get(unifiedListIndex);
					List<Predicate> unmatchedPredicatedListInSentence = getRemainingPredicateList(
							compoundSentence.getCompoundSentence(), unifiedListIndex);

					Map<String, String> theta = new HashMap<>();
					theta = unify(predicate, matchedPredicate, theta);
					theta = handleLoops(theta);

					// printUnification(currentSentence, compoundSentence, predicate,
					// matchedPredicate, theta);

					if (theta != null && remainingPredicateListinQuery.isEmpty()
							&& unmatchedPredicatedListInSentence.isEmpty()) {
						answer = true;
						return answer;
					} else if (theta != null) {
						List<Predicate> additionalList = new ArrayList<>();
						additionalList.addAll(remainingPredicateListinQuery);
						additionalList.addAll(unmatchedPredicatedListInSentence);
						CompoundSentence resolvedSentence = substitute(additionalList, theta);
						resolvedSentence = getStandardSentence(resolvedSentence);
						// System.out.println("Resolved:[" + resolvedSentence.toString() + "]");

						if (resolvedList.get(resolvedSentence.toString()) == null) {
							dfsList.add(resolvedSentence);
							tellKb(copiedKb, resolvedSentence);
							resolvedList.put(resolvedSentence.toString(), true);
						}
					}
					// printList(dfsList);
					// printMap(resolvedList);
				}
			}
			// System.out.println(
			// "******************************************************************************************************************");
		}
		return answer;
	}

	private void printMap(Map<String, Boolean> resolvedList) {

		System.out.print("Current visited");
		resolvedList.forEach((k, v) -> {
			System.out.print("[");
			System.out.print(k);
			System.out.print("]");
			System.out.print(",");
		});
		System.out.println();
		System.out.println(
				"-----------------------------------------------------------------------------------------------------------------");
	}

	private void printList(List<CompoundSentence> dfsList) {

		System.out.print("Current Stack:");
		for (CompoundSentence compoundSentence : dfsList) {
			System.out.print("[");
			System.out.print(compoundSentence.toString());
			System.out.print("]");
			System.out.print(",");
		}
		System.out.println();
	}

	private void printUnification(CompoundSentence currentSentence, CompoundSentence compoundSentence,
			Predicate predicate, Predicate matchedPredicate, Map<String, String> theta) {

		System.out.print("Unification: ");
		System.out.print("[" + currentSentence.toString() + "] ");
		System.out.print("[" + compoundSentence.toString() + "] ");
		System.out.print("[" + predicate.toString() + "] ");
		System.out.print("[" + matchedPredicate.toString() + "] ");
		printTheta(theta);
		System.out.println();
	}

	private void printTheta(Map<String, String> theta) {

		if (theta == null) {
			System.out.print("{ None }");
		} else {
			theta.forEach((k, v) -> {
				System.out.print("{");
				System.out.print(k + ":" + v);
				System.out.print(",");
				System.out.print("}");
			});
		}
	}

	private int pickOneWithLeastNumberOfPredicates(List<CompoundSentence> dfsList) {

		int index = 0;
		int numberOfelements = Integer.MAX_VALUE;
		for (int i = 0; i < dfsList.size(); i++) {
			if (dfsList.get(i).getCompoundSentence().size() < numberOfelements) {
				index = i;
				numberOfelements = dfsList.get(index).getCompoundSentence().size();
			}
		}

		return index;
	}

	private CompoundSentence getStandardSentence(CompoundSentence resolvedSentence) {

		List<Predicate> predicates = resolvedSentence.getCompoundSentence();

		Collections.sort(predicates, new Comparator<Predicate>() {
			@Override
			public int compare(Predicate o1, Predicate o2) {
				return o1.getFunctionName().compareTo(o2.getFunctionName());
			}
		});

		resolvedSentence.setCompoundSentence(predicates);
		return resolvedSentence;
	}

	private Map<String, String> handleLoops(Map<String, String> theta) {

		if (theta != null) {
			int count = 1;
			int limit = 100;

			while (count < limit) {
				boolean isCycleThetaExists = false;
				theta.forEach((k, v) -> {
					if (theta.get(v) != null) {
						theta.put(k, theta.get(v));
						makebooleanTrue(isCycleThetaExists);
					}
				});
				if (!isCycleThetaExists) {
					break;
				}
			}
			return theta;
		}
		return null;
	}

	private boolean makebooleanTrue(boolean isCycleThetaExists) {
		return isCycleThetaExists = true;

	}

	private CompoundSentence substitute(List<Predicate> remainingPredicateListinQuery, Map<String, String> theta) {

		List<Predicate> list1 = new ArrayList<>();
		for (Predicate predicate : remainingPredicateListinQuery) {
			List<String> args1 = new ArrayList<>();
			List<String> arguments = predicate.getArguments();
			for (String string : arguments) {
				if (theta.get(string) != null) {
					args1.add(theta.get(string));
				} else {
					args1.add(string);
				}
			}
			list1.add(new Predicate(predicate.isNegated(), predicate.getFunctionName(), args1));
		}
		return new CompoundSentence(list1);
	}

	private int getUnifiedPredicateFromSentence(Predicate predicate, CompoundSentence compoundSentence) {

		List<Predicate> predicateList = compoundSentence.getCompoundSentence();

		int x = -1;
		for (int i = 0; i < predicateList.size(); i++) {
			if (predicateList.get(i).getFunctionName().equals(predicate.getFunctionName())) {
				x = i;
			}
		}
		return x;
	}

	private List<Predicate> getRemainingPredicateList(List<Predicate> sortedPredicateList, int i) {

		List<Predicate> clonedList = cloneList(sortedPredicateList);
		clonedList.remove(i);
		return clonedList;
	}

	private List<Predicate> cloneList(List<Predicate> baseList) {

		List<Predicate> newList = new ArrayList<>();
		for (int i = 0; i < baseList.size(); i++) {
			newList.add(baseList.get(i));
		}
		return newList;
	}

	private List<CompoundSentence> cloneCompoundSentenceList(List<CompoundSentence> baseList) {

		List<CompoundSentence> newList = new ArrayList<>();
		for (int i = 0; i < baseList.size(); i++) {
			newList.add(baseList.get(i));
		}
		return newList;
	}

	private List<CompoundSentence> getMatchedSentences(Predicate predicate, Map<String, Partition> copiedKb) {

		if (predicate.isNegated()) {
			return copiedKb.get(predicate.getFunctionName()).getPositiveSentences();
		} else {
			return copiedKb.get(predicate.getFunctionName()).getNegativeSentences();
		}
	}

	private Map<String, String> unify(Object x, Object y, Map<String, String> theta) {
		if (theta == null)
			return null;
		else if (checkClassEquivalence(x, y))
			return theta;
		else if (isConstant(x) && isConstant(y) && !x.equals(y)) {
			return null;
		} else if (isVariable(x)) {
			return unifyVar(x, y, theta);
		} else if (isVariable(y)) {
			return unifyVar(y, x, theta);
		} else if (x instanceof Predicate && y instanceof Predicate) {
			Predicate p = (Predicate) x;
			Predicate s = (Predicate) y;
			return unify(p.getArguments(), s.getArguments(), unify(p.getFunctionName(), s.getFunctionName(), theta));
		} else if (x instanceof List<?> && y instanceof List<?>) {
			List<String> firstList = (List<String>) x;
			List<String> secondList = (List<String>) y;
			return unifyList(firstList, secondList, theta);
		} else {
			return null;
		}
	}

	private Map<String, String> unifyList(List<String> list1, List<String> list2, Map<String, String> theta) {

		if (theta == null)
			return null;
		if (list1.size() != list2.size())
			return null;

		String s1 = list1.get(0);
		String s2 = list2.get(0);

		if (list1.size() == 1 && list2.size() == 1) {
			return unify(s1, s2, theta);
		}
		return unifyList(list1.subList(1, list1.size()), list2.subList(1, list2.size()), unify(s1, s2, theta));
	}

	private boolean checkClassEquivalence(Object x, Object y) {

		if (x.getClass().getName().equals(y.getClass().getName()) && x.equals(y)) {
			return true;
		}
		return false;
	}

	private Map<String, String> unifyVar(Object variable, Object sentence, Map<String, String> theta) {

		if (theta.get(variable.toString()) != null) {
			return unify(theta.get(variable.toString()), sentence, theta);
		} else if (theta.get(sentence.toString()) != null) {
			return unify(variable, theta.get(sentence.toString()), theta);
		} else if (occurCheck(variable, sentence)) {
			return theta;
		} else {
			theta.put(variable.toString(), sentence.toString());
			return theta;
		}
	}

	private boolean occurCheck(Object variable, Object sentence) {
		return false;
	}

	private void printKb() {

		kb.forEach((k, v) -> {
			System.out.println("=============================================================================");
			System.out.println("KeyName");
			System.out.println(k);
			System.out.println("Positive Sentences");
			printCompoundSentence(v.getPositiveSentences());
			System.out.println("Negative sentence");
			printCompoundSentence(v.getNegativeSentences());
		});
	}

	private void printCompoundSentence(List<CompoundSentence> compoundSentenceList) {
		for (CompoundSentence compoundSentence : compoundSentenceList) {
			for (Predicate predicate : compoundSentence.getCompoundSentence()) {
				printPredicate(predicate);
			}
			System.out.println();
		}
	}

	private void printPredicate(Predicate predicate) {
		System.out.print(predicate.isNegated() ? "~" : "");
		System.out.print(predicate.getFunctionName() + "(");
		for (String string : predicate.getArguments()) {
			System.out.print(string + ",");
		}
		System.out.print(")v");
	}

	private void processOutput() {
		for (String string : solutions) {
			System.out.println(string);
		}
		Path file = Paths.get("output.txt");
		try {
			Files.write(file, solutions, Charset.forName("UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Failed to write output");
		}
	}

	private void printInput() {
		System.out.println("Number of statements in Kb " + numberOfKbStatements);
		for (String string : kbStrings) {
			System.out.println(string);
		}
		System.out.println("Number of queries in kb " + numOfQueries);

		for (String string : queries) {
			System.out.println(string);
		}
	}

	private void readInput() {
		Stream<String> stream = null;
		try {
			String cur_dir = System.getProperty("user.dir");
			List<String> inputLines = new ArrayList<>();
			stream = Files.lines((Paths.get(cur_dir + "/input.txt")));
			inputLines = stream.collect(Collectors.toList());
			processData(inputLines);
		} catch (IOException e) {
			System.out.println("IO exception");
			e.printStackTrace();
		} finally {
			stream.close();
		}
	}

	private void processData(List<String> inputLines) {
		numOfQueries = Integer.parseInt(inputLines.remove(0));
		int i;
		for (i = 0; i < numOfQueries; i++) {
			queries.add(inputLines.get(i));
		}
		numberOfKbStatements = Integer.parseInt(inputLines.get(i));
		for (int j = i + 1; j < i + numberOfKbStatements + 1; j++) {
			kbStrings.add(inputLines.get(j));
		}
	}

	private void printUnification(Predicate predicate, Predicate matchedPredicate, Map<String, String> theta) {

		System.out.print(predicate.isNegated() ? "~" : "");
		System.out.print(predicate.getFunctionName() + "(");
		for (String string : predicate.getArguments()) {
			System.out.print(string + ",");
		}
		System.out.print(")");

		System.out.println();

		System.out.print(matchedPredicate.isNegated() ? "~" : "");
		System.out.print(matchedPredicate.getFunctionName() + "(");
		for (String string : matchedPredicate.getArguments()) {
			System.out.print(string + ",");
		}
		System.out.print(")");

		theta.forEach((k, v) -> {
			System.out.println(k + v);
		});

	}

	private boolean isConstant(Object object) {

		if (object instanceof Predicate) {
			return false;
		}
		String string = object.toString();
		if (Character.isUpperCase(string.charAt(0)) && !(string.contains("(") || string.contains(")"))) {
			return true;
		}
		return false;
	}

	private boolean isVariable(Object object) {

		if (object instanceof Predicate) {
			return false;
		}
		String string = object.toString();
		if (Character.isLowerCase(string.charAt(0)) && !(string.contains("(") || string.contains(")"))) {
			return true;
		}
		return false;
	}

	class CompoundSentence {

		private List<Predicate> compoundSentence;

		public CompoundSentence() {

		}

		public CompoundSentence(List<Predicate> compoundSentence) {
			this.compoundSentence = compoundSentence;
		}

		public List<Predicate> getCompoundSentence() {
			return compoundSentence;
		}

		public void setCompoundSentence(List<Predicate> compoundSentence) {
			this.compoundSentence = compoundSentence;
		}

		public String toString() {

			StringBuilder stringBuilder = new StringBuilder();
			List<Predicate> predicates = this.getCompoundSentence();

			for (Predicate predicate : predicates) {
				stringBuilder.append(",");
				stringBuilder.append(predicate.toString());
			}

			return stringBuilder.toString();
		}
	}

	class Partition {

		private List<CompoundSentence> positiveSentences;
		private List<CompoundSentence> negativeSentences;

		public Partition(List<CompoundSentence> positiveSentences, List<CompoundSentence> negativeSentences) {
			super();
			this.positiveSentences = positiveSentences;
			this.negativeSentences = negativeSentences;
		}

		public List<CompoundSentence> getPositiveSentences() {
			return positiveSentences;
		}

		public void setPositiveSentences(List<CompoundSentence> positiveSentences) {
			this.positiveSentences = positiveSentences;
		}

		public List<CompoundSentence> getNegativeSentences() {
			return negativeSentences;
		}

		public void setNegativeSentences(List<CompoundSentence> negativeSentences) {
			this.negativeSentences = negativeSentences;
		}
	}

	class Predicate {
		private boolean isNegated;
		private String functionName;
		private List<String> arguments;

		public Predicate(boolean isNegated, String functionName, List<String> arguments) {
			super();
			this.isNegated = isNegated;
			this.functionName = functionName;
			this.arguments = arguments;
		}

		public Predicate() {

		}

		public boolean isNegated() {
			return isNegated;
		}

		public void setNegated(boolean isNegated) {
			this.isNegated = isNegated;
		}

		public String getFunctionName() {
			return functionName;
		}

		public void setFunctionName(String functionName) {
			this.functionName = functionName;
		}

		public List<String> getArguments() {
			return arguments;
		}

		public void setArguments(List<String> arguments) {
			this.arguments = arguments;
		}

		@Override
		public String toString() {
			StringBuilder stringBuilder = new StringBuilder();
			if (this.isNegated) {
				stringBuilder.append("~");
			}
			stringBuilder.append(this.getFunctionName());
			List<String> argumentList = this.getArguments();

			for (String string : argumentList) {
				stringBuilder.append(",");
				stringBuilder.append(string);
			}
			return stringBuilder.toString();
		}
	}
}
