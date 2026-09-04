@echo off
title L2J Mobius - Login Server Console

:: 自動讀取登入服資料夾內的 java.cfg 優化參數
set /p JVM_SETTINGS=<java.cfg

:: 強制指定 Java 25 核心並運行登入服核心 (注意後面是 LoginServer.jar)
"C:\Program Files\BellSoft\LibericaJDK-25\bin\java.exe" %JVM_SETTINGS% -jar ../libs/LoginServer.jar
pause
