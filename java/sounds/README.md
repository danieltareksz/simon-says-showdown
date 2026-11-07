# Voice clips

`clips.csv` lists every clip the game plays (`name,text`). The screen text for each instruction comes from the same file, so the voice and the screen always match.

The `.wav` files are generated and not committed. Make them once on your machine, from this folder:

| OS | Command |
| --- | --- |
| Windows | `powershell -ExecutionPolicy Bypass -File make_clips_windows.ps1` |
| macOS | `./make_clips_mac.sh` |

You can also record the lines yourself. Every clip must be WAV, 16-bit PCM, mono, 22,050 Hz; trim the silence at both ends. The game skips any clip in a different format and prints its name.

Keep commas out of the spoken text in `clips.csv`.
