package com.github.digilab.tools.core;
import java.util.LinkedList;

//This class is for handling specific control lines like the S in an SR FlipFlop
class ControlLineRuleSet{
    String name;
    char[][] ruleGrid; //the indices are number (0/1)(0/1) first is for current state the second is for next state
    public ControlLineRuleSet(String name, char[][] ruleGrid){
        this.name = name;
        this.ruleGrid = ruleGrid;
    }
}

//This class handles the entirety of a FlipFlop and includes different Control Lines
class FFRuleSet {
    int numberOfControlLines;
    LinkedList<ControlLineRuleSet> ruleSet;
    public FFRuleSet(){
        numberOfControlLines = 0;
        ruleSet = new LinkedList<ControlLineRuleSet>();
    }
    //Adds a new Control Line like adding the S Control Line to an SR FlipFlop implementation
    //and this allows you to solve the control line at that index
    public void AddControlLine(ControlLineRuleSet newRule){
        ruleSet.add(newRule);
    }
}


public class FlipFlopRealizer {
    
    FFRuleSet rules;
    PLATable table;

    //Initiate the FFRealizaer from the FFRuleset Created Priorly and needs a PLATable 
    // (Recommended : use the GetTableFromExpresion() method to genereate the table in the expected format)
    public FlipFlopRealizer(FFRuleSet rules, PLATable table){
        this.rules = rules;
        this.table = table;
    }
    
    //Checks the Straight-Forward reasons that will prevent this FFRealizer from being solved
    public boolean IsSolvable(){
        if (table.stateVarCount != 1 || table.numberOfRows != 4) {
            return false;
        }
        return true;
    }

    //Gets the SolutionData of that Control-Line at that index ..(indices are 0-based and in the order of insertion of that control-Line )
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

        if (terms.isEmpty()) { // So that we use this null value to handle it later in the WebHandler
            return null;
        }else{
            simplificationData = QuineMckluskySolver.getPrimeImplicantsMixedTerms( terms.toArray(new String[0]), 
                                                                                   dontCares.toArray(new String[0]), 
                                                                                   1 + table.inVarCount);
        
            optimizationResult = QuineMckluskySolver.getOptimalExpression( QuineMckluskySolver.termListToStrArr(simplificationData.primeImplicants), 
                                                                           terms.toArray(new String[0]));
        }


        return new SolutionData(simplificationData, optimizationResult);
    }

    //used as an auxilary function for GetRowInStr()
    private static int GetIndexOfVar(String[] varNameList, String target){
        for (int i = 0; i < varNameList.length; i++) {
            if (target.equals(varNameList[i])) {
                return i;
            }
        }
        return -1;

    }
    
    //Get the boolean expression written the notation(stateBits|inVars) for a certain Trm written in Variable Notiation like :
    // e.g : (Q0)A(Bb)'C and getting '1101' from it
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
    
    //Given an expression like ABC' + BD  and constructs the PLATable from it that can be used to construct the FFRealizer
    
    public static PLATable GetTableFromExpression(String expression, String[] varNameList, String stateVarName){ 
        //we need to have only one state variable other state variables should be considered as other inVars
        PLATable table = new PLATable(varNameList, null, 1);
        
        // Seperating the expression to it's terms
        String[] terms = expression.split("\\s*\\+\\s*"); 
        
        // The Terms that define the ones of the function
        LinkedList<String> onTerms = new LinkedList<String>(); 
        
        //total intersection of zeroes of the function
        LinkedList<Term> offIntersections = new LinkedList<Term>();
        offIntersections.add(new Term("-".repeat(table.inVarCount + table.stateVarCount)));
        
        //Default Values for on and off states
        State state1 = new State(1, "00");
        State state0 = new State(0, "00");
        
        // getting the ones of a function 
        for (String termStr : terms) {
            onTerms.add(GetRowInString(termStr, varNameList, stateVarName));
        }
        //Adding the Ones of the function
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
            //getting the running Complement Intersection that is : The total intersection of the complements of all the ones of that function
            //which is basically a way of getting the zeroes of a function
            Term tempTerm = new Term(termStr);
            offIntersections = Term.getIntersectionList(offIntersections, tempTerm.getComplement());
        }
        
        //Adding the Zeroes of the function
        String[] offTerms = QuineMckluskySolver.termListToStrArr(offIntersections);

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
        // String[] outVars = {};
        char[][] gridS = {{'0','1'},{'0','-'}}; //SR S FF grid
        char[][] gridR = {{'-','0'},{'1','0'}}; //SR R FF grid


        FFRuleSet rules = new FFRuleSet();
        rules.AddControlLine(new ControlLineRuleSet("S", gridS));
        rules.AddControlLine(new ControlLineRuleSet("R", gridR));

        PLATable genTable = FlipFlopRealizer.GetTableFromExpression("(Q_1)(Ab)(Bc) + (Q_1)'(Ab)(Bc)'", inVars, "Q_1");
        System.out.println(genTable.toString());
        FlipFlopRealizer ffSolver = new FlipFlopRealizer(rules, genTable);
        String expressionS = QuineMckluskySolver.GetExpression(ffSolver.SolveControlLineAt(0).optimizationResult.terms, null);
        String expressionR = QuineMckluskySolver.GetExpression(ffSolver.SolveControlLineAt(1).optimizationResult.terms, null);
        System.out.println("Control Lines' Outputs : ");
        System.out.println("S : " + expressionS);
        System.out.println("R : " + expressionR);

    }

}
