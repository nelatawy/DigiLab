package com.github.digilab.tools.core;
import java.util.LinkedList;

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
    public State(int ID){
        stateID = ID;
        links = new LinkedList<Link>();
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

}
