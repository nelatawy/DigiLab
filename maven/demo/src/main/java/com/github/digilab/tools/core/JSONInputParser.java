package com.github.digilab.tools.core;

import org.json.JSONArray;
import org.json.JSONObject;

public class JSONInputParser {
    
    public static int GetIntFromJSONOBJ(JSONObject jsonObj, String KeyForVar , int fallbackVal){
        if (!jsonObj.has(KeyForVar)) {
            // System.err.println("Invalid Key to obtain elements");
            return fallbackVal;
        }
        Object jsonValue = jsonObj.get(KeyForVar);
        if (jsonValue instanceof String) {
            try {
                int intVal = Integer.parseInt((String)jsonValue);
                return intVal;
            } catch (Exception e) {
                // System.err.println("Mismatching Value Type At Key");
                return fallbackVal;
            }
        }
        // System.err.println("Mismatching Value Type At Key");
        return fallbackVal;
    }
    public static String GetStrFromJSONOBJ(JSONObject jsonObj, String KeyForVar , String fallbackVal){
        if (!jsonObj.has(KeyForVar)) {
            // System.err.println("Invalid Key to obtain elements");
            return fallbackVal;
        }
        String output;
        try {
            output = (String)jsonObj.get(KeyForVar);
        } catch (Exception e) {
            output = fallbackVal;
        }
        return output;
    }
    
    public static String[] GetStrArrFromJSONOBJ(JSONObject jsonObj, String KeyForVar){
        if (!jsonObj.has(KeyForVar)) {
            // System.err.println("Invalid Key to obtain elements");
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
        // System.err.println("Mismatching Value Type At " + KeyForVar);
        return null;
    }
    public static int[] GetIntArrFromJSONOBJ(JSONObject jsonObj, String KeyForVar){
        if (!jsonObj.has(KeyForVar)) {
            // System.err.println("Invalid Key to obtain MinTerms");
            return null;
        }
        Object val = jsonObj.get(KeyForVar);
        if (val instanceof JSONArray) {
            JSONArray jsonArr = (JSONArray)val;
            int[] intArr = new int[jsonArr.length()];
            for(int i = 0; i < jsonArr.length(); i++){
                try{
                    intArr[i] = Integer.parseInt(jsonArr.getString(i));
                } catch (Exception e){
                    return null;
                }       
            }
            return intArr;
        }
        if (val instanceof String) {
            if ((String)val == "") return null;
            int[] intArr = new int[1];
            try{
                intArr[0] = Integer.parseInt((String)val);
            } catch (Exception e){
                return null;
            }
            return intArr;
        }
        // System.err.println("Mismatching Value Type At " + KeyForVar);
        return null;
    }
    
}
