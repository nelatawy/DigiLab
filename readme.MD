DigiLab
=======

## About
This Project offers a collection of tools that intend to ease the process of solving or dealing with certain problems in both  ***combinational/sequential logic design.***

## Installation
## First Method
1. ### Run the setup shell script using Git Bash  (PowerShell doesn't support it) or your native terminal if on a UNIX machine
    If it didn't work then try to append to the script file the executable flag ..(From Your Bash Terminal type this at the root directory of the repo)
    ```bash
    chmod +x setup.sh
    ```

## Second Method (Manual -- If you don't have the Bash terminal)

1. ### Install Flask MicroFrameWork for Python

    ```python
    pip3 install flask
    ```
2. ### Install MikTex for LaTex Support to Generate PDFs
    - ### Windows

        ```powershell
        winget install --id=MiKTeX.MiKTeX --source=winget
        ```
    
    - ### Linux

        ```bash
        sudo apt update
        sudo apt install -y miktex
        ```
        Then finish the setup
        ```bash
        miktexsetup finish
        initexmf --admin --set-config-value [MPM]AutoInstall=1
        ```
   
   - ### MacOS
        ```bash
        brew install  --cask miktex
        ```
        Then finish the setup
        ```bash
        miktexsetup finish
        initexmf --admin --set-config-value [MPM]AutoInstall=1
        ```


## Tools Included

> ### _Quine Mcklusky Solver_
 Helps you to get the optimal expression of a function given it's defining  min-terms and dont-cares

> ### _Link Paths Solver_
 Helps you solve a sequential system (Finite State Machines) in an SM chart way given it's defining link paths from one state to another, and the Moore Outputs linked to every state
 > ### _Flip-Flop Realizer_
 Helps you realize a boolean expression given the state-variable name and input-variable names and the flip-flop type.
 > ### _Flip-Flop Config Adder_
 Considered a helping tool to the FFRealizer and it's main purpose is to help you add a custom Flip-Flop configuration for you to use later in Realization.