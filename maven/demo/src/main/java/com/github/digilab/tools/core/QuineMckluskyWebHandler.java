package com.github.digilab.tools.core;

import java.util.Arrays;

import org.json.*;

public class QuineMckluskyWebHandler {


    // public static int GetNumberOfVariablesFromJSONOBJ(JSONObject jsonObj, String KeyForNumberOfVars){
    //     int numberOfVariables = GetIntFromJSONOBJ(jsonObj, KeyForNumberOfVars, -1);
    //     if (numberOfVariables == -1) {
    //         // System.err.println("No 'NumberOfVar' was obtained returning -1");
    //     }
    //     return numberOfVariables;
    // }
    

    // public static String[] GetVariableListFromJSONOBJ(JSONObject jsonObj, String KeyForVariables){
    //     String[] varList = GetStrArrFromJSONOBJ(jsonObj,KeyForVariables);
    //     if (varList == null) {
    //         // System.err.println("Unable To Obtain Variable Names.. Returning Null");
    //         return null;
    //     }
    //     return varList;
    // }
    
    // public static int[] GetMinTermIntsFromJSONOBJ(JSONObject jsonObj, String KeyForMinTerms){
    //     int[] minTerms = GetIntArrFromJSONOBJ(jsonObj,KeyForMinTerms);
    //     if (minTerms == null) {
    //         System.err.println("Unable To Obtain MinTerms.. Returning Null");
    //         return null;
    //     }
    //     return minTerms;
    // }
    // public static String[] GetMinTermStrsFromJSONOBJ(JSONObject jsonObj, String KeyForMinTerms){
    //     String[] minTerms = GetStrArrFromJSONOBJ(jsonObj,KeyForMinTerms);
    //     if (minTerms == null) {
    //         System.err.println("Unable To Obtain MinTerms.. Returning Null");
    //         return null;
    //     }
    //     return minTerms;
    // }
    
    // public static String[] GetDontCaresFromJSONOBJ(JSONObject jsonObj, String KeyForDontCares){
    //     String[] dontCares = GetStrArrFromJSONOBJ(jsonObj,KeyForDontCares);
    //     if (dontCares == null) {
    //         // System.err.println("Unable To Obtain DontCares.. Returning Null");
    //         return null;
    //     }
    //     return dontCares;
    // }
    // public static int[] GetDontCaresIntsFromJSONOBJ(JSONObject jsonObj, String KeyForDontCares){
    //     int[] dontCares = GetIntArrFromJSONOBJ(jsonObj,KeyForDontCares);
    //     if (dontCares == null) {
    //         // System.err.println("Unable To Obtain MinTerms.. Returning Null");
    //         return null;
    //     }
    //     return dontCares;
    // }
    
    public static JSONObject SimplificationDataToResultJSON(SimplificationData data){
        JSONObject result = new JSONObject();
        result.put("ItrCount", data.iterationCount);
        result.put("GroupsPerIteration",data.GetGroupCountPerItr());
        for (iteration itr : data.GetAllIterations()) {
            JSONObject itr_json = new JSONObject();
            for (Group grp : itr.groups) {
                itr_json.put("Gp" + grp.numberOfOnes, QuineMckluskySolver.termListToStrArr(grp.terms));
            }
            result.put("Itr" + itr.iterationNumber , itr_json);
        }
        return result;
    }
    public static JSONObject PIChartDataToResultJSON(SimplificationData simplificationData, OptimizationResult optimizationRes , int[] minTerms){
        JSONObject result = new JSONObject();
        result.put("PIChart", optimizationRes.piChart);
        result.put("CoveringTerms", QuineMckluskySolver.termListToStrArr(simplificationData.primeImplicants));
        result.put("MinTerms", minTerms);
        return result;
    }
    public static JSONObject OutputDataToResultJSON(OptimizationResult optimizationRes, String[] variables){
        JSONObject result = new JSONObject();
        result.put("OptimizedBoolOutput", QuineMckluskySolver.termListToStrArr(optimizationRes.terms));

        String optimizedVarOutput = QuineMckluskySolver.GetExpression(optimizationRes.terms, variables);
        result.put("OptimizedVarOutput", optimizedVarOutput);
        return result;
    }
    
    private static boolean AllTermsWithinRange(int[]terms, int minVal, int maxVal){
        if (Arrays.stream(terms).max().getAsInt() > maxVal || Arrays.stream(terms).min().getAsInt()  < minVal) {
            return false;
        }
        return true;
    }
    public static void main(String[] args) {
        String jsonStr = args[0];
        JSONObject jsonObj = new JSONObject(jsonStr);
        int numberOfVariables = JSONInputParser.GetIntFromJSONOBJ(jsonObj,"NumberOfVars", -1);
        String[] variables = JSONInputParser.GetStrArrFromJSONOBJ(jsonObj,"Variables");
        int[] minTerms = JSONInputParser.GetIntArrFromJSONOBJ(jsonObj,"MinTerms");
        int[] dontCares = JSONInputParser.GetIntArrFromJSONOBJ(jsonObj, "DontCares");

        JSONObject Data_JSON = new JSONObject();
        
        //EDGE-CASE HANDLING
        //numberOfVariables Edge-Case Handling
        if (numberOfVariables < 1 || numberOfVariables > 10) {
            Data_JSON.put("Problem", "Invalid number of variables");
            System.out.println(Data_JSON.toString());
            return;
        }
        //Variables' Array Edge-Case Handling
        if (variables == null || variables.length != numberOfVariables) { 
            Data_JSON.put("Problem", "There's a mismathch between the NumberOfVars and the count of the Variables provided");
            System.out.println(Data_JSON.toString());
            return;
        }

        int maxAllowedVal = (int)Math.pow(2, numberOfVariables) - 1;
        int minAllowedVal = 0;


        //MinTerm Edge-Case Handling
        if (minTerms == null || minTerms.length == 0) { 
            Data_JSON.put("Problem", "No MinTerms Were Given");
            System.out.println(Data_JSON.toString());
            return;
        }
        //if it was null it wont procceed so it wont dereference a null pointer
        if (!QuineMckluskyWebHandler.AllTermsWithinRange(minTerms, minAllowedVal, maxAllowedVal)) {
            Data_JSON.put("Problem", "Largest Allowed MinTerm for specified NumberOfVars is " + maxAllowedVal);
            System.out.println(Data_JSON.toString());
            return;
        }

        //DontCare Edge-Case Handling
        if (dontCares != null && dontCares.length > 0 ) {
            if (!QuineMckluskyWebHandler.AllTermsWithinRange(dontCares, minAllowedVal, maxAllowedVal)) {
                Data_JSON.put("Problem", "Largest Allowed DontCare for specified NumberOfVars is " + maxAllowedVal);
                System.out.println(Data_JSON.toString());
                return;
            }
        }
        //we couldn't resolve the nesting here since unlike the previous case there isn't an early exit point here
        //here if dont cares weren't given we just procceed with null and that's allowed
        
        //Core Logic Edge-Case Handling
        SimplificationData simplificationData ;
        OptimizationResult optimizationRes;
        try {
            simplificationData = QuineMckluskySolver.getPrimeImplicantsMinTerms(minTerms, dontCares, numberOfVariables);
            // Find the optimal minimized expression
            optimizationRes = QuineMckluskySolver.getOptimalExpression(
                QuineMckluskySolver.termListToStrArr(simplificationData.primeImplicants), minTerms, numberOfVariables);
        } catch (Exception e) {
            Data_JSON.put("Problem", "An Error Occured while processing ..Failed To Give Output");
            System.out.println(Data_JSON.toString());
            return;
        }

        //Output-JSON Setup
        JSONObject simplificationData_JSON = SimplificationDataToResultJSON(simplificationData);
        JSONObject PIChartData_JSON = PIChartDataToResultJSON(simplificationData, optimizationRes, minTerms);
        JSONObject OutputData_JSON = OutputDataToResultJSON(optimizationRes, variables);

        Data_JSON.put("SimplificationData", simplificationData_JSON);
        Data_JSON.put("PIChartData", PIChartData_JSON);
        Data_JSON.put("OutputData", OutputData_JSON);

        System.out.println(Data_JSON.toString());
        return;
    }
}