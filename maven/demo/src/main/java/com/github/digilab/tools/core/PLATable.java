package com.github.digilab.tools.core;

import java.util.LinkedList;

class RelationRow {
    String currentState,nextState;
    String input,output;
    public RelationRow(String currentState,String nextState, String input, String output){
        this.currentState = currentState;
        this.nextState = nextState;
        this.input = input;
        this.output = output;
    }
}
class SolutionData {
    SimplificationData simplificationData;
    OptimizationResult optimizationResult;
    public SolutionData(SimplificationData simplificationData, OptimizationResult optimizationResult){
        this.simplificationData = simplificationData;
        this.optimizationResult = optimizationResult;
    }
}
public class PLATable{
    LinkedList<RelationRow> rows ;
    String[] inVars,outVars;//variable names excuding state vars
    int numberOfRows;
    int inVarCount,outVarCount,stateVarCount; //variable count(Excluding state variables)
    int maxStateCount;
    boolean InBooleanForm; 
    //is True when we haven't yet substituted the var name for it's binary representation after state assignment
    public PLATable(String[] inVars, String[] outVars ,int stateVarCount){ //input and output are excluding the state vars
        this.rows = new LinkedList<RelationRow>();
        this.numberOfRows  = 0;
        
        this.inVars  = inVars;
        this.outVars = outVars;
        
        this.inVarCount    = inVars.length;
        this.outVarCount   = outVars.length;
        this.stateVarCount = stateVarCount;

        this.maxStateCount = (int)Math.pow(2,stateVarCount);
    }
    public void AddRow(RelationRow row){
        rows.push(row);
        numberOfRows++;
    }
    
    public boolean isValidAssignmentArr(String[] stateAssignments){
        if (stateAssignments == null || this.maxStateCount > stateAssignments.length) {
            return false;
        }
        for (int i = 0; i < this.maxStateCount; i++) { //i only care about the first n assignments that'll cover all the states in the table
            if (stateAssignments[i] == null || Term.isBooleanString(stateAssignments[i]) || stateAssignments[i].length() != this.stateVarCount) {
                return false;
            }
        }
        return true;
    }
    public String GetStateStrAssignment(State state, String[] stateAssignments){
        int stateIndex = state.stateID;
        return (stateAssignments == null)?Term.minTermToString(stateIndex, this.stateVarCount) : stateAssignments[stateIndex];
    }
    public String GetStateStrAssignment(String stateStr, String[] stateAssignments){ //if state is given as a string with notation S0,S1,S2 etc and 'DC' for entire dontCare terms e.g '----''
        if (stateStr == "DC") {
            return "-".repeat(this.stateVarCount);
        }
    int stateIndex;
        try {
            stateIndex = Integer.parseInt(stateStr.replaceAll("S",""));
        } catch (Exception e) {
            return null;//if it's not a valid number
        }
        return (stateAssignments == null)?Term.minTermToString(stateIndex, this.stateVarCount) : stateAssignments[stateIndex];
    
    }

    
    public String[] GetTermsOf(boolean isStateVar , int index, String[] stateAssignments){
        LinkedList<String> tempList = new LinkedList<String>(); 
        //linked list is because we dont know how many terms we'll be having and having extra "null" terms will throw null pointer exception
        
        for (RelationRow row : this.rows) {
            String lookUpStr = (isStateVar)? GetStateStrAssignment(row.nextState , stateAssignments): row.output;
            if (lookUpStr.charAt(index) == '1') {
                    tempList.push(GetStateStrAssignment(row.currentState, stateAssignments) + row.input);
            }
        }
        return tempList.toArray(new String[0]);
    }
    public String[] GetDontCaresOf(boolean isStateVar , int index, String[] stateAssignments){
        LinkedList<String> tempList = new LinkedList<String>();

        for (RelationRow row : this.rows) {
            String lookUpStr = (isStateVar)? GetStateStrAssignment(row.nextState , stateAssignments) : row.output;
            if (lookUpStr.charAt(index) == '-') {
                    tempList.push(GetStateStrAssignment(row.currentState, stateAssignments) + row.input);
            }
        }
        return tempList.toArray(new String[0]);
    }
    
    public String toString(){
        String result = "Table\n----------------";
        for (RelationRow row : this.rows) {
            result += "\n" + row.currentState + "|" + row.input + "|||" + row.nextState + "|" + row.output;
        }
        return result;
    }

    public SolutionData SolveVariableAt(boolean isStateVar, int index, String[] stateAssignments){ 
        //if stateAssignment is null the default assignment is used
        //index is relative to the type of variable e.g index 0 for isStateVar = true is the MostSignifactBit of stateVars
        //while index 0 for isStateVar = false is the first outVar
        String[] usedAssignment = null;
        if (isValidAssignmentArr(stateAssignments)) {
            usedAssignment = stateAssignments;
        }

        try{
            String[] terms = this.GetTermsOf(isStateVar, index, usedAssignment);
            String[] dontCares = this.GetDontCaresOf(isStateVar, index, usedAssignment);
            
            SimplificationData data = QuineMckluskySolver.getPrimeImplicantsMixedTerms(terms,dontCares,inVars.length + stateVarCount);
            OptimizationResult res = QuineMckluskySolver.getOptimalExpression(QuineMckluskySolver.termListToStrArr(data.primeImplicants) , terms);
            return new SolutionData(data, res);
            
        } catch (Exception e) {
            // An Issue occured while trying to solve
            return null;
        }
    }
    public static void main(String[] args){
        // String[] inVars = {"Rb","Reset","D7","D711","D2312","Eq"};
        String[] inVars = {"a","b"};
        String[] outVars = {"c","d"};
        int stateVarCount = 2;
        PLATable table = new PLATable(inVars, outVars, stateVarCount);
        
        table.AddRow(new RelationRow("S0", "S1", "01", "10"));
        table.AddRow(new RelationRow("S1", "S2", "--", "01"));
        table.AddRow(new RelationRow("S2", "DC", "--", "--"));
        table.AddRow(new RelationRow("S3", "DC", "--", "--"));
        // table.AddRow(new RelationRow("S0", "S0", "0-----", "0000"));
        // table.AddRow(new RelationRow("S0", "S1", "1-----", "0000"));
        // table.AddRow(new RelationRow("S1", "S1", "1-----", "0010"));
        // table.AddRow(new RelationRow("S1", "S4", "0--00-", "0001"));
        // table.AddRow(new RelationRow("S1", "S3", "0--01-", "0000"));
        // table.AddRow(new RelationRow("S1", "S2", "0--1--", "0000"));
        // table.AddRow(new RelationRow("S2", "S2", "-0----", "1000"));
        // table.AddRow(new RelationRow("S2", "S0", "-1----", "1000"));
        // table.AddRow(new RelationRow("S3", "S0", "-1----", "0100"));
        // table.AddRow(new RelationRow("S3", "S3", "-0----", "0100"));
        // table.AddRow(new RelationRow("S4", "S4", "0-----", "0000"));
        // table.AddRow(new RelationRow("S4", "S5", "1-----", "0000"));
        // table.AddRow(new RelationRow("S5", "S4", "0-0--0", "0000"));
        // table.AddRow(new RelationRow("S5", "S3", "0-1--0", "0000"));
        // table.AddRow(new RelationRow("S5", "S2", "0----1", "0000"));
        // table.AddRow(new RelationRow("S5", "S5", "1-----", "0010"));
        // table.AddRow(new RelationRow("S6", "DC", "------", "----"));
        // table.AddRow(new RelationRow("S7", "DC", "------", "----"));
        
        // System.out.println(table.toString());
        for(int i = 0; i < outVars.length; i++){
            // String[] terms = table.GetTermsOf(false,i,null);
            // String[] dontCares = table.GetDontCaresOf(false,i,null);
            // SimplificationData data = QuineMckluskySolver.getPrimeImplicantsMixedTerms(terms,dontCares,inVars.length + stateVarCount);
            // OptimizationResult res = QuineMckluskySolver.getOptimalExpression(QuineMckluskySolver.termListToStrArr(data.primeImplicants) , terms);
            SolutionData output = table.SolveVariableAt(false, i, null);
            System.out.println(outVars[i] + " : " + QuineMckluskySolver.GetExpression(output.optimizationResult.terms, null) );
        }
    }
    
    
}
