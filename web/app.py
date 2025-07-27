from flask import Flask,request,render_template,send_file
import subprocess
import json
import uuid
import os
import logging



import sys
sys.stdout.reconfigure(line_buffering=True)



curr_dir = os.path.dirname(os.path.abspath(__file__))
# print(curr_dir)
# Build the absolute path to your JAR file
# core_logic_dir = os.path.join(curr_dir, "..", "maven", "demo", "target", "classes")
core_logic_dir = os.path.join(curr_dir, "..", "maven", "demo", "target","solver-tools.jar")
webpath = os.path.abspath(os.path.join(curr_dir, "..","web"))
ffconfigpath = os.path.abspath(os.path.join(webpath, "FFConfigs"))
classpath = os.path.abspath(core_logic_dir)
PDF_DIR_NAME = "generated_pdfs"
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
    # pi_chart = []
    # optimized_expr = ""
    # optimized_terms = ""
    # min_terms = []
    # covering_terms = []
    # itr_data = []
    # problem = ""
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

        # optimized_expr = result_json["OutputData"]["OptimizedVarOutput"]
        # optimized_terms = result_json["OutputData"]["OptimizedBoolOutput"]
        
        # pi_chart = result_json["PIChartData"]["PIChart"]
        # covering_terms = result_json["PIChartData"]["CoveringTerms"]
        # min_terms = result_json["PIChartData"]["MinTerms"]
        
        # itr_data = JSONtoNestedItrData(result_json)
        solutionData = {
            "OptimizedVarOutput" : result_json["OutputData"]["OptimizedVarOutput"],
            "OptimizedBoolOutput": result_json["OutputData"]["OptimizedBoolOutput"],
            "PIChart"            : result_json["PIChartData"]["PIChart"],
            "CoveringTerms"      : result_json["PIChartData"]["CoveringTerms"],
            "MinTerms"           : result_json["PIChartData"]["MinTerms"],
            "ItrData"            : JSONtoNestedItrData(result_json)
        }

        print(process.stdout)
        return render_template(
            "QuineMcklusky.html",
            solutionPhase = True,
            # optimized_expr=optimized_expr,
            # optimized_terms = optimized_terms, 
            # pi_chart = pi_chart,
            # min_terms = min_terms,
            # covering_terms = covering_terms,
            # itr_data = itr_data,
            solutionData = solutionData,
            enumerate = enumerate,
            zip = zip,
            len = len)


def generatePDF(laTexCode, fileName, folderName):
    # Create full folder path
    output_dir = os.path.join(webpath, folderName)
    os.makedirs(output_dir, exist_ok=True)

    tex_path = os.path.join(output_dir, f"{fileName}.tex")

    # Write LaTeX code to file
    with open(tex_path, "w") as file:
        file.write(laTexCode)

    try:
        subprocess.run(
            ["pdflatex", "-interaction=nonstopmode", f"{fileName}.tex"],
            cwd=os.path.abspath(output_dir),
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            check=True
        )
        return os.path.join(output_dir, f"{fileName}.pdf")
    except subprocess.CalledProcessError:
        return -1


@app.route('/LinkPathsSolver',methods = ['GET','POST'])
def LinkPathSolver():
    print("DEBUG:", "lol", flush=True)
    problem = ""
    if request.method == "GET" :
        return render_template("LinkPathsSolver.html",solutionPhase = False)
    
    if request.method == 'POST':
        stateVarCount = int(request.form["StateVarCount"])
        stateCount    = 2**stateVarCount
        # stateVarNames = ["Q"+ str(i) for i in range(stateVarCount - 1,-1,-1)]
        stateVarNames = ["Q"+ str(i) for i in range(stateVarCount - 1,-1,-1)]
        inVarCount    = int(request.form["InVarCount"])
        outVarCount   = int(request.form["OutVarCount"])
        inVarNames    = [item.strip() for item in request.form["InVariables"].split(',') if item.strip()] 
        outVarNames   = [item.strip() for item in request.form["OutVariables"].split(',') if item.strip()] 
        mooreOutputs  = []
        for i in range(stateCount):
            mooreOutputs.append(request.form["S" + str(i) + "_MooreOutput"])
        linkCount = request.form["LinkCount"]
        linkPaths = []
        for i in range(int(linkCount)):
            currLink = {
                "fromState": request.form["Link" + str(i) + "_fromState"],
                "inStr"    : request.form["Link" + str(i) + "_inStr"],
                "outStr"   : request.form["Link" + str(i) + "_outStr"],
                "toState"  : request.form["Link" + str(i) + "_toState"]
            }

        inVarString = ""
        for name in inVarNames :
            inVarString += name + ","
        inVarString = inVarString.removesuffix(",")
        print (inVarString)

        linkPaths.append(currLink)
        logging.basicConfig(level=logging.DEBUG)
        logging.debug(request.form)
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
        logging.debug(arguments_json)
        cmd = ["java", "-cp", classpath,"com.github.digilab.tools.core.LinkPathsSolverWebHandler"]
        process = subprocess.run(cmd + [arguments_json], capture_output=True, text=True)
        try:
            result_json = json.loads(process.stdout)
        except json.JSONDecodeError as e:
            print("JSON decode error:", e)
            print("Raw err:", process.stderr)
            print("Raw output:", process.stdout)
            optimized_expr = "Java did not return valid JSON. See server logs."
            return render_template("LinkPathsSolver.html", problem = "Java did not return valid JSON. See server logs.")
        
        if("Problem" in result_json):
            problem = result_json["Problem"]
            return render_template(
            "LinkPathsSolver.html",
            solutionPhase = True,
            problem = problem)
        
        laTexCode = result_json["LaTexStateGraph"]
        stateVarOutputs = result_json["StateVarOutputs"]
        nullStateVars = result_json["NullStateVars"]
        outVarOutputs = result_json["OutVarOutputs"]
        nullOutVars = result_json["NullOutVars"]

        graph = generatePDF(laTexCode,"stateGraph",PDF_DIR_NAME)
        if  graph == -1:
            problem = "Couldn't Generate the StateGraph.."
            return render_template(
            "LinkPathsSolver.html",
            solutionPhase = True,
            problem = problem)
        else :
            print("DEBUG:", "lol", flush=True)

        # totalOutVars = stateVarNames + outVarNames.split(',')
        stateVarSolutions = []
        outVarSolutions = []

        print(nullOutVars)
        print(nullStateVars)
        print(process.stdout)
        for var in stateVarNames :
            if var in nullStateVars :
                continue
            var_json = stateVarOutputs[var]
            currVar = {
                "Name"               : var,
                "ItrData"            :JSONtoNestedItrData(var_json),
                "OptimizedVarOutput" :var_json["OutputData"]["OptimizedVarOutput"],
                "OptimizedBoolOutput":var_json["OutputData"]["OptimizedBoolOutput"],
                "PIChart"            :var_json["PIChartData"]["PIChart"],
                "CoveringTerms"      :var_json["PIChartData"]["CoveringTerms"],
                "Terms"              :var_json["PIChartData"]["MinTerms"]
            }
            stateVarSolutions.append(currVar)

        for var in outVarNames :
            if var in nullOutVars :
                continue
            var_json = outVarOutputs[var]
            currVar = {
                "Name"               : var,
                "ItrData"            :JSONtoNestedItrData(var_json),
                "OptimizedVarOutput" :var_json["OutputData"]["OptimizedVarOutput"],
                "OptimizedBoolOutput":var_json["OutputData"]["OptimizedBoolOutput"],
                "PIChart"            :var_json["PIChartData"]["PIChart"],
                "CoveringTerms"      :var_json["PIChartData"]["CoveringTerms"],
                "Terms"              :var_json["PIChartData"]["MinTerms"]
            }
            outVarSolutions.append(currVar)
            logging.debug(process.stdout)

        
        print("DEBUG:", outVarSolutions, flush=True)

        
        return render_template(
            "LinkPathsSolver.html",
            solutionPhase = True,
            stateVarSolutions = stateVarSolutions,
            nullStateVars = nullStateVars,
            outVarSolutions = outVarSolutions,
            inVarNames = inVarString,
            nullOutVars = nullOutVars,
            pdfName = "stateGraph",
            folderName = PDF_DIR_NAME,
            enumerate = enumerate,
            raw = process.stdout,
            zip = zip,
            len = len)

@app.route('/LinkPathsSolverPDF/<pdf_name>/<folder_name>')
def servePDF(pdf_name,folder_name):
    pdf_path = os.path.join(webpath,folder_name, f"{pdf_name}.pdf")
    if not os.path.exists(pdf_path):
        return "Couldn't find the file with the specified name"
    else:
        return send_file(pdf_path, mimetype="application/pdf")


@app.route('/FFConfigAdder', methods = ['GET','POST'])
def FFConfigAdder():
    if request.method == 'GET' :
        return render_template("FFConfigAdder.html")
    elif request.method == 'POST' :
        try:
            ffName = request.form["flipflpName"]
            varCount = request.form["controlLineCount"]
            varNames = [item.strip() for item in request.form["VarNames"].split(',')]
            if len(varNames) != int(varCount) :
                raise RuntimeError("Something went wrong")
            data_dict = {}
            for index in range(1,int(varCount) + 1):
                data_dict["Var" + str(index)] = {}
            for currentS in [0,1] :
                for nextS in [0,1] :
                    for index in range(1,int(varCount) + 1):
                        data_dict["Var" + str(index)][str(currentS) + str(nextS)] = request.form["Var" + str(index) +"_Current" + str(currentS) + "_Next" + str(nextS)]
            data_dict["VarNames"] = varNames
            data_json = json.dumps(data_dict, indent=4)
            os.makedirs(ffconfigpath, exist_ok=True)
            
            write_path = os.path.join(ffconfigpath, ffName + ".json")
            with open(write_path, "w" if os.path.exists(write_path) else "x") as file :
                file.write(data_json)

            return render_template("FFConfigAdder.html", submit = True)
        except :
            return render_template("FFConfigAdder.html", submit = True, problem = "Invalid data .. please check that the number of control lines matches the number of names provided \n and that the table data is valid")
    
# @app.route('/FFRealizer', methods = ['GET','POST'])
# def FFRealizer():
#     # Getting the possibleConfigs
#     flipflopNames = [f for f in os.listdir(ffconfigpath)]

#     if request.method == 'GET' :
#         return render_template("FFRealizer.html", solutionPhase = False, configs = flipflopNames)
#     elif request.method == 'POST' :

#         varNames = [item.strip() for item in request.form["VarNames"].split(',')]
#         stateVarName = request.form["StateVarName"]
#         expression = request.form["Expression"]
#         ffName = request.form.get("FFName")
#         with open(os.path.abspath(os.path.join( ffconfigpath , ffName) ), "r") as file:
#             ff_data_json = file.read()
        
#         data = {
#             "VarNames"     : varNames,
#             "StateVarName" : stateVarName,
#             "Expression"   : expression
#         }
#         arguments_json = json.dumps(data)
#         arguments = [arguments_json, ff_data_json]

#         cmd = ["java", "-cp", classpath,"com.github.digilab.tools.core.FlipFlopRealizerWebHandler"]
#         process = subprocess.run(cmd + arguments, capture_output= True, text= True)
#         print(process.stdout)
#         try:
#             result_json = json.loads(process.stdout)
#         except json.JSONDecodeError as e:
#             print("JSON decode error:", e)
#             print("Raw err:", process.stderr)
#             print("Raw output:", process.stdout)
#             return render_template("FFRealizer.html", problem = "Java did not return valid JSON. See server logs.")
#         if "Problem" in result_json :
#             return render_template("FFRealizer.html",
#                                     solutionPhase = True,
#                                     problem = result_json["Problem"],
#                                     configs = flipflopNames)
#         # no need to custom handle it .. it's already a dict-like structure which works properly for this
#         solutions = dict(result_json)
#         return render_template("FFRealizer.html",
#                                 solutionPhase = True,
#                                 varSolutions = solutions,
#                                 len = len ,
#                                 configs = flipflopNames)
    
@app.route('/FFRealizer', methods = ['GET' , 'POST'])
def FFRealizer():
    # Getting the possibleConfigs
    flipflopNames = [f for f in os.listdir(ffconfigpath)]
    if request.method == 'GET' :
        stateVar = ""
        varNames = []
        expression = ""
        forwarded = False
        if(request.args.__contains__("forwarded") and request.args.get("forwarded").lower() == 'true' ):
            stateVar = request.args.get("state_var")
            varNames = request.args.get("var_names")
            expression = request.args.get("expression")
            forwarded = True
        # Providing values to handle the redirecting process and giving back the URL args
        return render_template("FFRealizer.html",
                                forwarded = forwarded ,
                                stateVar = stateVar,
                                varNames = varNames,
                                expression = expression, 
                                solutionPhase = False, 
                                configs = flipflopNames)
        
    elif request.method == 'POST' :

        forwarded = (request.args.get("forwarded").lower() == "true")
        if forwarded :
            print(request.args.get("var_names"))
        varNames = [name.strip() for name in request.args.get("var_names").split(",")] if forwarded else [item.strip() for item in request.form["VarNames"].split(',')]
        stateVarName = request.args.get("state_var") if forwarded else request.form["StateVarName"]
        expression = request.args.get("expression") if forwarded else request.form["Expression"]
        print(expression)
        print(varNames)
        ffName = request.form.get("FFName")
        with open(os.path.abspath(os.path.join( ffconfigpath , ffName) ), "r") as file:
            ff_data_json = file.read()
        
        data = {
            "VarNames"     : varNames,
            "StateVarName" : stateVarName,
            "Expression"   : expression
        }
        arguments_json = json.dumps(data)
        arguments = [arguments_json, ff_data_json]

        cmd = ["java", "-cp", classpath,"com.github.digilab.tools.core.FlipFlopRealizerWebHandler"]
        process = subprocess.run(cmd + arguments, capture_output= True, text= True)
        print(process.stdout)
        try:
            result_json = json.loads(process.stdout)
        except json.JSONDecodeError as e:
            print("JSON decode error:", e)
            print("Raw err:", process.stderr)
            print("Raw output:", process.stdout)
            return render_template("FFRealizer.html", problem = "Java did not return valid JSON. See server logs.")
        if "Problem" in result_json :
            return render_template("FFRealizer.html",
                                    solutionPhase = True,
                                    forwarded = forwarded,
                                    problem = result_json["Problem"],
                                    len = len,
                                    configs = flipflopNames)
        # no need to custom handle it .. it's already a dict-like structure which works properly for this
        solutions = dict(result_json)
        return render_template("FFRealizer.html",
                                solutionPhase = True,
                                forwarded = forwarded,
                                varSolutions = solutions,
                                len = len ,
                                configs = flipflopNames)
    

app.run(debug=True, port=8080)
