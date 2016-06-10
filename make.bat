@echo off
@REM ##########################################################################
@REM
@REM  make script for Windows
@REM
@REM ##########################################################################
if "%OS%"=="Windows_NT" setlocal ENABLEEXTENSIONS
set KEY_NAME_JAVA_DEV="HKLM\SOFTWARE\JavaSoft\Java Development Kit"
set KEY_NAME_JAVA_RUN="HKLM\SOFTWARE\JavaSoft\Java Runtime Environment"
set VALUE_NAME=CurrentVersion
set ASSEMBLED=assembled\
set JAVABIN=builder\bin\
set JAVASRC=builder\

for /f "tokens=4-5 delims=. " %%i in ('ver') do set VERSION=%%i.%%j
if %VERSION% == 6.1 (
	FOR /F "usebackq skip=2 tokens=3" %%A IN (`REG QUERY %KEY_NAME_JAVA_DEV% /v %VALUE_NAME% 2^>nul`) DO (
		set JAVA_DEV=%%A
	)
	FOR /F "usebackq skip=2 tokens=3" %%A IN (`REG QUERY %KEY_NAME_JAVA_RUN% /v %VALUE_NAME% 2^>nul`) DO (
		set JAVA_RUN=%%A
	)
) else if %VERSION% == 5.1 (
	FOR /F "usebackq skip=4 tokens=3" %%A IN (`REG QUERY %KEY_NAME_JAVA_DEV% /v %VALUE_NAME% 2^>nul`) DO (
		set JAVA_DEV=%%A
	)
	FOR /F "usebackq skip=4 tokens=3" %%A IN (`REG QUERY %KEY_NAME_JAVA_RUN% /v %VALUE_NAME% 2^>nul`) DO (
		set JAVA_RUN=%%A
	)
)

if not defined JAVA_DEV (
    @echo %KEY_NAME_JAVA_DEV%\%VALUE_NAME% not found.
	@echo.
	@echo No Java Development Kit found
	@echo :: FAIL
	@echo.
    goto mainEnd
)

if not defined JAVA_RUN (
    @echo %KEY_NAME_JAVA_RUN%\%VALUE_NAME% not found.
	@echo.
	@echo No Java Runtime Environment found
	@echo :: FAIL
	@echo.
    goto mainEnd
)

set JAVA_CURRENT_DEV="HKLM\SOFTWARE\JavaSoft\Java Development Kit\%JAVA_DEV%"
set JAVA_CURRENT_RUN="HKLM\SOFTWARE\JavaSoft\Java Runtime Environment\%JAVA_RUN%"
set JAVA_HOME_NAME=JavaHome

if %VERSION% == 6.1 (
	FOR /F "usebackq skip=2 tokens=3*" %%A IN (`REG QUERY %JAVA_CURRENT_DEV% /v %JAVA_HOME_NAME% 2^>nul`) DO (
		set JAVA_PATH_DEV=%%A %%B
	)
	FOR /F "usebackq skip=2 tokens=3*" %%A IN (`REG QUERY %JAVA_CURRENT_RUN% /v %JAVA_HOME_NAME% 2^>nul`) DO (
		set JAVA_PATH_RUN=%%A %%B
	)
) else if %VERSION% == 5.1 (
	FOR /F "usebackq skip=4 tokens=3,4" %%A IN (`REG QUERY %JAVA_CURRENT_DEV% /v %JAVA_HOME_NAME% 2^>nul`) DO (
		set JAVA_PATH_DEV=%%A %%B
	)
	FOR /F "usebackq skip=4 tokens=3,4" %%A IN (`REG QUERY %JAVA_CURRENT_RUN% /v %JAVA_HOME_NAME% 2^>nul`) DO (
		set JAVA_PATH_RUN=%%A %%B
	)
)

set JAVA_DEV_BIN="%JAVA_PATH_DEV%\bin\javac.exe"
set JAVA_RUN_BIN="%JAVA_PATH_RUN%\bin\java.exe"
set PrintInfo=1

if not exist %JAVA_DEV_BIN% (
	@echo %JAVA_DEV_BIN% not found.
	@echo.
	@echo No installed Java Development Kit found
	@echo :: FAIL
	@echo.
    goto mainEnd
)

if not exist %JAVA_RUN_BIN% (
	@echo %JAVA_RUN_BIN% not found.
	@echo.
	@echo No installed Java Runtime Environment found
	@echo :: FAIL
	@echo.
    goto mainEnd
)

if /i "%1"=="-h" (
	@echo Assembled the txt files in *.hex and *.bmp format.
	@echo Use '-q' parameter to start this script in quite mode.
	@echo.
	goto mainEnd
) else if "%1"=="-q" (
	set PrintInfo=0
)
goto init

:init
if not exist %JAVABIN% (
	if %PrintInfo% == 1 (
		@echo Making %JAVABIN% directory
	)
	mkdir %JAVABIN%
)

if not exist %ASSEMBLED% (
	if %PrintInfo% == 1 (
		@echo Making %ASSEMBLED% directory
	)
	mkdir %ASSEMBLED%
)

>nul 2>nul dir /a-d "%JAVABIN%*" || (
	if %PrintInfo% == 1 (
		@echo Compile java classes...
	)
	call %JAVA_DEV_BIN% -d %cd%\%JAVABIN% %cd%\%JAVASRC%*.java
)

>nul 2>nul dir /a-d "%ASSEMBLED%*" && (
	if %PrintInfo% == 1 (
		@echo Remove old files...
	)
	del /q %ASSEMBLED%*.*
)
goto assemble

:assemble
if %PrintInfo% == 1 (
	@echo Assembling font hex...
)
call %JAVA_RUN_BIN% -cp %cd%\%JAVABIN% Main font.txt 8 hex "%ASSEMBLED%/funscii-8.hex"
call %JAVA_RUN_BIN% -cp %cd%\%JAVABIN% Main font.txt 8 hex "%ASSEMBLED%/funscii-8-alt.hex"
call %JAVA_RUN_BIN% -cp %cd%\%JAVABIN% Main font.txt 8 hex "%ASSEMBLED%/funscii-8-fantasy.hex"
call %JAVA_RUN_BIN% -cp %cd%\%JAVABIN% Main font.txt 8 hex "%ASSEMBLED%/funscii-8-mcr.hex"
call %JAVA_RUN_BIN% -cp %cd%\%JAVABIN% Main font.txt 8 hex "%ASSEMBLED%/funscii-8-thin.hex"
call %JAVA_RUN_BIN% -cp %cd%\%JAVABIN% Main font.txt 16 hex "%ASSEMBLED%/funscii-16.hex"
if %PrintInfo% == 1 (
	@echo Assembling font bitmap...
)
call %JAVA_RUN_BIN% -cp %cd%\%JAVABIN% Main font.txt 8 bmp "%ASSEMBLED%/funscii-8-coverage.bmp" 256 256
call %JAVA_RUN_BIN% -cp %cd%\%JAVABIN% Main font.txt 16 bmp "%ASSEMBLED%/funscii-16-coverage.bmp" 256 256
goto mainEnd

:mainEnd
if %PrintInfo% == 1 (
	@echo Done
)
if "%OS%"=="Windows_NT" endlocal
