package com.github.digilab.tools.core;

import org.json.JSONArray;
import org.json.JSONObject;

public class FlipFlopRealizerWebHandler {
    public static void main(String[] args){
        String jsonStr = args[0];
        JSONObject jsonObj = new JSONObject(jsonStr);
        JSONObject outJSON = new JSONObject();

        String[] varNames = JSONInputParser.GetStrArrFromJSONOBJ(jsonObj, "VarNames");
        String stateVar = JSONInputParser.GetStrFromJSONOBJ(jsonObj, "StateVarName", "");

        // String[] varNames = {"A","B","C"};
        // String stateVar = "Q";
        String[] totalInVars = new String[varNames.length + 1];
        
        totalInVars[0] = stateVar;
        for (int i = 1; i < varNames.length + 1; i++) {
            totalInVars[i] = varNames[i - 1];
        }
        
        String expression = JSONInputParser.GetStrFromJSONOBJ(jsonObj, "Expression", "");
        // String expression = "QAB + BC'";

        //For Testing Default Assignment SR FlipFlop
        char[][] gridS = {{'0','1'},{'0','-'}}; //SR S FF grid
        char[][] gridR = {{'-','0'},{'1','0'}}; //SR R FF grid
        FFRuleSet ffRuleSet = new FFRuleSet();
        String[] controlLineNames = {"S","R"};
        ControlLineRuleSet controlLineRuleSet1 = new ControlLineRuleSet("S", gridS);
        ControlLineRuleSet controlLineRuleSet2 = new ControlLineRuleSet("R", gridR);
        ffRuleSet.AddControlLine(controlLineRuleSet1);
        ffRuleSet.AddControlLine(controlLineRuleSet2);

        PLATable table = FlipFlopRealizer.GetTableFromExpression(expression, varNames, stateVar);
        // System.out.println(table.toString());
        FlipFlopRealizer ffRealizer = new FlipFlopRealizer(ffRuleSet, table);
        for (int i = 0; i < ffRuleSet.numberOfControlLines; i++) {
            // JSONObject controlLineSol = new JSONObject();
            SolutionData solData = ffRealizer.SolveControlLineAt(i);
            String solExpression = QuineMckluskySolver.GetExpression(solData.optimizationResult.terms, totalInVars);
            // JSONObject simplificationData_JSON = JSONOutputFormatter.SimplificationDataToResultJSON(solData.simplificationData);
            // controlLineSol.put("SimplificationData", simplificationData_JSON);
            outJSON.put(controlLineNames[i], solExpression);

        }
        System.out.println(outJSON.toString());
    }
}
