echo off
:: gradlew.bat script for Windows OS

set DIR=%~dp0
if "%DIR%"=="" set DIR=.

if exist "%DIR%gradle\wrapper\gradle-wrapper.jar" (
  java -Duser.home="%DIR%" -cp "%DIR%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
) else (
  echo Unable to locate the gradle-wrapper.jar file. Please run 'gradle wrapper' first.
  exit /b 1
)