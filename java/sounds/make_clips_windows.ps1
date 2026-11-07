# Generates one WAV per line of clips.csv (16-bit, mono, 22050 Hz).
# Run from this folder in Windows PowerShell 5.1 (not PowerShell 7).
# To pick a voice: $voice.GetInstalledVoices() then $voice.SelectVoice("<name>")

Add-Type -AssemblyName System.Speech
$voice = New-Object System.Speech.Synthesis.SpeechSynthesizer
$voice.Rate = 1
$fmt = New-Object System.Speech.AudioFormat.SpeechAudioFormatInfo(22050,
    [System.Speech.AudioFormat.AudioBitsPerSample]::Sixteen,
    [System.Speech.AudioFormat.AudioChannel]::Mono)
Import-Csv clips.csv | ForEach-Object {
    $voice.SetOutputToWaveFile((Join-Path $PWD "$($_.name).wav"), $fmt)
    $voice.Speak($_.text)
}
$voice.SetOutputToNull()   # closes the last file
