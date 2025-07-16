package com.github.digilab.tools.core;
import java.util.Arrays;
import java.util.LinkedList;

import org.json.JSONArray;
import org.json.JSONObject;

public class LinkPathsSolverWebHandler {
    public static final int INVALID_TERMS = -1;
    public static final int VALID_TERMS  = 1;
    
    public static int CheckValidityOfMooreOutputs(String[] mooreOutputs, int outVarCount){
        for (int i = 0; i < mooreOutputs.length; i++) {
            if (!Term.isBooleanString(mooreOutputs[i]) || mooreOutputs[i].length() != outVarCount) {
                return INVALID_TERMS;
            }
        }
        return VALID_TERMS;
    }
    
    public static int AssignStatesFromLinkPaths(JSONArray linkPaths, int linkPathCount, boolean[] addedStates , State[] states , String[] mooreOutputs , int inVarCount, int outVarCount){
        for (int i = 0; i < linkPathCount; i++) {
            JSONObject currLink = linkPaths.getJSONObject(i);
            
            String fromState = JSONInputParser.GetStrFromJSONOBJ(currLink, "fromState", "").replaceAll("S",""); //to only get ID
            int fromStateID = Integer.parseInt(fromState);
            //no need to handle non-numeric data since it's prohibited in the front-end
            String toState = JSONInputParser.GetStrFromJSONOBJ(currLink, "toState", "").replaceAll("S",""); //to only get ID
            int toStateID = Integer.parseInt(toState);
            
            addedStates[fromStateID] = true;
            //mark only the from states so that we dont have to add it in the table afterwards..it's already there
            //Adding the state on it's first encounter
            if (states[fromStateID] == null) {
                states[fromStateID] = new State(fromStateID, mooreOutputs[fromStateID]);
            }
            if (states[toStateID] == null) {
                states[toStateID] = new State(toStateID, mooreOutputs[toStateID]);
            }
            //Adding the Input And Output String of CurrLink
            String inStr = JSONInputParser.GetStrFromJSONOBJ(currLink, "inStr", ""); 
            String outStr = JSONInputParser.GetStrFromJSONOBJ(currLink, "outStr", ""); 
            if (!Term.isBooleanString(inStr) || !Term.isBooleanString(outStr) || inStr.length() != inVarCount || outStr.length() != outVarCount) {
                return INVALID_TERMS;
            }
            states[fromStateID].AddLinkTo(states[toStateID], inStr, outStr);//since we store outLiks Only
        }
        return VALID_TERMS;
    }
    public static void main(String[] args){
        // String jsonStr = "{\r\n" + //
        //                 "  \"stateVarCount\": 2,\r\n" + //
        //                 "  \"inVarCount\": 2,\r\n" + //
        //                 "  \"outVarCount\": 2,\r\n" + //
        //                 "  \"inVarNames\": [\"a\", \"b\"],\r\n" + //
        //                 "  \"outVarNames\": [\"c\", \"d\"],\r\n" + //
        //                 "  \"mooreOutputs\": [\"11\", \"10\" , \"10\", \"00\"],\r\n" + //
        //                 "  \"linkPathsCount\": \"2\", \r\n" + //
        //                 "  \"linkPaths\": [\r\n" + //
        //                 "    {\r\n" + //
        //                 "      \"fromState\": \"S0\",\r\n" + //
        //                 "      \"inStr\": \"01\",\r\n" + //
        //                 "      \"outStr\": \"10\",\r\n" + //
        //                 "      \"toState\": \"S1\"\r\n" + //
        //                 "    },\r\n" + //
        //                 "    {\r\n" + //
        //                 "      \"fromState\": \"S1\",\r\n" + //
        //                 "      \"inStr\": \"--\",\r\n" + //
        //                 "      \"outStr\": \"01\",\r\n" + //
        //                 "      \"toState\": \"S2\"\r\n" + //
        //                 "    }\r\n" +
        //                 "  ]\r\n" + //
        //                 "}\r\n" + //
        //                 "";
        String jsonStr = args[0];
        JSONObject jsonObj = new JSONObject(jsonStr);
        JSONObject Data_JSON = new JSONObject();

        int stateVarCount = JSONInputParser.GetIntFromJSONOBJ(jsonObj,"stateVarCount", -1);
        int maxNumberOfStates = (int)Math.pow(2, stateVarCount);
        // System.out.println(maxNumberOfStates);
        
        int inVarCount  = JSONInputParser.GetIntFromJSONOBJ(jsonObj,"inVarCount", -1);
        String[] inVars = JSONInputParser.GetStrArrFromJSONOBJ(jsonObj,"inVarNames");
        
        int outVarCount  = JSONInputParser.GetIntFromJSONOBJ(jsonObj,"outVarCount", -1);
        String[] outVars = JSONInputParser.GetStrArrFromJSONOBJ(jsonObj,"outVarNames");

        String[] totalInVars = new String[stateVarCount + inVarCount];
        //Validation of Counts and Names of Variables
        if (inVarCount != inVars.length) {
            Data_JSON.put("Problem", "There's a mismatch between the Count of InVariables and the number of InVariables provided");
            System.out.println(Data_JSON.toString());
            return;
        }
        if (outVarCount != outVars.length) {
            Data_JSON.put("Problem", "There's a mismatch between the Count of OutVariables and the number of OutVariables provided");
            System.out.println(Data_JSON.toString());
            return;
        }
        
        //making a joint inVarNameList for the expressions
        for (int i = stateVarCount - 1; i >= 0; i--) {
            totalInVars[i] = "Q" + i;
        }
        for (int i = 0; i < inVarCount; i++) {
            totalInVars[i + stateVarCount] = inVars[i];
        }


        int linkPathCount = JSONInputParser.GetIntFromJSONOBJ(jsonObj,"linkPathsCount", -1);

        String[] mooreOutputs = JSONInputParser.GetStrArrFromJSONOBJ(jsonObj, "mooreOutputs");
        State[] states = new State[maxNumberOfStates];//indices indicate stateIDs
        // for (int i = 0; i < maxNumberOfStates; i++) {
        //     states[i] = new State(i, mooreOutputs[i]); // so that we dont have any null states
        // }
        boolean[] addedStates = new boolean[maxNumberOfStates];
        
        //Checking the validity of Moore outputs
        if (CheckValidityOfMooreOutputs(mooreOutputs, outVarCount) == INVALID_TERMS) {
            Data_JSON.put("Problem", "Invalid Moore Output(Make sure that it's a valid Boolean term having the same number of bits as the outVar count)");
            System.out.println(Data_JSON.toString());
            return;
        }
        //Validation and Addition of LinkPaths
        JSONArray linkPaths = jsonObj.getJSONArray("linkPaths");
        if (AssignStatesFromLinkPaths(linkPaths, linkPathCount, addedStates, states, mooreOutputs, inVarCount, outVarCount) == INVALID_TERMS) {
            Data_JSON.put("Problem", "Invalid Link Input/Output String (Make sure that it's a valid Boolean term having the same number of bits as the inVar count)");
            System.out.println(Data_JSON.toString());
            return;
        }
        // for (int i = 0; i < linkPathCount; i++) {
        //     JSONObject currLink = linkPaths.getJSONObject(i);
            
        //     String fromState = JSONInputParser.GetStrFromJSONOBJ(currLink, "fromState", "").replaceAll("S",""); //to only get ID
        //     int fromStateID = Integer.parseInt(fromState);
        //     //no need to handle non-numeric data since it's prohibited in the front-end
        //     String toState = JSONInputParser.GetStrFromJSONOBJ(currLink, "toState", "").replaceAll("S",""); //to only get ID
        //     int toStateID = Integer.parseInt(toState);
            
        //     addedStates[fromStateID] = true;
        //     //mark only the from states so that we dont have to add it in the table afterwards..it's already there
        //     //Adding the state on it's first encounter
        //     if (states[fromStateID] == null) {
        //         states[fromStateID] = new State(fromStateID, mooreOutputs[fromStateID]);
        //     }
        //     if (states[toStateID] == null) {
        //         states[toStateID] = new State(toStateID, mooreOutputs[toStateID]);
        //     }
        //     //Adding the Input And Output String of CurrLink
        //     String inStr = JSONInputParser.GetStrFromJSONOBJ(currLink, "inStr", ""); 
        //     String outStr = JSONInputParser.GetStrFromJSONOBJ(currLink, "outStr", ""); 
        //     if (!Term.isBooleanString(inStr) || !Term.isBooleanString(outStr) || inStr.length() != inVarCount || outStr.length() != outVarCount) {
        //         Data_JSON.put("Problem", "Invalid Link Input/Output String (Make sure that it's a valid Boolean term having the same number of bits as the inVar count)");
        //         System.out.println(Data_JSON.toString());
        //         return;
        //     }
        //     states[fromStateID].AddLinkTo(states[toStateID], inStr, outStr);//since we store outLiks Only
        // }

        boolean optimalAssignment = false;
        //Swapping the stateID to it's Boolean Representation

        //Now We Have The Entirety of Input-Data
        StateGraph stateGraph = new StateGraph(inVars, outVars);
        for (State state : states) {
            if (state != null) 
                stateGraph.AddState(state);
        }

        // System.out.println("Count of states in state graph : " + stateGraph.numberOfStates);

        String laTexStr = stateGraph.ToLaTexCircularGraph(7);
        Data_JSON.put("LaTexStateGraph", laTexStr);

        PLATable table = stateGraph.ToPLATable(mooreOutputs);
        // System.out.println("Count of state bits in table : " + table.stateVarCount);
        //to get minimized expressions and steps of solution of output bits
        //Adding stateVars solution with steps
        JSONArray nullStateVarOutputs = new JSONArray();
        JSONObject stateVarOutputs = new JSONObject();
        // System.out.println("TOTAL IN VARS");
        // for (String str : totalInVars) {
        //     System.out.print(str + " ");
        // }
        
        for (int i = 0; i < table.stateVarCount; i++) {
            try {
                // System.out.println("Trying to process " + totalInVars[i] + " now");
                JSONObject state_var_Data = new JSONObject();
                String[] terms = table.GetTermsOf(true, i, optimalAssignment?null/*TODO:optimal assignment */:null);
                // System.out.println("→ terms.length = " + (terms != null ? terms.length : -1));

                
                SolutionData solutionData ;
                // JSONArray debugObj = new JSONArray();
                // if (terms != null) {
                //     for (String string : terms) {
                //         debugObj.put(string);
                //     }
                // }
                // Data_JSON.put("DEBUG Terms for " + totalInVars[i], debugObj); // Will still insert []
                // System.out.println("PUT => DEBUG Terms for " + totalInVars[i] + " = " + debugObj);
                // System.out.println("After put, current keys: " + Data_JSON.keySet());

                //Assigning the SolutionData
                if (terms.length == 0) { // it's fully zero--no function
                    nullStateVarOutputs.put(totalInVars[i]);
                    // Data_JSON.put("DUBUG " + i + " state var", totalInVars[i] + " SHOULD BE ADDED");

                }
                else{
                    solutionData = table.SolveVariableAt(true, i, optimalAssignment?null/*TODO:optimal assignment */:null);
                    JSONObject simplificationData_JSON = JSONOutputFormatter.SimplificationDataToResultJSON(solutionData.simplificationData);
                    state_var_Data.put("SimplificationData", simplificationData_JSON);
                    
                    JSONObject PIChartData_JSON = JSONOutputFormatter.PIChartDataToResultJSON(solutionData.simplificationData, solutionData.optimizationResult, terms);
                    state_var_Data.put("PIChartData", PIChartData_JSON);
                    
                    JSONObject OutputData_JSON = JSONOutputFormatter.OutputDataToResultJSON(solutionData.optimizationResult, totalInVars);
                    state_var_Data.put("OutputData", OutputData_JSON);
                    stateVarOutputs.put(totalInVars[i], state_var_Data);
                }


            } catch (Exception e) {
                Data_JSON.put("Problem", "An Issue occured while trying to find solution." + e.getMessage() + Arrays.toString(e.getStackTrace()));
                System.out.println(Data_JSON.toString());
                return;
            }
            
        }
        
        Data_JSON.put("StateVarOutputs", stateVarOutputs);
        Data_JSON.put("NullStateVars", nullStateVarOutputs);

        //Adding outVars solution with steps
        JSONArray nullVarOutputs = new JSONArray();
        JSONObject outVarOutputs = new JSONObject();
        
        for (int i = 0; i < table.outVarCount; i++) {
            try {
                // System.out.println("Trying to process " + outVars[i] + " now");
                JSONObject out_var_Data = new JSONObject();
                String[] terms = table.GetTermsOf(false, i, optimalAssignment?null/*TODO:optimal assignment */:null);
                // System.out.println("→ terms.length = " + (terms != null ? terms.length : -1));

                SolutionData solutionData ;
                // JSONArray debugObj = new JSONArray();
                // if (terms != null) {
                //     for (String string : terms) {
                //         debugObj.put(string);
                //     }
                // }
                // Data_JSON.put("DEBUG Terms for " + outVars[i], debugObj);
                // System.out.println("PUT => DEBUG Terms for " + outVars[i] + " = " + debugObj);
                // System.out.println("After put, current keys: " + Data_JSON.keySet());
                
                if (terms.length == 0) {
                    nullVarOutputs.put(outVars[i]);
                    // Data_JSON.put("DUBUG " + i + " out var", outVars[i] + " SHOULD BE ADDED");
                }

                else{
                    solutionData = table.SolveVariableAt(false, i, optimalAssignment?null/*TODO:optimal assignment */:null);
                    JSONObject simplificationData_JSON = JSONOutputFormatter.SimplificationDataToResultJSON(solutionData.simplificationData);
                    out_var_Data.put("SimplificationData", simplificationData_JSON);
                    JSONObject PIChartData_JSON = JSONOutputFormatter.PIChartDataToResultJSON(solutionData.simplificationData, solutionData.optimizationResult, terms);
                    out_var_Data.put("PIChartData", PIChartData_JSON);
                    JSONObject OutputData_JSON = JSONOutputFormatter.OutputDataToResultJSON(solutionData.optimizationResult, totalInVars);
                    out_var_Data.put("OutputData", OutputData_JSON);
                    outVarOutputs.put(outVars[i], out_var_Data); //since its after the stateVars
                }


            } catch (Exception e) {
                Data_JSON.put("Problem", "An Issue occured while trying to find solution.");
                System.out.println(Data_JSON.toString());
                return;
            }
        }
        
        Data_JSON.put("OutVarOutputs", outVarOutputs);
        Data_JSON.put("NullOutVars", nullVarOutputs);

        System.out.println(Data_JSON.toString());
    }
    
}
