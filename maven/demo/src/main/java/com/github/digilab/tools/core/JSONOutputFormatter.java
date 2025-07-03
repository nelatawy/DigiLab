package com.github.digilab.tools.core;
import org.json.*;

public class JSONOutputFormatter{
        
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

    public static JSONObject PIChartDataToResultJSON(SimplificationData simplificationData, OptimizationResult optimizationRes , int[] terms){
        JSONObject result = new JSONObject();
        result.put("PIChart", optimizationRes.piChart);
        result.put("CoveringTerms", QuineMckluskySolver.termListToStrArr(simplificationData.primeImplicants));
        result.put("MinTerms", terms);
        return result;
    }

    public static JSONObject PIChartDataToResultJSON(SimplificationData simplificationData, OptimizationResult optimizationRes , String[] terms){
        JSONObject result = new JSONObject();
        result.put("PIChart", optimizationRes.piChart);
        result.put("CoveringTerms", QuineMckluskySolver.termListToStrArr(simplificationData.primeImplicants));
        result.put("MinTerms", terms);
        return result;
    }
    
    public static JSONObject OutputDataToResultJSON(OptimizationResult optimizationRes, String[] variables){
        JSONObject result = new JSONObject();
        result.put("OptimizedBoolOutput", QuineMckluskySolver.termListToStrArr(optimizationRes.terms));

        String optimizedVarOutput = QuineMckluskySolver.GetExpression(optimizationRes.terms, variables);
        result.put("OptimizedVarOutput", optimizedVarOutput);
        return result;
    }
    
}