package com.github.digilab.tools.core;

import java.util.HashSet;
import java.util.LinkedList;

import org.json.JSONArray;
import org.json.JSONObject;

public class LinkPathsSolverWebHandler {
    public static void main(String[] args){
        String jsonStr = args[0];
        JSONObject jsonObj = new JSONObject(jsonStr);

        int stateVarCount = JSONInputParser.GetIntFromJSONOBJ(jsonObj,"stateVarCount", -1);
        int maxNumberOfStates = (int)Math.pow(2, stateVarCount);
        
        int inVarCount  = JSONInputParser.GetIntFromJSONOBJ(jsonObj,"inVarCount", -1);
        String[] inVars = JSONInputParser.GetStrArrFromJSONOBJ(jsonObj,"inVarNames");

        int outVarCount  = JSONInputParser.GetIntFromJSONOBJ(jsonObj,"outVarCount", -1);
        String[] outVars = JSONInputParser.GetStrArrFromJSONOBJ(jsonObj,"outVarNames");

        int linkPathCount = JSONInputParser.GetIntFromJSONOBJ(jsonObj,"linkPathsCount", -1);


        StateGraph stateGraph = new StateGraph(inVars, outVars);
        HashSet<Integer> seenStatesByIDs = new HashSet<Integer>();
        
        String[] mooreOutputs = JSONInputParser.GetStrArrFromJSONOBJ(jsonObj, "mooreOutputs");
        State[] states = new State[maxNumberOfStates];//indices indicate stateIDs
        
        JSONArray linkPaths = jsonObj.getJSONArray("linkPaths");
        for (int i = 0; i < linkPathCount; i++) {
            JSONObject currLink = linkPaths.getJSONObject(i);
            String fromState = JSONInputParser.GetStrFromJSONOBJ(currLink, "fromState", "").replaceAll("S",""); //to only get ID
            String toState = JSONInputParser.GetStrFromJSONOBJ(currLink, "toState", "").replaceAll("S",""); //to only get ID

            String inStr = JSONInputParser.GetStrFromJSONOBJ(currLink, "inStr", ""); 
            String outStr = JSONInputParser.GetStrFromJSONOBJ(currLink, "outStr", ""); 
            
        }
    }
}
