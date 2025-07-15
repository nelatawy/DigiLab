package com.github.digilab.tools.core;

import java.util.Arrays;

import org.json.*;

public class QuineMckluskyWebHandler {


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
            Data_JSON.put("Problem", "There's a mismatch between the NumberOfVars and the number of Variables provided");
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
        JSONObject simplificationData_JSON = JSONOutputFormatter.SimplificationDataToResultJSON(simplificationData);
        JSONObject PIChartData_JSON = JSONOutputFormatter.PIChartDataToResultJSON(simplificationData, optimizationRes, minTerms);
        JSONObject OutputData_JSON = JSONOutputFormatter.OutputDataToResultJSON(optimizationRes, variables);

        Data_JSON.put("SimplificationData", simplificationData_JSON);
        Data_JSON.put("PIChartData", PIChartData_JSON);
        Data_JSON.put("OutputData", OutputData_JSON);

        System.out.println(Data_JSON.toString());
        return;
    }
}