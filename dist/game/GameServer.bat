@echo off
title L2J Mobius - Game Server Console

:: 自動讀取資料夾內的 java.cfg 優化參數
set /p JVM_SETTINGS=<java.cfg

:: 強制指定 Java 25 核心並運行伺服器
"C:\Program Files\BellSoft\LibericaJDK-25\bin\java.exe" %JVM_SETTINGS% -jar ../libs/GameServer.jar
pause
