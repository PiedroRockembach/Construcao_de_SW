@echo off
setlocal enabledelayedexpansion

REM Localizar executável do Maven
where mvn >nul 2>nul
if %ERRORLEVEL% equ 0 (
    set MVN_CMD=mvn
) else if exist "C:\Users\eduep\.maven\maven-3.9.15\bin\mvn.cmd" (
    set MVN_CMD="C:\Users\eduep\.maven\maven-3.9.15\bin\mvn.cmd"
) else (
    echo [ERRO] Maven nao encontrado no PATH nem no diretorio padrao.
    exit /b 1
)

echo ======================================================================
echo  Execucao de Testes de Mutacao com PITest (Ciclo 3)
echo ======================================================================

set TARGET=%~1

if "%TARGET%"=="" (
    echo Executando testes de mutacao em todos os microsservicos de negocio...
    call :run_pitest pecas-service
    call :run_pitest clientes-service
    call :run_pitest representantes-service
) else (
    call :run_pitest %TARGET%
)

echo.
echo ======================================================================
echo  Relatorios de Mutacao gerados:
echo   - pecas-service:          pecas-service\target\pit-reports\index.html
echo   - clientes-service:       clientes-service\target\pit-reports\index.html
echo   - representantes-service: representantes-service\target\pit-reports\index.html
echo ======================================================================
exit /b 0

:run_pitest
set SERVICE=%~1
echo.
echo ----------------------------------------------------------------------
echo  Rodando PITest no servico: %SERVICE%
echo ----------------------------------------------------------------------
cd "%~dp0\%SERVICE%"
call %MVN_CMD% test-compile pitest:mutationCoverage
cd "%~dp0"
goto :eof
