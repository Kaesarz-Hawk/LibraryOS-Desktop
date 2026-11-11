@echo off
title LibraryOS Desktop Launcher
cd /d "%~dp0"
if exist "dist\LibraryOS\LibraryOS.exe" (
    start "" "dist\LibraryOS\LibraryOS.exe" %*
) else (
    java -cp "target\libraryos-1.0.0-SNAPSHOT.jar;target\lib\*" com.libraryos.app.AppLauncher %*
)

