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
        int size = this.rules.ruleSet.size();
        
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

    public static void main(String[] args){
        String[] inVars = {"A","B","C"};
        String[] outVars = {};
        char[][] gridS = {{'0','1'},{'0','-'}}; //SR S FF grid
        char[][] gridR = {{'-','0'},{'1','0'}}; //SR R FF grid
        PLATable table = new PLATable(inVars, null, 1);
        table.AddRow(new RelationRow(new State(0, "00"), new State(1, "00"), "010", "0"));
        table.AddRow(new RelationRow(new State(1, "00"), new State(0, "00"), "110", "0"));

        FFRuleSet rules = new FFRuleSet();
        rules.AddControlLine(new ControlLineRuleSet("S", gridS));
        rules.AddControlLine(new ControlLineRuleSet("R", gridR));

        FlipFlopRealizer ffSolver = new FlipFlopRealizer(rules, table);
        String expressionS = QuineMckluskySolver.GetExpression(ffSolver.SolveControlLineAt(0).optimizationResult.terms, null);
        String expressionR = QuineMckluskySolver.GetExpression(ffSolver.SolveControlLineAt(1).optimizationResult.terms, null);
        System.out.println(expressionS + "\n" + expressionR);
    }

}
