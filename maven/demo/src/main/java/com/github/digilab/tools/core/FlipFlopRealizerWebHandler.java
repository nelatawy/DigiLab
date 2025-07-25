package com.github.digilab.tools.core;

import org.json.JSONArray;
import org.json.JSONObject;

public class FlipFlopRealizerWebHandler {
    public static void main(String[] args){
        String jsonStr = args[0];
        String ffDataJson = args[1];

        JSONObject jsonObj = new JSONObject(jsonStr);
        JSONObject ffData = new JSONObject(ffDataJson);
        JSONObject outJSON = new JSONObject();

        String[] varNames = JSONInputParser.GetStrArrFromJSONOBJ(jsonObj, "VarNames");
        String stateVar = JSONInputParser.GetStrFromJSONOBJ(jsonObj, "StateVarName", "");
        String[] totalInVars = new String[varNames.length + 1];
        // String[] varNames = {"A","B","C"};
        // String stateVar = "Q";
        String[] controlLineNames = JSONInputParser.GetStrArrFromJSONOBJ(ffData, "VarNames");
        int controlLineCount = controlLineNames.length;
        
        totalInVars[0] = stateVar;
        for (int i = 1; i < varNames.length + 1; i++) {
            totalInVars[i] = varNames[i - 1];
        }
        
        String expression = JSONInputParser.GetStrFromJSONOBJ(jsonObj, "Expression", "");
        // String expression = "QAB + BC'";

        //For Testing Default Assignment SR FlipFlop
        FFRuleSet ffRuleSet = new FFRuleSet();
        for (int i = 1; i <= controlLineCount; i++) {
            char[][] grid = new char[2][2];
            JSONObject currVar = ffData.getJSONObject("Var" + i);

            for (int current = 0; current <= 1; current++) {
                for (int next = 0; next <= 1; next++) {
                    grid[current][next] = JSONInputParser.GetStrFromJSONOBJ(currVar, Integer.toString(current) + Integer.toString(next), "").charAt(0);
                }
            }
            ControlLineRuleSet controlLineRuleSet = new ControlLineRuleSet(controlLineNames[i -1], grid);
            ffRuleSet.AddControlLine(controlLineRuleSet);
        }

        // String[] controlLineNames = {"S","R"};
        // ControlLineRuleSet controlLineRuleSet1 = new ControlLineRuleSet("S", gridS);
        // ControlLineRuleSet controlLineRuleSet2 = new ControlLineRuleSet("R", gridR);
        // ffRuleSet.AddControlLine(controlLineRuleSet1);
        // ffRuleSet.AddControlLine(controlLineRuleSet2);
        PLATable table;
        try {
            table = FlipFlopRealizer.GetTableFromExpression(expression, varNames, stateVar);
        }
        catch(Exception e){
            outJSON.put("Problem",  "An error occured while trying to generate the PLATable");
            System.out.println(outJSON.toString());
            return;
        }

        // System.out.println(table.toString());
        FlipFlopRealizer ffRealizer = new FlipFlopRealizer(ffRuleSet, table);
        for (int i = 0; i < ffRuleSet.numberOfControlLines; i++) {
            try  {
                SolutionData solData = ffRealizer.SolveControlLineAt(i);
                String solExpression;
                if (solData == null) { // no solution .. trivial FSM
                    solExpression = "0";
                }
                else {
                    solExpression = QuineMckluskySolver.GetExpression(solData.optimizationResult.terms, totalInVars);
                }

                outJSON.put(controlLineNames[i], solExpression);
            }
            catch (Exception e) {
                outJSON.put("Problem",  "An error occured while trying to find solution");
                System.out.println(outJSON.toString());
                return;
            }


        }
        System.out.println(outJSON.toString());
    }
}
