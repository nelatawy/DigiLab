import java.util.Hashtable;

import org.json.*;

public class QuineMckluskyWebHandler {

    public static String[] GetVariableListFromJSONOBJ(JSONObject jsonObj, String KeyForVariables){
        if (!jsonObj.has(KeyForVariables)) {
            System.err.println("Invalid Key to obtain variables");
            return null;
        }
        Object variableList = jsonObj.get(KeyForVariables);
        if (variableList instanceof JSONArray) {
            JSONArray jsonArr = (JSONArray)variableList;
            String[] varStrArr = new String[jsonArr.length()];
            for(int i = 0; i < jsonArr.length(); i++){
                varStrArr[i] = jsonArr.getString(i);
            }
            return varStrArr;
        }
        if (variableList instanceof String) {
            String[] varStrArr = new String[1];
            varStrArr[0] = (String)variableList;
            
            return varStrArr;
        }
        System.err.println("Mismatching Value Type At KeyForVariables");
        return null;
    }
    public static int GetNumberOfVariablesFromJSONOBJ(JSONObject jsonObj, String KeyForNumberOfVars){
        Object value = jsonObj.get(KeyForNumberOfVars);
        if (value instanceof String) {
            try {
                int numberOfVariables = Integer.parseInt((String)value);
                return numberOfVariables;
            } catch (Exception e) {
                System.err.println("Mismatching Value Type At KeyForNumberOfVars");
                return -1;
            }
        }
        System.err.println("Mismatching Value Type At KeyForNumberOfVars");
        return -1;
    }
    public static String[] GetMinTermsFromJSONOBJ(JSONObject jsonObj, String KeyForMinTerms){
        if (!jsonObj.has(KeyForMinTerms)) {
            System.err.println("Invalid Key to obtain MinTerms");
            return null;
        }
        Object variableList = jsonObj.get(KeyForMinTerms);
        if (variableList instanceof JSONArray) {
            JSONArray jsonArr = (JSONArray)variableList;
            String[] minTermsStrArr = new String[jsonArr.length()];
            for(int i = 0; i < jsonArr.length(); i++){
                minTermsStrArr[i] = jsonArr.getString(i);
            }
            return minTermsStrArr;
        }
        if (variableList instanceof String) {
            String[] minTermsStrArr = new String[1];
            minTermsStrArr[0] = (String)variableList;
            
            return minTermsStrArr;
        }
        System.err.println("Mismatching Value Type At KeyForMinTerms");
        return null;
    }
}