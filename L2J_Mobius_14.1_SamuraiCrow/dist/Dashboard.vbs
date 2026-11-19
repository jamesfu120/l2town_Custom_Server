Dim shell
Dim fileSystem
Dim scriptDirectory
Dim javaHome
Dim javaExecutable
Dim command

Set shell = WScript.CreateObject("WScript.Shell")
Set fileSystem = CreateObject("Scripting.FileSystemObject")
scriptDirectory = fileSystem.GetParentFolderName(WScript.ScriptFullName)
shell.CurrentDirectory = scriptDirectory

javaHome = "C:\Program Files\BellSoft\LibericaJDK-25"
javaExecutable = fileSystem.BuildPath(javaHome, "bin\javaw.exe")

command = Chr(34) & javaExecutable & Chr(34) & " --enable-native-access=ALL-UNNAMED -Dfile.encoding=UTF-8 -jar libs\Dashboard.jar"
shell.Run command, 0, False
