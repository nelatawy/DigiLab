#!/bin/bash

pip3 install flask
winget install --id=MiKTeX.MiKTeX --source=winge#!/bin/bash

# Install Flask
pip3 install flask

# OS Detection
OS=$(uname)

if [[ "$OS" == "Linux" ]]; then
    echo "Detected Linux. Installing MiKTeX..."
    sudo apt update
    sudo apt install -y miktex
elif [[ "$OS" == "Darwin" ]]; then
    echo "Detected macOS. Installing MiKTeX..."
    brew install --cask miktex
elif [[ "$OS" =~ MINGW|MSYS|CYGWIN ]]; then
    echo "Detected Windows. Installing MiKTeX with winget..."
    winget install --id=MiKTeX.MiKTeX --source=winget
else
    echo "Unsupported OS: $OS"
    exit 1
fi

