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
    problem = ""
    if request.method == "GET" :
        return render_template("QuineMcklusky.html",solutionPhase = False)
    
    if request.method == "POST":
        numberOfVariables = request.form["VarCount"]
        variables_input   = request.form["Variables"]
        minTerms_input    = request.form["MinTerms"]
        dontCares_input   = request.form["DontCares"]

        minTerms = [item.strip() for item in minTerms_input.split(',') if item.strip()]
        dontCares = [item.strip() for item in dontCares_input.split(',') if item.strip()]
        variables = [item.strip() for item in variables_input.split(',') if item.strip()]


        data = {
            "NumberOfVars" : numberOfVariables,
            "MinTerms"     : minTerms,
            "DontCares"    : dontCares,
            "Variables"    : variables
        }

        arguments_json = json.dumps(data)

        cmd = ["java", "-cp", classpath,"com.github.digilab.tools.core.QuineMckluskyWebHandler"]
        process = subprocess.run(cmd + [arguments_json], capture_output=True, text=True)
        
        print("Java output:", process.stdout)
        try:
            result_json = json.loads(process.stdout)
        except json.JSONDecodeError as e:
            print("JSON decode error:", e)
            print("Raw output:", process.stdout)
            optimized_expr = "Java did not return valid JSON. See server logs."
            return render_template("QuineMcklusky.html", problem = problem, output=optimized_expr)

        if("Problem" in result_json):
            problem = result_json["Problem"]
            return render_template(
            "QuineMcklusky.html",
            solutionPhase = True,
            problem = problem)

        optimized_expr = result_json["OutputData"]["OptimizedVarOutput"]
        optimized_terms = result_json["OutputData"]["OptimizedBoolOutput"]
        
        pi_chart = result_json["PIChartData"]["PIChart"]
        covering_terms = result_json["PIChartData"]["CoveringTerms"]
        min_terms = result_json["PIChartData"]["MinTerms"]
        
        itr_data = JSONtoNestedItrData(result_json)
        print(process.stdout)
        return render_template(
            "QuineMcklusky.html",
            solutionPhase = True,
            optimized_expr=optimized_expr,
            optimized_terms = optimized_terms, 
            pi_chart = pi_chart,
            min_terms = min_terms,
            covering_terms = covering_terms,
            itr_data = itr_data,
            enumerate = enumerate,
            zip = zip)


@app.route('/LinkPathsSolver',methods = ['GET','POST'])
def LinkPathSolver():
    if request.method == 'POST':
        stateVarCount = int(request.form["StateVarCount"])
        inVarCount    = int(request.form["InVarCount"])
        outVarCount   = int(request.form["OutVarCount"])
        inVarNames    = request.form["InVariables"]
        outVarNames   = request.form["OutVariables"]
        mooreOutputs  = []
        for i in range(stateVarCount):
            mooreOutputs.append(request.form["S" + str(i) + "_MooreOutput"])
        linkCount = request.form["LinkCount"]
        linkPaths = []
        for i in range(linkCount):
            currLink = {
                "fromState": request.form["Link" + str(i) + "_fromState"],
                "inStr"    : request.form["Link" + str(i) + "_inStr"],
                "outStr"   : request.form["Link" + str(i) + "_outStr"],
                "toState"  : request.form["Link" + str(i) + "_toState"]
            }
            linkPaths.append(currLink)
        
        data = {
            "stateVarCount" : stateVarCount,
            "inVarCount"    : inVarCount,
            "outVarCount"   : outVarCount,
            "inVarNames"    : inVarNames,
            "outVarNames"   : outVarNames,
            "mooreOutputs"  : mooreOutputs,
            "linkPathsCount": linkCount,
            "linkPaths"     : linkPaths
        }
        arguments_json = json.dumps(data)
        cmd = ["java", "-cp", classpath,"com.github.digilab.tools.core.LinkPathsSolverWebHandler"]
        process = subprocess.run(cmd + [arguments_json], capture_output=True, text=True)
        try:
            result_json = json.loads(process.stdout)
        except json.JSONDecodeError as e:
            print("JSON decode error:", e)
            print("Raw output:", process.stdout)
            optimized_expr = "Java did not return valid JSON. See server logs."
            return render_template("LinkPathsSolver.html", problem = problem)
        
        if("Problem" in result_json):
            problem = result_json["Problem"]
            return render_template(
            "LinkPathsSolver.html",
            solutionPhase = True,
            problem = problem)

    return render_template("LinkPathsSolver.html")

app.run(port=8080,debug=True)