package com.github.digilab.tools.core;

import java.util.Arrays;

import org.json.*;

public class QuineMckluskyWebHandler {

    private static int GetIntFromJSONOBJ(JSONObject jsonObj, String KeyForVar , int fallbackVal){
        Object jsonValue = jsonObj.get(KeyForVar);
        if (jsonValue instanceof String) {
            try {
                int intVal = Integer.parseInt((String)jsonValue);
                return intVal;
            } catch (Exception e) {
                System.err.println("Mismatching Value Type At Key");
                return fallbackVal;
            }
        }
        System.err.println("Mismatching Value Type At Key");
        return -fallbackVal;
    }
    public static int GetNumberOfVariablesFromJSONOBJ(JSONObject jsonObj, String KeyForNumberOfVars){
        int numberOfVariables = GetIntFromJSONOBJ(jsonObj, KeyForNumberOfVars, -1);
        if (numberOfVariables == -1) {
            System.err.println("No 'NumberOfVar' was obtained returning -1");
        }
        return numberOfVariables;
    }
    
    private static String[] GetStrArrFromJSONOBJ(JSONObject jsonObj, String KeyForVar){
        if (!jsonObj.has(KeyForVar)) {
            System.err.println("Invalid Key to obtain MinTerms");
            return null;
        }
        Object variableList = jsonObj.get(KeyForVar);
        if (variableList instanceof JSONArray) {
            JSONArray jsonArr = (JSONArray)variableList;
            String[] StrArr = new String[jsonArr.length()];
            for(int i = 0; i < jsonArr.length(); i++){
                StrArr[i] = jsonArr.getString(i);
            }
            return StrArr;
        }
        if (variableList instanceof String) {
            String[] StrArr = new String[1];
            StrArr[0] = (String)variableList;
            
            return StrArr;
        }
        System.err.println("Mismatching Value Type At " + KeyForVar);
        return null;
    }
    private static int[] GetIntArrFromJSONOBJ(JSONObject jsonObj, String KeyForVar){
        if (!jsonObj.has(KeyForVar)) {
            System.err.println("Invalid Key to obtain MinTerms");
            return null;
        }
        Object val = jsonObj.get(KeyForVar);
        if (val instanceof JSONArray) {
            JSONArray jsonArr = (JSONArray)val;
            int[] intArr = new int[jsonArr.length()];
            for(int i = 0; i < jsonArr.length(); i++){
                intArr[i] = Integer.parseInt(jsonArr.getString(i));
            }
            return intArr;
        }
        if (val instanceof String) {
            int[] intArr = new int[1];
            intArr[0] = (int)val;
            
            return intArr;
        }
        System.err.println("Mismatching Value Type At " + KeyForVar);
        return null;
    }
    
    public static String[] GetVariableListFromJSONOBJ(JSONObject jsonObj, String KeyForVariables){
        String[] varList = GetStrArrFromJSONOBJ(jsonObj,KeyForVariables);
        if (varList == null) {
            System.err.println("Unable To Obtain Variable Names.. Returning Null");
            return null;
        }
        return varList;
    }
    
    public static int[] GetMinTermIntsFromJSONOBJ(JSONObject jsonObj, String KeyForMinTerms){
        int[] minTerms = GetIntArrFromJSONOBJ(jsonObj,KeyForMinTerms);
        if (minTerms == null) {
            System.err.println("Unable To Obtain MinTerms.. Returning Null");
            return null;
        }
        return minTerms;
    }
    public static String[] GetMinTermStrsFromJSONOBJ(JSONObject jsonObj, String KeyForMinTerms){
        String[] minTerms = GetStrArrFromJSONOBJ(jsonObj,KeyForMinTerms);
        if (minTerms == null) {
            System.err.println("Unable To Obtain MinTerms.. Returning Null");
            return null;
        }
        return minTerms;
    }
    
    public static String[] GetDontCaresFromJSONOBJ(JSONObject jsonObj, String KeyForDontCares){
        String[] dontCares = GetStrArrFromJSONOBJ(jsonObj,KeyForDontCares);
        if (dontCares == null) {
            System.err.println("Unable To Obtain DontCares.. Returning Null");
            return null;
        }
        return dontCares;
    }
    public static int[] GetDontCaresIntsFromJSONOBJ(JSONObject jsonObj, String KeyForDontCares){
        int[] dontCares = GetIntArrFromJSONOBJ(jsonObj,KeyForDontCares);
        if (dontCares == null) {
            System.err.println("Unable To Obtain MinTerms.. Returning Null");
            return null;
        }
        return dontCares;
    }
    
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
    
    public static void main(String[] args) {
        String jsonStr = args[0];
        JSONObject jsonObj = new JSONObject(jsonStr);
        int numberOfVariables = GetNumberOfVariablesFromJSONOBJ(jsonObj,"NumberOfVars");
        String[] variables = GetVariableListFromJSONOBJ(jsonObj,"Variables");
        int[] minTerms = GetMinTermIntsFromJSONOBJ(jsonObj,"MinTerms");
        int[] dontCares = GetDontCaresIntsFromJSONOBJ(jsonObj, "DontCares");
        
        if (numberOfVariables < 1) {
            System.err.println("Invalid NumberOfVars");
            return;
        }
        int maxAllowedVal = (int)Math.pow(2, numberOfVariables);
        if (Arrays.stream(minTerms).max().getAsInt() >= maxAllowedVal) {
            System.err.println("Max Possible MinTerm for specified NumberOfVars is" + maxAllowedVal);
            return;
        }
        if (Arrays.stream(dontCares).max().getAsInt() >= maxAllowedVal) {
            System.err.println("Max Possible DontCares for specified NumberOfVars is" + maxAllowedVal);
            return;
        }

        SimplificationData simplificationData = QuineMckluskySolver.getPrimeImplicantsMinTerms(minTerms, dontCares, numberOfVariables);

        // Find the optimal minimized expression
        OptimizationResult optimizationRes = QuineMckluskySolver.getOptimalExpression(
            QuineMckluskySolver.termListToStrArr(simplificationData.primeImplicants), minTerms, numberOfVariables);
        
        JSONObject Data_JSON = new JSONObject();
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