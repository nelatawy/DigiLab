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
            
            int currStateVal = Integer.parseInt(currStateStr);
            int nextStateVal = Integer.parseInt(nextStateStr);

            char val = targetLineRules.ruleGrid[currStateVal][nextStateVal];

            if (val == '1') {
                terms.add(currStateStr + row.input);
            }
            else if (val == '-') {
                dontCares.add(currStateStr + row.input);
            }

        }

        SimplificationData simplificationData = QuineMckluskySolver.getPrimeImplicantsMixedTerms( terms.toArray(new String[0]), 
                                                                                                  dontCares.toArray(new String[0]), 
                                                                                                  1 + table.inVarCount);

        OptimizationResult optimizationResult = QuineMckluskySolver.getOptimalExpression( QuineMckluskySolver.termListToStrArr(simplificationData.primeImplicants), 
                                                                                          terms.toArray(new String[0]));
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
        if (currVar != "") { //it's a non-negated varName
            inputRowStr[GetIndexOfVar(varNameList, currVar)] = '1';
        }
        return stateStr + String.copyValueOf(inputRowStr);
    }
    public static PLATable GetTableFromExpression(String expression, String[] varNameList, String stateVarName){ 
        //we need to have only one state variable other state variables should be considered as other inVars
        PLATable table = new PLATable(varNameList, null, 1);
        String[] terms = expression.split("\\s*\\+\\s*");
        for (String term : terms) {
            String rowInStr = GetRowInString(term, varNameList, stateVarName);
            switch (rowInStr.charAt(0)) { //check state bit
                case '0':
                    table.AddRow(new RelationRow(new State(0, "00"), new State(1, "00"), rowInStr.substring(1), "0"));
                    break;
                case '1':
                    table.AddRow(new RelationRow(new State(1, "00"), new State(1, "00"), rowInStr.substring(1), "0"));
                    break;
                case '-':
                    table.AddRow(new RelationRow(new State(0, "00"), new State(1, "00"), rowInStr.substring(1), "0"));
                    table.AddRow(new RelationRow(new State(1, "00"), new State(1, "00"), rowInStr.substring(1), "0"));
                    break;
            }
        }
        return table;
    }
    public static void main(String[] args){
        String[] inVars = {"A","B"};
        String[] outVars = {};
        char[][] gridS = {{'0','1'},{'0','-'}}; //SR S FF grid
        char[][] gridR = {{'-','0'},{'1','0'}}; //SR R FF grid
        // PLATable table = new PLATable(inVars, null, 1);
        // table.AddRow(new RelationRow(new State(0, "00"), new State(1, "00"), "010", "0"));
        // table.AddRow(new RelationRow(new State(1, "00"), new State(0, "00"), "110", "0"));

        FFRuleSet rules = new FFRuleSet();
        rules.AddControlLine(new ControlLineRuleSet("S", gridS));
        rules.AddControlLine(new ControlLineRuleSet("R", gridR));

        PLATable genTable = FlipFlopRealizer.GetTableFromExpression("Q(A)B + Q'(A)(B)'", inVars, "Q");
        FlipFlopRealizer ffSolver = new FlipFlopRealizer(rules, genTable);
        String expressionS = QuineMckluskySolver.GetExpression(ffSolver.SolveControlLineAt(0).optimizationResult.terms, null);
        // String expressionR = QuineMckluskySolver.GetExpression(ffSolver.SolveControlLineAt(1).optimizationResult.terms, null);
        System.out.println(expressionS );
        // System.out.println(FlipFlopRealizer.GetRowInString("ABC",inVars,"Q"));

        String test = "";
        String test2 = "A";
        test += 'A';
        System.out.println(FlipFlopRealizer.GetIndexOfVar(inVars, test2));
        System.out.println(genTable.toString());
        // System.out.println(test == test2);
    }

}
