@echo off
:: Batch script to start Sobzy Backend with Administrator privileges
:: This ensures Hotspot, ICS, and Traffic Capture work correctly.

>nul 2>&1 "%SYSTEMROOT%\system32\cacls.exe" "%SYSTEMROOT%\system32\config\system"

if '%errorlevel%' NEQ '0' (
    echo [!] Requesting Administrator privileges...
    goto UACPrompt
) else ( goto gotAdmin )

:UACPrompt
    echo Set UAC = CreateObject^("Shell.Application"^) > "%temp%\getadmin.vbs"
    set params = %*
    echo UAC.ShellExecute "cmd.exe", "/c %~s0 %params%", "", "runas", 1 >> "%temp%\getadmin.vbs"
    "%temp%\getadmin.vbs"
    del "%temp%\getadmin.vbs"
    exit /B

:gotAdmin
    pushd "%~dp0"
    echo [*] Starting SOBZY Backend...
    :: Use Maven to run if in dev environment, otherwise use the JAR
    if exist "mvnw.cmd" (
        call .\mvnw.cmd spring-boot:run
    ) else if exist "target\sobzy-backend-*.jar" (
        for /f "delims=" %%i in ('dir /b /s target\sobzy-backend-*.jar') do set JAR_FILE=%%i
        java -jar "%JAR_FILE%"
    ) else (
        echo [!] No JAR or mvnw found. Please build the project first.
        pause
    )
    popd
