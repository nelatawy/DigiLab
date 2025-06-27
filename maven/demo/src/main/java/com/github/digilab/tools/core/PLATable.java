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

public class PLATable{
    LinkedList<RelationRow> rows ;
    String[] inVars,outVars;//variable names excuding state vars
    int numberOfRows;
    int inVarCount,outVarCount,stateVarCount; //variable count(Excluding state variables)
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
    }
    public void AddRow(RelationRow row){
        rows.push(row);
        numberOfRows++;
    }

    public String[] GetTermsOf(boolean isStateVar , int index){
        LinkedList<String> tempList = new LinkedList<String>(); 
        //linked list is because we dont know how many terms we'll be having and having extra "null" terms will throw null pointer exception
        
        for (RelationRow row : this.rows) {
            String lookUpStr = (isStateVar)? row.nextState : row.output;
            if (lookUpStr.charAt(index) == '1') {
                    tempList.push(row.currentState + row.input);
            }
        }
        return tempList.toArray(new String[0]);
    }
    public String[] GetDontCaresOf(boolean isStateVar , int index){
        LinkedList<String> tempList = new LinkedList<String>();
        for (RelationRow row : this.rows) {
            String lookUpStr = (isStateVar)? row.nextState : row.output;
            if (lookUpStr.charAt(index) == '-') {
                    tempList.push(row.currentState + row.input);
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

    public static void main(String[] args){
        String[] inVars = {"Rb","Reset","D7","D711","D2312","Eq"};
        String[] outVars = {"Win","Lose","Roll","Sp"};
        int stateVarCount = 3;
        PLATable table = new PLATable(inVars, outVars, 3);
        
        table.AddRow(new RelationRow("000", "000", "0-----", "0000"));
        table.AddRow(new RelationRow("000", "001", "1-----", "0000"));
        table.AddRow(new RelationRow("001", "001", "1-----", "0010"));
        table.AddRow(new RelationRow("001", "100", "0--00-", "0001"));
        table.AddRow(new RelationRow("001", "011", "0--01-", "0000"));
        table.AddRow(new RelationRow("001", "010", "0--1--", "0000"));
        table.AddRow(new RelationRow("010", "010", "-0----", "1000"));
        table.AddRow(new RelationRow("010", "000", "-1----", "1000"));
        table.AddRow(new RelationRow("011", "000", "-1----", "0100"));
        table.AddRow(new RelationRow("011", "011", "-0----", "0100"));
        table.AddRow(new RelationRow("100", "100", "0-----", "0000"));
        table.AddRow(new RelationRow("100", "101", "1-----", "0000"));
        table.AddRow(new RelationRow("101", "100", "0-0--0", "0000"));
        table.AddRow(new RelationRow("101", "011", "0-1--0", "0000"));
        table.AddRow(new RelationRow("101", "010", "0----1", "0000"));
        table.AddRow(new RelationRow("101", "101", "1-----", "0010"));
        table.AddRow(new RelationRow("110", "---", "------", "----"));
        table.AddRow(new RelationRow("111", "---", "------", "----"));
        
        System.out.println(table.toString());
        for(int i = 0; i < outVars.length; i++){
            String[] terms = table.GetTermsOf(false,i);
            String[] dontCares = table.GetDontCaresOf(false,i);
            SimplificationData data = QuineMckluskySolver.getPrimeImplicantsMixedTerms(terms,dontCares,inVars.length + stateVarCount);
            OptimizationResult res = QuineMckluskySolver.getOptimalExpression(QuineMckluskySolver.termListToStrArr(data.primeImplicants) , terms);
            System.out.println(outVars[i] + " : " + QuineMckluskySolver.GetExpression(res.terms, null) );
        }
    }
    
}
