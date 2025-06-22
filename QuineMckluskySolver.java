import java.util.HashSet;
import java.util.LinkedList;

// This class describes a Boolean Term like "0011" or "00-1" and '-' can be any char to be considered a "Dont-Care"
class Term {
    StringBuilder minTerm;
    int numberOfOnes;
    boolean isChecked; // may be used in validation
    boolean isDontCare;

    // Create a Term from a Boolean String
    public Term(String minterm){
        minTerm = new StringBuilder(minterm);
        numberOfOnes = 0;
        for(char c : minTerm.toString().toCharArray()){
            if(c == '1') numberOfOnes++;
        }
        isChecked = false;
    }
    // Converts a specified bit at "bitIndex" to a "Dont-Care"
    public void toDontCare(int bitIndex){
        if(bitIndex >= minTerm.length()){
            System.err.println("BitIndex-OutOfBoundsError");
            return;
        }
        if (minTerm.charAt(bitIndex) == '1') {
            numberOfOnes--;
        }
        minTerm.setCharAt(bitIndex,'-');
    }

    // Count the number of non 1 nor 0 chars (number of don't-cares)
    public int countDontCareVars(){
        int count = 0;
        for (char var : this.minTerm.toString().toCharArray()) {
            count += (var != '1' && var != '0')? 1: 0;
        }
        return count;
    }

    // Create a copy of a Term
    public static Term cpyTerm (Term term){
        Term newTerm = new Term(term.minTerm.toString());
        return newTerm;
    }

    // Find the first differing bit between two terms
    public static int findFirstDiffBet(Term term1, Term term2){
        if (term1.minTerm.length() != term2.minTerm.length()) {
            System.err.println("ComparingTwoTermsOfMismatchingLengths");
            return -1;
        }
        for(int i = 0; i < term1.toString().length(); i++){
            if (term1.minTerm.charAt(i) != term2.minTerm.charAt(i))
                return i;
        }
        return -1;
    }

    // Count the number of differing bits between two terms
    public static int countDiffBet(Term term1, Term term2){
        if (term1.minTerm.length() != term2.minTerm.length()) {
            System.err.println("ComparingTwoTermsOfMismatchingLengths");
            return -1;
        }
        int count = 0;
        for(int i = 0; i < term1.minTerm.length(); i++){
            if (term1.minTerm.charAt(i) != term2.minTerm.charAt(i))
                count++;
        }
        return count;
    }

    public boolean isCoveredBy(Term term){
        if (term.minTerm.length() != this.minTerm.length()) return false;
        for (int i = 0; i < term.minTerm.length(); i++) {
            char c1 = term.minTerm.charAt(i);
            char c2 = this.minTerm.charAt(i);
            if (c1 != '-'  && c1 != c2) {
                return false;
            }
        }
        return true;
    }
    
    public boolean ContainsTermToOptimize (Term term){
        int diffCount = 0;
        if (term.minTerm.length() != this.minTerm.length()) return false;
        for (int i = 0; i < term.minTerm.length(); i++) {
            char c1 = this.minTerm.charAt(i);
            char c2 = term.minTerm.charAt(i);
            if (c1 != '-' ) {
                if(c2 == '-') 
                    return false; //second is more general in a bit
                else if (c2 != c1) 
                    diffCount++;
            }
        }
        return (diffCount <= 1);
    }
    public static int findFirstMainBitsDiffBet(Term coveringTerm, Term coveredTerm) {
        if (coveringTerm.minTerm.length() != coveredTerm.minTerm.length()) {
            System.err.println("ComparingTwoTermsOfMismatchingLengths");
            return -1;
        }
        for(int i = 0; i < coveredTerm.toString().length(); i++){
            char c1 = coveringTerm.minTerm.charAt(i);
            char c2 = coveredTerm.minTerm.charAt(i);
            if (c1 != '-'  && c1 != c2)
                return i;
        }
        return -1;
    }
    public static Term GetTheMoreCoveringTerm(Term term1, Term term2){
        if (term1.minTerm.length() != term2.minTerm.length()) return null;
        if (term1.countDontCareVars() > term2.countDontCareVars()) {
            return term1;
        }else {
            return term2;
        }
    }
    // Recursively generate all minterms covered by a pattern with '-'
    public static void generateMinterms(String pattern, int idx, StringBuilder current, HashSet<String> result) {
        if (idx == pattern.length()) {
            result.add(current.toString());
            return;
        }
        char c = pattern.charAt(idx);
        if (c == '-') {
            current.append('0');
            generateMinterms(pattern, idx + 1, current, result);
            current.setCharAt(current.length() - 1, '1');
            generateMinterms(pattern, idx + 1, current, result);
            current.deleteCharAt(current.length() - 1);
        } else {
            current.append(c);
            generateMinterms(pattern, idx + 1, current, result);
            current.deleteCharAt(current.length() - 1);
        }
    }

    // Convert an integer to a binary string of targetSize length
    public static String minTermToString(int num , int targetSize){

        String term = "";
        while (num >= 1) {
            term = num%2 + term;
            num /= 2;
        }
        while (term.length() < targetSize) {
            term = '0' + term;
        }
        return term;
    }
}

// Group of terms with the same number of 1's
class Group {
    int numberOfOnes;
    LinkedList<Term> terms;
    public Group(int number){
        numberOfOnes = number;
        terms = new LinkedList<Term>();
    }
    public void insertTerm(Term term){
        terms.push(term);
    }
}

// Represents a minimization step (iteration)
class iteration {
    int iterationNumber;
    Group groups[]; // not linked since we know the size and we will traverse back and forth

    public iteration (int numberOfGroups, int iterNumber){
        iterationNumber = iterNumber;
        groups = new Group[numberOfGroups];
        for (int i = 0; i < numberOfGroups;i++) {
            groups[i] = new Group(i);
        }
    }
    public int GetNumberOfTerms(){ //Returns the number of terms in this iteration
        int count = 0;
        for (Group group : groups) {
            count += group.terms.size();
        }
        return count;
    }
    public boolean Contains(Term target){
        for (Group group : this.groups) {
            for (Term term : group.terms) {
                if (term.minTerm.toString().equals(target.minTerm.toString())) return true;
            }
        }
        return false;
    }
    
    public void SetUpGroupsFromArrays(String[] Terms, String[] DontCares){
        for(String term : Terms){
            Term newTerm = new Term(term.trim());
            int insertPos = newTerm.numberOfOnes;
            this.groups[insertPos].terms.push(newTerm);
        }
        // Add don't-cares to their groups
        if(DontCares != null) {
            for(String term : DontCares){
                Term newTerm = new Term(term.trim());
                newTerm.isDontCare = true;
                int insertPos = newTerm.numberOfOnes;
                this.groups[insertPos].terms.push(newTerm);
            }
        }
        
    }
    public void SetUpGroupsFromArrays(int[] Terms, int[] DontCares, int numberOfVariables){
        for(int term : Terms){
            Term newTerm = new Term(Term.minTermToString(term, numberOfVariables));
            int insertPos = newTerm.numberOfOnes;
            this.groups[insertPos].terms.push(newTerm);
        }
        // Add don't-cares to their groups
        if (DontCares != null) {
            for(int term : DontCares){
                Term newTerm = new Term(Term.minTermToString(term, numberOfVariables));
                newTerm.isDontCare = true;
                int insertPos = newTerm.numberOfOnes;
                this.groups[insertPos].terms.push(newTerm);
            }
        }
        
    }
    
    public int MinimizeByAdjacentGroup(int GroupIndex,iteration nextIteration){ //returns number of minimizations done
        int numberOfMinimizations = 0;
        for (Term term1 : this.groups[GroupIndex].terms) {
            for(Term term2  : this.groups[GroupIndex + 1].terms){
                if (Term.countDiffBet(term1,term2) == 1) {
                    Term newTerm = Term.cpyTerm(term1);
                    int diffBitIndex = Term.findFirstDiffBet(term1,term2);
                    newTerm.toDontCare(diffBitIndex);
                    newTerm.isDontCare = (term1.isDontCare && term2.isDontCare);
                    int insertPos = newTerm.numberOfOnes;
                    nextIteration.groups[insertPos].terms.push(newTerm);
                    term1.isChecked = term2.isChecked = true;
                    numberOfMinimizations++;
                }
            }
        }
        return numberOfMinimizations;
    }
    public int MinimizeByComparingFollowingGroups(int GroupIndex, iteration nextIteration){
        // System.out.println("Starting minimization");
        int numberOfMinimizations = 0;
        Group currGroup = this.groups[GroupIndex];
        for (Term term : currGroup.terms) {
            for (Group group : this.groups) {
                if (group.numberOfOnes < currGroup.numberOfOnes) continue;
                for (Term compTerm : group.terms) {
                    if (term == compTerm) continue;
                    
                    if (Term.countDiffBet(compTerm, term) == 1) { // regular minimization step
                        int bitIndex = Term.findFirstDiffBet(term,compTerm);
                        Term newTerm = Term.cpyTerm(term);
                        newTerm.toDontCare(bitIndex);
                        newTerm.isDontCare = (term.isDontCare && compTerm.isDontCare);
                        term.isChecked = compTerm.isChecked = true;
                        int insertPos = newTerm.numberOfOnes;
                        if (!nextIteration.Contains(newTerm)) {
                            nextIteration.groups[insertPos].terms.push(newTerm); 
                        }
                        numberOfMinimizations++;
                        System.out.println(newTerm.minTerm + " Added By " + term.minTerm + " and " + compTerm.minTerm);
                    }
                    else if (term == Term.GetTheMoreCoveringTerm(term, compTerm)) { // if first covers second
                        if (compTerm.isCoveredBy(term)) {
                            compTerm.isChecked = true; //but we wont add it since it's fully covered
                            term.isDontCare = term.isDontCare && compTerm.isDontCare;
                            
                            System.out.println("Term1 " + term.minTerm + " fully covers " + compTerm.minTerm);
                        }else if (term.ContainsTermToOptimize(compTerm)){
                            int bitIndex = Term.findFirstMainBitsDiffBet(term,compTerm);
                            Term newTerm = Term.cpyTerm(compTerm);
                            newTerm.toDontCare(bitIndex);
                            newTerm.isDontCare = (term.isDontCare && compTerm.isDontCare);
                            int insertPos = newTerm.numberOfOnes;
                            if (!nextIteration.Contains(newTerm)) {
                                nextIteration.groups[insertPos].terms.push(newTerm);
                            }
                            compTerm.isChecked = true;
                            numberOfMinimizations++;
                            // System.out.println("Term1 " + term.minTerm + " has a term to optimize " + compTerm.minTerm);
                            System.out.println(newTerm.minTerm + " Added By " + term.minTerm + " and " + compTerm.minTerm);
                        }

                    }
                    else{
                        if (term.isCoveredBy(compTerm)) { //if second covers first
                            term.isChecked = true; //but we wont add it since it's fully covered
                            compTerm.isDontCare = compTerm.isDontCare && term.isDontCare;
                            System.out.println("Term2 " + compTerm.minTerm + " fully covers " + term.minTerm);
                        }
                        else if (compTerm.ContainsTermToOptimize(term)){
                            int bitIndex = Term.findFirstMainBitsDiffBet(compTerm,term);
                            Term newTerm = Term.cpyTerm(term);
                            newTerm.toDontCare(bitIndex);
                            newTerm.isDontCare = (term.isDontCare && compTerm.isDontCare);
                            int insertPos = newTerm.numberOfOnes;
                            if (!nextIteration.Contains(newTerm)) {
                                nextIteration.groups[insertPos].terms.push(newTerm);
                            }
                            System.out.println(newTerm.minTerm + " Added By " + term.minTerm + " and " + compTerm.minTerm);
                            term.isChecked = true;
                            numberOfMinimizations++;
                            // System.out.println("Term2 " + compTerm.minTerm + " has a term to optimize " + term.minTerm);
                        }
                    }
                    
                }
            }
        }
        return numberOfMinimizations;
    }
}

// This Class is for Intermediate Processes for finding the Optimal Solution but the methods modify the Original Instance
class OptimizationGrid {
    String minTerms[];
    int minTermsCoverCount[];

    String coveringTerms[];
    int coveringTermsCount[];

    int checkedCount; // for the marking and optimization step
    boolean bitMap[][];

    // Constructor: builds the grid for optimization
    public OptimizationGrid(String[] termsToCoverBy , String[] termsToBeCovered ){
        minTerms = termsToBeCovered;
        coveringTerms = termsToCoverBy;

        minTermsCoverCount = new int[minTerms.length];
        coveringTermsCount = new int[coveringTerms.length];
        bitMap = new boolean[termsToCoverBy.length][termsToBeCovered.length];

        checkedCount = 0;

        // Fill the bitmap and counts
        for (int termIndex = 0; termIndex <  termsToBeCovered.length ; termIndex++) {
            for (int coverTermIndex = 0; coverTermIndex < termsToCoverBy.length ; coverTermIndex++) {
                if(isCoveredBy(coveringTerms[coverTermIndex], minTerms[termIndex])){
                    coveringTermsCount[coverTermIndex]++;
                    minTermsCoverCount[termIndex]++;
                    bitMap[coverTermIndex][termIndex] = true;
                }
            }
        }
    }

    // Checks if minTerm is covered by coverTerm (with '-')
    private boolean isCoveredBy(String coverTerm,String minTerm){
        if (coverTerm.length() != minTerm.length()) return false;
        for (int i = 0; i < coverTerm.length(); i++) {
            char c1 = coverTerm.charAt(i);
            char c2 = minTerm.charAt(i);
            if (c1 != '-'  && c1 != c2) {
                return false;
            }
        }
        return true;
    }

    // Finds the minterm covered by the fewest terms
    public int mostImportantMinTerm(){
        int minCount = minTermsCoverCount[0];
        int minIndex = 0;
        for(int i =0; i < minTerms.length; i++){
            if (minTermsCoverCount[i] < minCount) {
                minCount = minTermsCoverCount[i];
                minIndex = i;
            }
        }
        return minIndex;
    }

    // Returns the index of the term with fewer literals
    public int GetTheTermWithFewerLiterals(int firstIndex, int secondIndex){
        if(coveringTerms[firstIndex].length() != coveringTerms[secondIndex].length()){
            System.out.println("Cant Compare Terms of Mismatching Lengths");
            return -1;
        }
        int firstLiteralCount = 0;
        int secondLiteralCount = 0;
        for(int i = 0 ; i < coveringTerms[firstIndex].length(); i++){
            char firstCompChar = coveringTerms[firstIndex].charAt(i);
            char secondCompChar = coveringTerms[secondIndex].charAt(i);
            if (firstCompChar == '1' || firstCompChar == '0') {
                firstLiteralCount++;
            }
            if (secondCompChar == '1' || secondCompChar == '0') {
                secondLiteralCount++;
            }
        }
        return (firstLiteralCount < secondLiteralCount)?firstIndex:secondIndex;
    }

    // Finds the covering term that covers the most minterms for a specific minterm
    public int mostCoveringTermIndex(int termIndex){
        int maxCount = 0;
        int maxTerm = -1; //if none was found
        for(int coveringTerm = 0; coveringTerm < coveringTerms.length; coveringTerm++){
            if(bitMap[coveringTerm][termIndex]){
                if(coveringTermsCount[coveringTerm] > maxCount){
                    maxCount = coveringTermsCount[coveringTerm];
                    maxTerm = coveringTerm;
                }
                else if(coveringTermsCount[coveringTerm] == maxCount){
                    maxTerm = GetTheTermWithFewerLiterals(maxTerm, coveringTerm);
                }
            }
        }
        return maxTerm;
    }

    // Finds the covering term that covers the most minterms globally
    public int mostCoveringTermGlobal(){
        int maxCount = 0;
        int maxTerm = 0;
        for(int coveringTerm = 0; coveringTerm < coveringTerms.length; coveringTerm++){
                if(coveringTermsCount[coveringTerm] > maxCount){
                    maxCount = coveringTermsCount[coveringTerm];
                    maxTerm = coveringTerm;
                }
                else if(coveringTermsCount[coveringTerm] == maxCount){
                    maxTerm = GetTheTermWithFewerLiterals(maxTerm, coveringTerm);
                }
        }
        return maxTerm;
    }

    // Checks if all minterms are covered
    public boolean isSolvable(){
        for (int minTermCount : minTermsCoverCount) {
            if(minTermCount == 0) return false;
        }
        return true;
    }

    // Marks a covering term as picked and updates the grid
    public void markAsPicked(int TermToBeMarked){
        for(int minTermIndex = 0; minTermIndex < minTerms.length; minTermIndex++){
            if(bitMap[TermToBeMarked][minTermIndex] && minTermsCoverCount[minTermIndex] < 10000){
                minTermsCoverCount[minTermIndex] = 10000;
                checkedCount++;
                for(int coverTermIndex = 0; coverTermIndex < coveringTerms.length; coverTermIndex++){
                    if(coverTermIndex == TermToBeMarked) continue;
                    if(bitMap[coverTermIndex][minTermIndex]) coveringTermsCount[coverTermIndex]--;
                }
            }
        }
        coveringTermsCount[TermToBeMarked] = 0;
    }

    // Finds the essential prime implicants and optimal cover
    public LinkedList<Term> Optimize(){
        LinkedList<Term> result = new LinkedList<Term>();
        int EPI_minTerm_checker = mostImportantMinTerm();
        // First, pick all essential prime implicants
        while (this.minTermsCoverCount[EPI_minTerm_checker] == 1) {
            int termToPick = this.mostCoveringTermIndex(EPI_minTerm_checker);
            this.markAsPicked(termToPick);
            Term newTerm = new Term(coveringTerms[termToPick]);
            result.push(newTerm);
            EPI_minTerm_checker = mostImportantMinTerm();
        }
        // Then, pick remaining terms to cover all minterms
        while (this.checkedCount < minTerms.length) {
            int termToPick = this.mostCoveringTermGlobal();
            this.markAsPicked(termToPick);
            Term newTerm = new Term(coveringTerms[termToPick]);
            result.push(newTerm);
        }
        return result;
    }

    public boolean[][] getGrid(){
        return this.bitMap;
    }
}

class OptimizationResult {
    public LinkedList<Term> terms;
    public boolean[][] piChart;
    public OptimizationResult(LinkedList<Term> terms, boolean[][] piChart) {
        this.terms = terms;
        this.piChart = piChart;
    }
}

class SimplificationData {
        LinkedList<Term> primeImplicants;
        LinkedList<iteration> doneIterations;
        int iterationCount;
        public SimplificationData(){
            primeImplicants = new LinkedList<Term>();
            doneIterations = new LinkedList<iteration>();
            iterationCount = 0;
        }
        public void AddIteration(iteration itr){
            doneIterations.push(itr);
            iterationCount++;
        }
        public void AddResult(LinkedList<Term> PIs){
            primeImplicants = PIs;
        }
        public int GetIterationCount(){
            return iterationCount;
        }
        public LinkedList<iteration> GetAllIterations(){
            return doneIterations;
        }
        public int GetGroupCountPerItr(){
            return this.GetAllIterations().getFirst().groups.length;
        }
}

public class QuineMckluskySolver {
    // Core Simplification Logic

    // Finds all prime implicants from minterms and don't-cares (String version)
    public static SimplificationData getPrimeImplicantsMinTerms(String[] Terms, String[] DontCares, int numberOfVariables){
        int numberOfGroups = numberOfVariables + 1;
        iteration currIteration = new iteration(numberOfGroups, 0);
        SimplificationData outData = new SimplificationData();
        // Add minterms to their groups
        currIteration.SetUpGroupsFromArrays(Terms, DontCares);

        // Main minimization loop
        while(true){
            iteration nextIteration = new iteration(numberOfGroups, currIteration.iterationNumber + 1);
            int numberOfMinimizations = 0;
            for(int GroupItr = 0; GroupItr < numberOfGroups - 1; GroupItr++){
                numberOfMinimizations += currIteration.MinimizeByAdjacentGroup(GroupItr, nextIteration);
            }
            outData.AddIteration(currIteration);
            if (numberOfMinimizations == 0) break;
            // Add unchecked terms to next iteration
            for (Group group : currIteration.groups) {
                for(Term term : group.terms){
                    if(term.isChecked || nextIteration.Contains(term))
                        continue; 
                    Term newTerm = Term.cpyTerm(term);
                    int insertPos = newTerm.numberOfOnes;
                    nextIteration.groups[insertPos].terms.push(newTerm);
                    
                }
            }
            currIteration = nextIteration;
        }
        // Collect all unchecked terms as prime implicants
        LinkedList<Term> result = new LinkedList<Term>();
        for (Group group : currIteration.groups) {
            for (Term term : group.terms) {
                if(!term.isDontCare)
                    result.push(term);
            }
        }
        outData.AddResult(result);
        return outData;
    }
    // Overload for int[] input
    public static SimplificationData getPrimeImplicantsMinTerms(int[] Terms, int[] DontCares, int numberOfVariables){
        int numberOfGroups = numberOfVariables + 1;
        iteration currIteration = new iteration(numberOfGroups, 0);
        SimplificationData outData = new SimplificationData();
        // Add minterms to their groups
        currIteration.SetUpGroupsFromArrays(Terms, DontCares, numberOfVariables);

        // Main minimization loop (same as above)
        while(true){
            LinkedList<Term> terms_out = new LinkedList<Term>();
            iteration nextIteration = new iteration(numberOfGroups, currIteration.iterationNumber + 1);
            int numberOfMinimizations = 0;
            for(int GroupItr = 0; GroupItr < numberOfGroups - 1; GroupItr++){
                numberOfMinimizations += currIteration.MinimizeByAdjacentGroup(GroupItr, nextIteration);
            }
            outData.AddIteration(currIteration);
            if (numberOfMinimizations == 0) break;
            // Add unchecked terms to next iteration
            for (Group group : currIteration.groups) {
                for(Term term : group.terms){ 
                    // no need to copy the Term since current Iteration elements will not be modified in the next.
                    if(term.isChecked || nextIteration.Contains(term)) 
                        continue;
                    terms_out.push(term);//for site integration
                    int insertPos = term.numberOfOnes;
                    nextIteration.groups[insertPos].terms.push(term);
                }
            }

            currIteration = nextIteration;
        }
        // Collect all non "fully-Dont-Care" terms as prime implicants
        LinkedList<Term> result = new LinkedList<Term>();
        for (Group group : currIteration.groups) {
            for (Term term : group.terms) {
                if(!term.isDontCare)
                    result.push(term);
            }
        }
        outData.AddResult(result);
        return outData;
    }

    public static SimplificationData getPrimeImplicantsMixedTerms(String[] Terms, String[] DontCares, int numberOfVariables){
        int numberOfGroups = numberOfVariables + 1;
        iteration currIteration = new iteration(numberOfGroups, 0);
        SimplificationData outData = new SimplificationData();
        // Add minterms to their groups
        currIteration.SetUpGroupsFromArrays(Terms, DontCares);
        while(true){
            System.out.println("New Iteration\n");
            iteration nextIteration = new iteration(numberOfGroups, currIteration.iterationNumber + 1);
            int numberOfMinimizations = 0;
            for(int GroupItr = 0; GroupItr < numberOfGroups; GroupItr++){
                numberOfMinimizations += currIteration.MinimizeByComparingFollowingGroups(GroupItr, nextIteration);
            }
            outData.AddIteration(currIteration);
            if (numberOfMinimizations == 0) break;
            // Add unchecked terms to next iteration
            for (Group group : currIteration.groups) {
                for(Term term : group.terms){
                    if(term.isChecked || nextIteration.Contains(term))
                        continue; 
                    Term newTerm = Term.cpyTerm(term);
                    int insertPos = newTerm.numberOfOnes;
                    nextIteration.groups[insertPos].terms.push(newTerm);
                    System.out.println("\n" + newTerm.minTerm + " was added as it's unchecked\n");
                }
            }
            currIteration = nextIteration;
        }
        // Collect all non "fully-Dont-Care" terms as prime implicants
        LinkedList<Term> result = new LinkedList<Term>();
        for (Group group : currIteration.groups) {
            for (Term term : group.terms) {
                if(!term.isDontCare)
                    result.push(term);
            }
        }
        outData.AddResult(result);
        System.out.println(termListToStrArr(outData.primeImplicants));
        return outData;
    }
    // Finds the optimal cover using the prime implicants and minterms
    public static OptimizationResult getOptimalExpression(String[] CoveringTerms,String[] minTerms){
        OptimizationGrid optimizationGrid = new OptimizationGrid(CoveringTerms, minTerms);
        if (!optimizationGrid.isSolvable()) {
            System.out.println("Unable to Cover All the Min Terms Using the Provided Terms");
            return null;
        }
        LinkedList<Term> result = optimizationGrid.Optimize();
        return new OptimizationResult(result, optimizationGrid.bitMap);
    }
    public static OptimizationResult getOptimalExpression(String[] CoveringTerms,int[] minTerms, int numberOfVariables){
        String[] minTerms_strs = new String[minTerms.length];

        for (int i = 0 ; i < minTerms.length; i++) {
            minTerms_strs[i] = Term.minTermToString(minTerms[i], numberOfVariables);
        }
        OptimizationGrid optimizationGrid = new OptimizationGrid(CoveringTerms, minTerms_strs);
        if (!optimizationGrid.isSolvable()) {
            System.out.println("Unable to Cover All the Min Terms Using the Provided Terms");
            return null;
        }
        LinkedList<Term> result = optimizationGrid.Optimize();


        return new OptimizationResult(result, optimizationGrid.bitMap);
    }

    // Converts a list of terms to a string array
    public static String[] termListToStrArr(LinkedList<Term> termList){
        String[] strArr = new String [termList.size()];
        int insertPos = 0;
        for (Term term : termList) {
            strArr[insertPos] = term.minTerm.toString();
            insertPos++;
        }
        return strArr;
    }
    // Converts a binary term to a variable expression (e.g., "01-" to "A'B")
    public static String ReplaceByVariables(Term binTerm ,String[] variables){
        if (variables == null) {
            return binTerm.minTerm.toString();
        }
        String newString = "";
        for (int i = 0; i < binTerm.minTerm.length(); i++) {
            switch (binTerm.minTerm.charAt(i)) {
                case '1':
                    newString += (variables[i].length() == 1) ? variables[i] : "(" + variables[i] + ")";
                    break;
                case '0':
                    newString += (variables[i].length() == 1) ? variables[i] + "'" : "(" + variables[i] + ")'";
                    break;
                default:
                    break;
            }
        }
        return newString;
    }

    // Builds the final minimized Boolean expression as a string
    public static String GetExpression(LinkedList<Term> terms, String[] variables){
        String Expression = "";
        HashSet<String> seen = new HashSet<>();
        int numberOfVariables = terms.getFirst().minTerm.length();
        if (variables != null && variables.length < numberOfVariables) {
            System.err.println("NotEnoughVariablesForExpression");
        }
        int count = 0;
        for (Term term : terms) {
            String bin = term.minTerm.toString();
            if (!seen.contains(bin)) {
                Expression += QuineMckluskySolver.ReplaceByVariables(term, variables);
                seen.add(bin);
                count++;
                if (count < terms.size()) {
                    Expression += " + ";
                }
            }
        }
        return Expression;
    }

    // Example main method for testing

    public static void main(String [] args){
        // String[] minTerms= {"0-00","--10","01-0","1-01","0101","1110"};
        // int[] terms = {0,4,2,6,10,14,4,6,9,5,14};
        String[] minTerms= {"0010--00-","1000-----","1001-----","1010-0--0","1011-----"};
        String[] DontCares = {"110------","111------"};
        // String[] DontCares = {"00--", "011-"};
        String[] Variables = {"A","B","C","Rb","Reset","D_7","D_711","D_2312","Eq"};
        SimplificationData simData = getPrimeImplicantsMixedTerms(minTerms,DontCares,9);
        System.out.println(GetExpression(simData.primeImplicants,Variables));
        OptimizationResult optRes = getOptimalExpression(termListToStrArr(simData.primeImplicants), minTerms);
        System.out.println(GetExpression(optRes.terms, Variables));
        Term term1 = new Term("0011");
        Term term2 = new Term("--10");
        // System.out.println(Term.GetTheMoreCoveringTerm(term1,term2 ));
        // System.out.println(Term.findFirstMainBitsDiffBet(term2,term1));
        System.out.println(term2.ContainsTermToOptimize(term1));
        

    }
}

// Minimization is correct
// What's Next : 
// 1-Revamp the Duplicate-Term-Removal : Edit:Done
// 2-Don't-Cares Handling : Edit: Done
// 3-Remove Terms covering same minterms : Edit:Done
// 4- Refine the Optimazion process and Dont Care Handling

