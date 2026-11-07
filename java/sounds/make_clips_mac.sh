#!/usr/bin/env bash
# Generates one WAV per line of clips.csv (16-bit, mono, 22050 Hz).
# Run from this folder. List voices with: say -v '?'
set -euo pipefail
tail -n +2 clips.csv | while IFS=, read -r name text; do
  say -o "/tmp/$name.aiff" "$text"
  afconvert -f WAVE -d LEI16@22050 -c 1 "/tmp/$name.aiff" "$name.wav"
done
