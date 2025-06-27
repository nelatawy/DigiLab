package com.github.digilab.tools.core;
import java.util.LinkedList;

class Coordinate{
    double x;
    double y;
    public Coordinate(double x, double y){
        this.x = x;
        this.y = y;
    }
}
class Link {
    State fromState,toState;
    String inputStr;
    String outputStr; 
    //written in string format because since we'll assign certain inputs and outputs to indices 
    //then a string allows random access on O(1) and optimal memory management
    public Link(State fromState, State toState , String input, String output){
        this.fromState = fromState;
        this.toState   = toState;
        this.inputStr  = input;
        this.outputStr = output;
    }
}
class State {
    LinkedList<Link> links;
    int stateID;
    String stateOutput;
    public State(int ID , String stateOutput){
        this.stateID = ID;
        this.stateOutput = stateOutput;
        this.links = new LinkedList<Link>();
    }
    public void AddLinkTo(State target, String input, String output){
        Link newLink = new Link(this, target, input, output);
        links.push(newLink);
    }
}
public class StateGraph {

    LinkedList<State> states;
    String[] inVars,outVars; //excluding state vars
    int numberOfStates;
    public StateGraph(String[] inVars, String[] outVars){
        this.inVars  = inVars;
        this.outVars = outVars;

        this.numberOfStates = 0;
        this.states = new LinkedList<State>();
    }
    
    public void AddState(State state){
        if (!states.contains(state)) {
            states.push(state);
            numberOfStates++;
        } else {
            System.out.println("Graph already has this state..State wasn't added");
        }
    }
    
    public PLATable ToPLATable(){
        int stateVarCount = (int) Math.ceil(Math.log(numberOfStates) / Math.log(2));
        PLATable table = new PLATable(inVars,outVars,stateVarCount);
        
        for (State state : states) {
            for (Link link : state.links) {
                String currentState = "S" + link.fromState.stateID;
                String nextState    = "S" + link.toState.stateID;
                String inputStr     = link.inputStr;
                String outputStr    = link.outputStr;
                
                RelationRow newRow  = new RelationRow(currentState, nextState, inputStr, outputStr);
                table.AddRow(newRow);
            }
        }
        return table;
    }

    private Coordinate[] GetCircularPositionsOfStates(double radius){
        float angleBetStates = (float)(2*Math.PI)/this.numberOfStates; // angle in Radians
        Coordinate[] positions = new Coordinate[numberOfStates];
        int currIndex = 0;
        Coordinate initialPos = new Coordinate(radius, 0);
        for (State state : this.states) {
            double x = initialPos.x * Math.cos(angleBetStates * currIndex) - initialPos.y * Math.sin(angleBetStates * currIndex);
            double y = initialPos.x * Math.sin(angleBetStates * currIndex) + initialPos.y * Math.cos(angleBetStates * currIndex);
                        positions[state.stateID] = new Coordinate(x, y);
            currIndex++;
        }
        return positions;
    }
    public String ToLaTexCircularGraph(double radius){
        String headerData = "\\documentclass{article}\r\n" + //
                            "\\usepackage{tikz}\r\n" + //
                            "\\usetikzlibrary{automata, positioning}\n";
        String body = "";

        String stateDeclaration = "";
        String pathDeclaration = ""; //since this block is all under \path unlike with node
        Coordinate[] stateLocations = this.GetCircularPositionsOfStates(radius);
        
        for (State state : states) {
            double statePosX = stateLocations[state.stateID].x;
            double statePosY = stateLocations[state.stateID].y;

            // Format to 2 decimal places
            String xStr = String.format("%.3f", statePosX);
            String yStr = String.format("%.3f", statePosY);

            stateDeclaration += "\\node[state]" + "\t(S" + state.stateID + ")" + "\t at (" + xStr  + "," + yStr + ")" + "\t{$" + "S_" + state.stateID + "$/" + state.stateOutput + "};\n";
            for (Link link : state.links) {
                pathDeclaration +=  "\\path[->] " +"(S" + state.stateID + ")" + "\t edge node [pos=0.2]" + "{" + link.inputStr + "/" + link.outputStr + "}" + "\t(S" + link.toState.stateID + ");\n";
            }
        }
        body += "\\begin{document}\r\n" + //
                "\r\n" + //
                "\\begin{tikzpicture}[shorten >=1pt, on grid, auto]";
        body +=  "\n" + stateDeclaration + "\n" + pathDeclaration + "\n";
        body += "\\end{tikzpicture}\r\n" + //
                "\r\n" + //
                "\\end{document}";
        return headerData + body;
    }
    
    public static void main(String[]args){
        String[] inVars = {"A","B","C"};
        String[] outVars = {"D","E","F"};
        StateGraph sg = new StateGraph(inVars, outVars);

        State state0 = new State(0, "100");
        State state1 = new State(1, "110");
        State state2 = new State(2, "000");
        State state3 = new State(3, "010");
        State state4 = new State(4, "100");
        State state5 = new State(5, "011");
        state0.AddLinkTo(state1, "100", "101");
        state1.AddLinkTo(state2, "101", "111");
        state2.AddLinkTo(state0, "110", "010");
        state3.AddLinkTo(state1, "100", "101");
        state4.AddLinkTo(state2, "101", "111");
        state5.AddLinkTo(state0, "110", "010");
        sg.AddState(state0);
        sg.AddState(state1);
        sg.AddState(state2);
        sg.AddState(state3);
        sg.AddState(state4);
        sg.AddState(state5);

        System.out.println(sg.ToLaTexCircularGraph(5));
    }
}
