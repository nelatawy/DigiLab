from flask import Flask,request,render_template
import subprocess
import json
import os




curr_dir = os.path.dirname(os.path.abspath(__file__))
# print(curr_dir)
# Build the absolute path to your JAR file
# core_logic_dir = os.path.join(curr_dir, "..", "maven", "demo", "target", "classes")
core_logic_dir = os.path.join(curr_dir, "..", "maven", "demo", "target","solver-tools.jar")
classpath = os.path.abspath(core_logic_dir)
# print(classpath)

app = Flask(__name__)
@app.route('/',methods = ['GET','POST'])
def index():
    return render_template("index.html")




def JSONtoNestedItrData(json_src):
    jsonObj = json_src["SimplificationData"]

    itrCount = jsonObj["ItrCount"]
    groupCount = jsonObj["GroupsPerIteration"]
    nested = []
    for itrIdx in range(itrCount):
        groups = []
        for gpIdx in range(groupCount):
            groups.append(jsonObj[f"Itr{itrIdx}"].get(f"Gp{gpIdx}", []))
        nested.append(groups)
    return nested


@app.route('/QuineMcklusky',methods = ['GET','POST'])
def QuineMcklusky():
    pi_chart = []
    optimized_expr = ""
    optimized_terms = ""
    min_terms = []
    covering_terms = []
    itr_data = []

    if request.method == "POST":
        numberOfVariables = request.form["VarCount"]
        variables_input   = request.form["Variables"]
        minTerms_input    = request.form["MinTerms"]
        dontCares_input   = request.form["DontCares"]

        minTerms  = [item.strip() for item in minTerms_input.split(',')]
        dontCares = [item.strip() for item in dontCares_input.split(',')]
        variables = [item.strip() for item in variables_input.split(',')]


        data = {
            "NumberOfVars" : numberOfVariables,
            "MinTerms"     : minTerms,
            "DontCares"    : dontCares,
            "Variables"    : variables
        }

        arguments_json = json.dumps(data)

        cmd = ["java", "-cp", classpath,"com.github.digilab.tools.core.QuineMckluskyWebHandler"]
        process = subprocess.run(cmd + [arguments_json], capture_output=True, text=True)
        
        try:
            result_json = json.loads(process.stdout)
        except json.JSONDecodeError as e:
            print("JSON decode error:", e)
            print("Raw output:", process.stdout)
            optimized_expr = "Java did not return valid JSON. See server logs."
            return render_template("QuineMcklusky.html", output=optimized_expr)

        optimized_expr = result_json["OutputData"]["OptimizedVarOutput"]
        optimized_terms = result_json["OutputData"]["OptimizedBoolOutput"]
        
        pi_chart = result_json["PIChartData"]["PIChart"]
        covering_terms = result_json["PIChartData"]["CoveringTerms"]
        min_terms = result_json["PIChartData"]["MinTerms"]
        
        itr_data = JSONtoNestedItrData(result_json)
        print(process.stdout)
    return render_template(
        "QuineMcklusky.html",
        optimized_expr=optimized_expr,
        optimized_terms = optimized_terms, 
        pi_chart = pi_chart,
        min_terms = min_terms,
        covering_terms = covering_terms,
        itr_data = itr_data,
        enumerate = enumerate,
        zip = zip)

app.run(port=8080,debug=True)