package com.github.digilab.tools.core;
import org.json.JSONArray;
import org.json.JSONObject;

public class LinkPathsSolverWebHandler {
    public static void main(String[] args){
        String jsonStr = args[0];
        JSONObject jsonObj = new JSONObject(jsonStr);
        JSONObject Data_JSON = new JSONObject();

        int stateVarCount = JSONInputParser.GetIntFromJSONOBJ(jsonObj,"stateVarCount", -1);
        int maxNumberOfStates = (int)Math.pow(2, stateVarCount);
        
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
        
        //Checking the validity of Moore outputs
        for (int i = 0; i < mooreOutputs.length; i++) {
            if (!Term.isBooleanString(mooreOutputs[i]) || mooreOutputs[i].length() != inVarCount) {
                Data_JSON.put("Problem", "Invalid Moore Output(Make sure that it's a valid Boolean term having the same number of bits as the inVar count)");
                System.out.println(Data_JSON.toString());
                return;
            }
        }
        //Validation and Addition of LinkPaths
        JSONArray linkPaths = jsonObj.getJSONArray("linkPaths");
        for (int i = 0; i < linkPathCount; i++) {
            JSONObject currLink = linkPaths.getJSONObject(i);
            
            String fromState = JSONInputParser.GetStrFromJSONOBJ(currLink, "fromState", "").replaceAll("S",""); //to only get ID
            int fromStateID = Integer.parseInt(fromState);
            //no need to handle non-numeric data since it's prohibited in the front-end
            String toState = JSONInputParser.GetStrFromJSONOBJ(currLink, "toState", "").replaceAll("S",""); //to only get ID
            int toStateID = Integer.parseInt(toState);

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
                Data_JSON.put("Problem", "Invalid Link Input/Output String (Make sure that it's a valid Boolean term having the same number of bits as the inVar count)");
                System.out.println(Data_JSON.toString());
                return;
            }
            states[fromStateID].AddLinkTo(states[toStateID], inStr, outStr);//since we store outLiks Only
        }

        boolean optimalAssignment = false;
        //Swapping the stateID to it's Boolean Representation

        //Now We Have The Entirety of Input-Data
        StateGraph stateGraph = new StateGraph(inVars, outVars);
        for (State state : states) {
            if (state != null) 
                stateGraph.AddState(state);
        }

        String laTexStr = stateGraph.ToLaTexCircularGraph(7);
        Data_JSON.put("LaTexStateGraph", laTexStr);

        PLATable table = stateGraph.ToPLATable();
        //Adding DontCare States
        for (int i = 0 ; i < states.length; i++) {
            if (states[i] == null) { // then all outputs are dont cares
                table.AddRow(new RelationRow(table.GetStateStrAssignment("S" + i, optimalAssignment?null/*TODO:optimal assignment */:null), 
                            "DC", 
                            "-".repeat(inVarCount), 
                            "-".repeat(outVarCount)));
            }
        }

        //to get minimized expressions and steps of solution of output bits
        
        //Adding stateVars solution with steps
        JSONObject stateVarOutputs = new JSONObject();
        for (int i = 0; i < table.stateVarCount; i++) {
            JSONObject state_var_Data = new JSONObject();
            String[] terms = table.GetTermsOf(true, i, optimalAssignment?null/*TODO:optimal assignment */:null);
            SolutionData solutionData ;
            try {
                solutionData = table.SolveVariableAt(true, i, optimalAssignment?null/*TODO:optimal assignment */:null);

            } catch (Exception e) {
                Data_JSON.put("Problem", "An Issue occured while trying to find solution.");
                System.out.println(Data_JSON.toString());
                return;
            }
            
            JSONObject simplificationData_JSON = QuineMckluskyWebHandler.SimplificationDataToResultJSON(solutionData.simplificationData);
            JSONObject PIChartData_JSON = QuineMckluskyWebHandler.PIChartDataToResultJSON(solutionData.simplificationData, solutionData.optimizationResult, terms);
            JSONObject OutputData_JSON = QuineMckluskyWebHandler.OutputDataToResultJSON(solutionData.optimizationResult, totalInVars);

            state_var_Data.put("SimplificationData", simplificationData_JSON);
            state_var_Data.put("PIChartData", PIChartData_JSON);
            state_var_Data.put("OutputData", OutputData_JSON);

            stateVarOutputs.put(totalInVars[i], state_var_Data);
        }
        Data_JSON.put("StateVarOutputs", stateVarOutputs);

        //Adding outVars solution with steps
        JSONObject outVarOutputs = new JSONObject();
        for (int i = 0; i < table.outVarCount; i++) {
            JSONObject out_var_Data = new JSONObject();
            String[] terms = table.GetTermsOf(false, i, optimalAssignment?null/*TODO:optimal assignment */:null);
            SolutionData solutionData ;
            try {
                solutionData = table.SolveVariableAt(false, i, optimalAssignment?null/*TODO:optimal assignment */:null);

            } catch (Exception e) {
                Data_JSON.put("Problem", "An Issue occured while trying to find solution.");
                System.out.println(Data_JSON.toString());
                return;
            }

            JSONObject simplificationData_JSON = QuineMckluskyWebHandler.SimplificationDataToResultJSON(solutionData.simplificationData);
            JSONObject PIChartData_JSON = QuineMckluskyWebHandler.PIChartDataToResultJSON(solutionData.simplificationData, solutionData.optimizationResult, terms);
            JSONObject OutputData_JSON = QuineMckluskyWebHandler.OutputDataToResultJSON(solutionData.optimizationResult, totalInVars);

            out_var_Data.put("SimplificationData", simplificationData_JSON);
            out_var_Data.put("PIChartData", PIChartData_JSON);
            out_var_Data.put("OutputData", OutputData_JSON);

            outVarOutputs.put(totalInVars[i + stateVarCount], out_var_Data); //since its after the stateVars
        }
        Data_JSON.put("OutVarOutputs", outVarOutputs);

    }
    
}
