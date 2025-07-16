package com.github.digilab.tools.core;
import java.util.LinkedList;


class ControlLineRuleSet{
    String name;
    char[][] ruleGrid;
    public ControlLineRuleSet(String name, char[][] ruleGrid){
        this.name = name;
        this.ruleGrid = ruleGrid;
    }
}

class FFRuleSet {
    int numberOfControlLines;
    LinkedList<ControlLineRuleSet> ruleSet;
    public FFRuleSet(){
        numberOfControlLines = 0;
        ruleSet = new LinkedList<ControlLineRuleSet>();
    }
    public void AddControlLine(ControlLineRuleSet newRule){
        ruleSet.add(newRule);
    }


}

public class FlipFlopRealizer {
    
    FFRuleSet rules;
    PLATable table;
    
    public FlipFlopRealizer(FFRuleSet rules, PLATable table){
        this.rules = rules;
        this.table = table;
    }
    
    public boolean IsSolvable(){
        if (table.stateVarCount != 1 || table.numberOfRows != 4) {
            return false;
        }
        return true;
    }

    public SolutionData SolveControlLineAt(int index){
        LinkedList<String> terms = new LinkedList<String>();
        LinkedList<String> dontCares = new LinkedList<String>();
        ControlLineRuleSet targetLineRules = null;
        
        int itr = 0; 
        for (ControlLineRuleSet ctrlLineRules : rules.ruleSet) {
            if (itr == index) {
                targetLineRules = ctrlLineRules;
                break;
            }
            itr++;
        }

        for (RelationRow row : table.rows) {
            String currStateStr = table.GetStateStrAssignment(row.currentState,null);
            String nextStateStr = table.GetStateStrAssignment(row.nextState,null);
            
            int currStateVal = Term.booleanToInt(currStateStr);
            int nextStateVal = Term.booleanToInt(nextStateStr);

            char val = targetLineRules.ruleGrid[currStateVal][nextStateVal];

            if (val == '1') {
                terms.add(currStateStr + row.input);
            }
            else if (val == '-') {
                dontCares.add(currStateStr + row.input);
            }

        }
        SimplificationData simplificationData;
        OptimizationResult optimizationResult;

        if (terms == null || terms.isEmpty()) {
            simplificationData = null;
            LinkedList<Term> nullTermList = new LinkedList<Term>();
            nullTermList.add(new Term("0"));
            optimizationResult = new OptimizationResult(nullTermList, null);
        }else{
            simplificationData = QuineMckluskySolver.getPrimeImplicantsMixedTerms( terms.toArray(new String[0]), 
                                                                                   dontCares.toArray(new String[0]), 
                                                                                   1 + table.inVarCount);
        
            optimizationResult = QuineMckluskySolver.getOptimalExpression( QuineMckluskySolver.termListToStrArr(simplificationData.primeImplicants), 
                                                                           terms.toArray(new String[0]));
        }


        return new SolutionData(simplificationData, optimizationResult);
    }


    private static int GetIndexOfVar(String[] varNameList, String target){
        for (int i = 0; i < varNameList.length; i++) {
            if (target.equals(varNameList[i])) {
                return i;
            }
        }
        return -1;

    }
    private static String GetRowInString(String term, String[] varNameList, String stateVarName){
        char[] inputRowStr = "-".repeat(varNameList.length).toCharArray();

        String currVar = "";
        String stateStr = "-"; 
        for (int itr = 0; itr < term.length(); itr++) {
            
            if (term.charAt(itr) == '\'') {//negated Non-Braced
                if(currVar.equals(stateVarName)){// it's the state
                    stateStr = "0";
                } else if (currVar != ""){
                    inputRowStr[GetIndexOfVar(varNameList, currVar)] = '0';
                }
                currVar = "";
            }
            else if (term.charAt(itr) != '(') { //Non-Braced-Var..oneChar
                if (currVar.equals(stateVarName)) { //previous wasn't negated
                    stateStr = "1";
                } else if (currVar != ""){
                    System.out.println(currVar);
                    inputRowStr[GetIndexOfVar(varNameList, currVar)] = '1';
                }
                currVar = "";//reset currVar
                currVar += term.charAt(itr);//new VarName
            }
            else { // new Braced Variable
                if (currVar.equals(stateVarName)) { //previous wasn't negated
                    stateStr = "1";
                } else if (currVar != "") { // add previous non-negated var 
                    inputRowStr[GetIndexOfVar(varNameList, currVar)] = '1';
                }
                currVar = "";
                itr++; //go to the beginning of variableName
                while (term.charAt(itr) != ')') {
                    currVar += term.charAt(itr);
                    itr++;
                }
                //we added the entire varName
            }
        }
        if (currVar.equals(stateVarName)) { //last var
            stateStr = "1";
        } else if (currVar != "") { // add previous non-negated var 
            inputRowStr[GetIndexOfVar(varNameList, currVar)] = '1';
        }
        return stateStr + String.copyValueOf(inputRowStr);
    }
    public static PLATable GetTableFromExpression(String expression, String[] varNameList, String stateVarName){ 
        //we need to have only one state variable other state variables should be considered as other inVars
        PLATable table = new PLATable(varNameList, null, 1);
        String[] terms = expression.split("\\s*\\+\\s*");
        LinkedList<String> onTerms = new LinkedList<String>();
        LinkedList<Term> intersection = new LinkedList<Term>();
        intersection.add(new Term("-".repeat(table.inVarCount + table.stateVarCount)));

        
        State state1 = new State(1, "00");
        State state0 = new State(0, "00");
        


        for (String termStr : terms) {
            onTerms.add(GetRowInString(termStr, varNameList, stateVarName));
        }
        //adding where the function gives 1
        for (String termStr : onTerms) {
            switch (termStr.charAt(0)) { //check state bit
                case '0':
                    table.AddRow(new RelationRow(state0, state1, termStr.substring(1), "0"));
                    break;
                case '1':
                    table.AddRow(new RelationRow(state1, state1, termStr.substring(1), "0"));
                    break;
                case '-':
                    table.AddRow(new RelationRow(state0, state1, termStr.substring(1), "0"));
                    table.AddRow(new RelationRow(state1, state1, termStr.substring(1), "0"));
                    break;
            }
            //getting the running Complement Intersection
            Term tempTerm = new Term(termStr);
            // System.out.println("Getting Intersection Between : Old IntersectionList");
            // for (Term term : intersection) {
            //     System.out.print(term.minTerm + " ");
            // }
            // System.out.println("\n And Between The Complement List of " + termStr);
            // for (Term term : tempTerm.getComplement()) {
            //     System.out.print(term.minTerm + " ");
            // }
            // System.out.println("\n");

            intersection = Term.getIntersectionList(intersection, tempTerm.getComplement());
            
            // System.out.println("Total Intersection");
            // for (Term term : intersection) {
            //     System.out.print(term.minTerm + " ");
            // }
            // System.out.println("\n");

        }
        
        //adding where the function gives 0
        String[] offTerms = QuineMckluskySolver.termListToStrArr(intersection);

        for (String termStr : offTerms) {
            // String rowInStr = GetRowInString(termStr, varNameList, stateVarName);
            switch (termStr.charAt(0)) { //check state bit
                case '0':
                    table.AddRow(new RelationRow(state0, state0, termStr.substring(1), "0"));
                    break;
                case '1':
                    table.AddRow(new RelationRow(state1, state0, termStr.substring(1), "0"));
                    break;
                case '-':
                    table.AddRow(new RelationRow(state0, state0, termStr.substring(1), "0"));
                    table.AddRow(new RelationRow(state1, state0, termStr.substring(1), "0"));
                    break;
            }
        }

        
        return table;
    }

    public static void main(String[] args){
        String[] inVars = {"Ab","Bc"};
        String[] outVars = {};
        char[][] gridS = {{'0','1'},{'0','-'}}; //SR S FF grid
        char[][] gridR = {{'-','0'},{'1','0'}}; //SR R FF grid
        // PLATable table = new PLATable(inVars, null, 1);
        // table.AddRow(new RelationRow(new State(0, "00"), new State(1, "00"), "010", "0"));
        // table.AddRow(new RelationRow(new State(1, "00"), new State(0, "00"), "110", "0"));

        FFRuleSet rules = new FFRuleSet();
        rules.AddControlLine(new ControlLineRuleSet("S", gridS));
        rules.AddControlLine(new ControlLineRuleSet("R", gridR));

        PLATable genTable = FlipFlopRealizer.GetTableFromExpression("(Q_1)(Ab)(Bc) + (Q_1)'(Ab)(Bc)'", inVars, "Q_1");
        System.out.println(genTable.toString());
        FlipFlopRealizer ffSolver = new FlipFlopRealizer(rules, genTable);
        String expressionS = QuineMckluskySolver.GetExpression(ffSolver.SolveControlLineAt(0).optimizationResult.terms, null);
        String expressionR = QuineMckluskySolver.GetExpression(ffSolver.SolveControlLineAt(1).optimizationResult.terms, null);
        System.out.println(expressionR );
        // System.out.println(FlipFlopRealizer.GetRowInString("ABC",inVars,"Q"));

        // String test = "";
        // String test2 = "A";
        // test += 'A';
        // System.out.println(FlipFlopRealizer.GetIndexOfVar(inVars, test2));
        // System.out.println(Term.booleanToInt("10111"));
        // System.out.println(Term.getIntersection(new Term("-1-0"), new Term("-11-")).minTerm);

        // System.out.println(test == test2);
    }

}
