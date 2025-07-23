package com.github.digilab.tools.core;
import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONObject;

//Deals With the JSONInput sent from the python code and parses it into valid arguments for the LinkPathsSolver 
//and then sends the solution back as a JSON String
public class LinkPathsSolverWebHandler {
    
    public static final int INVALID_TERMS = -1;
    public static final int VALID_TERMS  = 1;
    
    //Goes over the mooreOutputs and checks if there's an invalid boolean output expression or one of invalid length
    public static int CheckValidityOfMooreOutputs(String[] mooreOutputs, int outVarCount){
        for (int i = 0; i < mooreOutputs.length; i++) {
            if (!Term.isBooleanString(mooreOutputs[i]) || mooreOutputs[i].length() != outVarCount) {
                return INVALID_TERMS;
            }
        }
        return VALID_TERMS;
    }
    
    // Goes over the JSONArray and breaks it down to the fromState, toState, inStr, outStr and inserts it to the parameter "states"
    //Note That : states array should be allocated with length at least equal to the maximum number of states for the given states bits
    public static int AssignStatesFromLinkPaths(JSONArray linkPaths, int linkPathCount, State[] states , String[] mooreOutputs , int inVarCount, int outVarCount){
        for (int i = 0; i < linkPathCount; i++) {
            JSONObject currLink = linkPaths.getJSONObject(i);
            
            String fromState = JSONInputParser.GetStrFromJSONOBJ(currLink, "fromState", "").replaceAll("S",""); //to only get ID
            int fromStateID = Integer.parseInt(fromState);
            //no need to handle non-numeric data since it's prohibited in the front-end
            String toState = JSONInputParser.GetStrFromJSONOBJ(currLink, "toState", "").replaceAll("S",""); //to only get ID
            int toStateID = Integer.parseInt(toState);
            
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
        String jsonStr = args[0];

        JSONObject jsonObj = new JSONObject(jsonStr);
        
        //The Result (Solution) JSONObject
        JSONObject Data_JSON = new JSONObject();

        //Gets the number of state variables and calculates the largest possible number of states 
        // which can be used to allocate the arrays
        int stateVarCount = JSONInputParser.GetIntFromJSONOBJ(jsonObj,"stateVarCount", -1);
        int maxNumberOfStates = (int)Math.pow(2, stateVarCount);
        
        // Gets the variable counts and names to prepare for validation and for it to be used as arguments later
        int inVarCount  = JSONInputParser.GetIntFromJSONOBJ(jsonObj,"inVarCount", -1);
        int outVarCount  = JSONInputParser.GetIntFromJSONOBJ(jsonObj,"outVarCount", -1);
        String[] inVars = JSONInputParser.GetStrArrFromJSONOBJ(jsonObj,"inVarNames");
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
        

        //Making a joint inVarNameList for the expressions
        for (int i = 0; i < stateVarCount; i++) {
            totalInVars[i] = "Q" + (stateVarCount - i - 1); 
            // to have the conventional order : MSB(most significant bit) has the highest number
        }
        for (int i = 0; i < inVarCount; i++) {
            totalInVars[i + stateVarCount] = inVars[i];
        }


        int linkPathCount = JSONInputParser.GetIntFromJSONOBJ(jsonObj,"linkPathsCount", -1);
        String[] mooreOutputs = JSONInputParser.GetStrArrFromJSONOBJ(jsonObj, "mooreOutputs");
        State[] states = new State[maxNumberOfStates];//indices indicate stateIDs
        
        //Checking the validity of Moore outputs
        if (CheckValidityOfMooreOutputs(mooreOutputs, outVarCount) == INVALID_TERMS) {
            Data_JSON.put("Problem", "Invalid Moore Output(Make sure that it's a valid Boolean term having the same number of bits as the outVar count)");
            System.out.println(Data_JSON.toString());
            return;
        }
        //Validation and Addition of LinkPaths
        JSONArray linkPaths = jsonObj.getJSONArray("linkPaths");
        if (AssignStatesFromLinkPaths(linkPaths, linkPathCount, states, mooreOutputs, inVarCount, outVarCount) == INVALID_TERMS) {
            Data_JSON.put("Problem", "Invalid Link Input/Output String (Make sure that it's a valid Boolean term having the same number of bits as the inVar count)");
            System.out.println(Data_JSON.toString());
            return;
        }

        //TODO: Optimal state assignment algorithm
        boolean optimalAssignment = false;
        //Swapping the stateID to it's Boolean Representation

        //Now We Have The Entirety of Input-Data
        StateGraph stateGraph = new StateGraph(inVars, outVars);
        for (State state : states) {
            if (state != null)  //only add the needed states to not have non neccessary states
                stateGraph.AddState(state);
        }

        String laTexStr = stateGraph.ToLaTexCircularGraph(7);

        PLATable table = stateGraph.ToPLATable(mooreOutputs, stateVarCount);
        //to get minimized expressions and steps of solution of output bits
        

        JSONArray nullStateVarOutputs = new JSONArray();
        JSONObject stateVarOutputs = new JSONObject();
        
        JSONArray nullVarOutputs = new JSONArray();
        JSONObject outVarOutputs = new JSONObject();
        
        //Adding The Solutions to State Bits
        for (int i = 0; i < table.stateVarCount; i++) {
            try {
                JSONObject state_var_Data = new JSONObject();
                String[] terms = table.GetTermsOf(true, i, optimalAssignment?null/*TODO:optimal assignment */:null);

                
                SolutionData solutionData ;
                //Assigning the SolutionData
                if (terms.length == 0) { // it's fully zero--no function

                    nullStateVarOutputs.put(totalInVars[i]);
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

        //Adding The Solutions to Non-State Output Bits
        for (int i = 0; i < table.outVarCount; i++) {
            try {
                JSONObject out_var_Data = new JSONObject();
                String[] terms = table.GetTermsOf(false, i, optimalAssignment?null/*TODO:optimal assignment */:null);

                SolutionData solutionData ;
                if (terms.length == 0) {
                    nullVarOutputs.put(outVars[i]);
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

        
        Data_JSON.put("LaTexStateGraph", laTexStr);

        Data_JSON.put("StateVarOutputs", stateVarOutputs);
        Data_JSON.put("NullStateVars", nullStateVarOutputs);
        
        Data_JSON.put("OutVarOutputs", outVarOutputs);
        Data_JSON.put("NullOutVars", nullVarOutputs);

        System.out.println(Data_JSON.toString());
    }
    
}
