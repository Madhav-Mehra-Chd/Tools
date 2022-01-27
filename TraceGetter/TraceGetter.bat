
@ECHO OFF
set PATH=%systemroot%\System32;%PATH%;
mode con:cols=130
color 70
TITLE TraceGetter
:start
cls
set "email=BlankEmail"
set /p id1="Enter Server Name : "
set /p id2="Enter Identifier : "
set /p email="Enter Email : "
set /p id4="Enter Prefix to be added before Trace File: "


set PATH="C:\Program Files (x86)\Java\jre7\bin";%PATH%
set classpath=%classpath%;.C:\TraceGetter\helper\TraceGetter.class;C:\TraceGetter\helper\jsch-0.1.55.jar;C:\TraceGetter;C:\TraceGetter\helper\mail.jar;C:\TraceGetter;C:\TraceGetter\helper\activation-1.1.1.jar;C:\TraceGetter;

java  TraceGetter %id1% %id2% %email% %id4%
pause
echo %ERRORLEVEL%


IF ERRORLEVEL 1000 goto start
IF ERRORLEVEL 2 goto runagain
IF ERRORLEVEL 1 goto start
IF ERRORLEVEL 0 goto start

:runagain

set /p id5="Enter Identifier Again : "
set /p id6="Enter Server Name : "
java  TraceGetter %id6% %id5% %email% %id4%
pause

IF ERRORLEVEL 2 goto runagain
IF ERRORLEVEL 1 goto start
IF ERRORLEVEL 0 goto start
:exit1
%SystemRoot%\explorer.exe "C:\TraceGetter\Traces"
:exit
echo Bye.

