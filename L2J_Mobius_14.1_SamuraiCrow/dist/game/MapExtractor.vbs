'Get Java path.
Dim path
Set shell = WScript.CreateObject("WScript.Shell")
path = shell.Environment.Item("JAVA_HOME")
If path = "" Then
	MsgBox "Could not find JAVA_HOME environment variable!", vbOKOnly, "MapExtractor"
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

'Collect arguments: [<clientRoot> <outputRoot>] for headless mass generation.
args = ""
For Each arg In WScript.Arguments
	args = args & " """ & arg & """"
Next
If args <> "" Then
	window = 1
End If

'Generate command.
command = path & "java --enable-native-access=ALL-UNNAMED -Xmx8g -cp ./../libs/* org.l2jmobius.tools.MapExtractor" & args
If window = 1 Then
	command = "cmd /c start ""L2J Mobius - MapExtractor"" " & command
End If

'Run the MapExtractor.
exitcode = shell.Run(command, window, True)
