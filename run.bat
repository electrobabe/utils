:: run batch script for Docker

@echo off
rem bat script for Windows

:: change this name
set SERVICE_NAME=my-service
set "__BAT_NAME=%~nx0"

echo **** YAY ****

if "%~1"=="" goto :usage
if /i "%~1"=="build"    call :build & goto :eof
if /i "%~1"=="start"    call :start & goto :eof
if /i "%~1"=="log"      call :log & goto :eof
if /i "%~1"=="stop"     call :stop & goto :eof
if /i "%~1"=="remove"   call :remove & goto :eof
goto :usage

:build
    echo Building service image...

    call docker build --rm --no-cache -t %SERVICE_NAME%:latest .
    goto :eof

:start
    echo Starting services...
    call docker-compose up -d
    goto :eof

:log
    echo Show logs of container %SERVICE_NAME%
    call docker logs %SERVICE_NAME% -f
    goto :eof

:stop
    echo Stopping services for %SERVICE_NAME%

    :: save return from command into variable 'container_id'
    for /f "tokens=*" %%a in (
        'call docker ps ^| grep -m1 %SERVICE_NAME% ^| awk "{print $1}"'
    ) do (
        set container_id=%%a
    )

    if defined container_id (
        echo docker stop container %container_id%
        call docker stop %container_id%
    )

    goto :eof

:remove
    echo Removing container for %SERVICE_NAME%

    :: save return from command into variable 'container_id'
    for /f "tokens=*" %%a in (
        'call docker ps ^| grep -m1 %SERVICE_NAME% ^| awk "{print $1}"'
    ) do (
        set container_id=%%a
    )

    if defined container_id (
        echo docker rm %container_id%
        call docker rm %container_id%

        echo Removing image
        call docker rmi %SERVICE_NAME%

        echo Successfully shutdown %SERVICE_NAME%
    )

    goto :eof

:usage
    echo USAGE:
    echo   %__BAT_NAME% (build ^| start ^| log ^| stop ^| remove)
    echo.
    echo.  build        Build service image
    echo.  start        Start service %SERVICE_NAME%
    echo.  log          Show logs of service %SERVICE_NAME%
    echo.  stop         Stop service %SERVICE_NAME%
    echo.  remove       Remove container and images of %SERVICE_NAME%
    goto :eof
