@echo off
setlocal enabledelayedexpansion

echo Compiling Java...
javac -d bin src/game/*.java src/players/Player20220808060.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    exit /b 1
)

echo Running tests...
set STUDENT_ID=20220808060
set LOG_FILE=results/Player%STUDENT_ID%_Summary.log

REM Create results directory if it doesn't exist
if not exist results mkdir results

REM Variables to track total scores for each board size
set TOTAL_10=0
set TOTAL_25=0
set TOTAL_50=0
set COUNT_10=0
set COUNT_25=0
set COUNT_50=0

REM Run tests on all board sizes
for %%s in (10 25 50) do (
    echo Testing %%sx%%s boards...
    
    for %%i in (1 2 3 4 5) do (
        if exist boards/board_%%sx%%s_%%i.dat (
            echo - Board %%sx%%s #%%i
            
            REM Run the test and capture the final score
            java -cp bin game.Tester boards/board_%%sx%%s_%%i.dat %STUDENT_ID% > temp_output.txt
            
            REM Extract score for averaging
            for /f "tokens=2" %%a in ('findstr /C:"%STUDENT_ID%" temp_output.txt') do (
                set SCORE=%%a
                
                REM Add to the appropriate total
                if %%s==10 (
                    set /a TOTAL_10+=!SCORE!
                    set /a COUNT_10+=1
                ) else if %%s==25 (
                    set /a TOTAL_25+=!SCORE!
                    set /a COUNT_25+=1
                ) else if %%s==50 (
                    set /a TOTAL_50+=!SCORE!
                    set /a COUNT_50+=1
                )
            )
            
            del temp_output.txt
        )
    )
)

REM Create a fresh log file with only the summary
echo Results for Player%STUDENT_ID% > %LOG_FILE%
echo ================================== >> %LOG_FILE%
echo. >> %LOG_FILE%

REM Calculate and log averages
echo Summary of Scores by Board Size: >> %LOG_FILE%
echo ---------------------------------- >> %LOG_FILE%

if !COUNT_10! GTR 0 (
    set /a AVG_10=!TOTAL_10!/!COUNT_10!
    echo 10x10 boards: !AVG_10! points average ^(!TOTAL_10! total from !COUNT_10! boards^) >> %LOG_FILE%
) else (
    echo 10x10 boards: No tests run >> %LOG_FILE%
)

if !COUNT_25! GTR 0 (
    set /a AVG_25=!TOTAL_25!/!COUNT_25!
    echo 25x25 boards: !AVG_25! points average ^(!TOTAL_25! total from !COUNT_25! boards^) >> %LOG_FILE%
) else (
    echo 25x25 boards: No tests run >> %LOG_FILE%
)

if !COUNT_50! GTR 0 (
    set /a AVG_50=!TOTAL_50!/!COUNT_50!
    echo 50x50 boards: !AVG_50! points average ^(!TOTAL_50! total from !COUNT_50! boards^) >> %LOG_FILE%
) else (
    echo 50x50 boards: No tests run >> %LOG_FILE%
)

echo. >> %LOG_FILE%
if !COUNT_10! GTR 0 set /a TOTAL_BOARDS=!COUNT_10!+!COUNT_25!+!COUNT_50!
if !COUNT_10! GTR 0 set /a TOTAL_SCORE=!TOTAL_10!+!TOTAL_25!+!TOTAL_50!
if !COUNT_10! GTR 0 set /a OVERALL_AVG=!TOTAL_SCORE!/!TOTAL_BOARDS!
if !COUNT_10! GTR 0 echo Overall average: !OVERALL_AVG! points across all !TOTAL_BOARDS! boards >> %LOG_FILE%

echo Tests completed! Results saved to %LOG_FILE%
echo.
echo Summary of results:
echo ===================
if !COUNT_10! GTR 0 echo 10x10 boards: !AVG_10! points average
if !COUNT_25! GTR 0 echo 25x25 boards: !AVG_25! points average
if !COUNT_50! GTR 0 echo 50x50 boards: !AVG_50! points average
if !COUNT_10! GTR 0 echo Overall average: !OVERALL_AVG! points across all !TOTAL_BOARDS! boards

endlocal 