'Get Java path.
Dim path
Set shell = WScript.CreateObject("WScript.Shell")
path = shell.Environment.Item("JAVA_HOME")
If path = "" Then
	MsgBox "Could not find JAVA_HOME environment variable!", vbOKOnly, "NpcEditor"
Else
	If InStr(path, "\bin") = 0 Then
		path = path + "\bin\"
	Else
		path = path + "\"
	End If
	path = Replace(path, "\\", "\")
	path = Replace(path, "Program Files", "Progra~1")
End If

'Load GUI configuration.
window = 1
Set file = CreateObject("Scripting.FileSystemObject").OpenTextFile("config\Interface.ini", 1)
Do While Not file.AtEndOfStream
	If Replace(LCase(file.ReadLine()), " ", "") = "enablegui=true" Then
		window = 0
		Exit Do
	End If
Loop
file.Close
Set file = Nothing

'Generate command.
command = path & "java --enable-native-access=ALL-UNNAMED -cp ./../libs/* org.l2jmobius.tools.editor.NpcEditor"
If window = 1 Then
	command = "cmd /c start ""L2J Mobius - NPC / Mob Editor"" " & command
End If

'Run the NPC / Mob Editor.
exitcode = shell.Run(command, window, True)
