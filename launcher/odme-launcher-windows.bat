@echo off

rem Function to check if Java is installed
:check_java
java -version >nul 2>&1
if %errorlevel% == 0 (
    echo Java is already installed on this PC.
    java -version
) else (
    echo Java is not installed on this PC.

    :: Specify the URL to download the Java installer
    set "installerURL=https://javadl.oracle.com/webapps/download/AutoDL?BundleId=248737_8c876547113c4e4aab3c868e9e0ec572"
    
    :: Specify the location to save the downloaded installer
    set "installerPath=C:\Users\adeut\Downloads\JavaSetup8u381.exe" ::please modify the installerPath with the correct downloding folder path of your computer for exemple by modify adeut by you correct pc username and you got it in C:\Users
    
    echo Downloading Java installer...
    curl -o "%installerPath%" "%installerURL%" 2>nul
    
    :: Check if the download was successful
    if not exist "%installerPath%" (
        echo Failed to download Java installer from %installerURL%.
    ) else (
        echo Installing Java...
        "%installerPath%" /s  
        
        :: Check if installation was successful
        java -version >nul 2>&1
        if %errorlevel% == 0 (
            echo Java has been installed successfully.
            java -version
        ) else (
            echo Failed to install Java.
            exit /b 1
        )
    )
)

rem Run the JAR file
java -jar SESEditor-1.0-SNAPSHOT-jar-with-dependencies.jar

:end

