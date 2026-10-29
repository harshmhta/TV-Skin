#!/usr/bin/env python3
"""Decode real, TV-generated preview clips with an independent decoder."""
import argparse
import json
from pathlib import Path
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("clips", nargs="+", type=Path)
args = parser.parse_args()
for clip in args.clips:
    probe = subprocess.check_output([
        "ffprobe", "-v", "error", "-select_streams", "v:0", "-show_streams",
        "-of", "json", str(clip),
    ])
    stream = json.loads(probe)["streams"][0]
    if (stream["width"], stream["height"]) != (3840, 2160):
        raise SystemExit(f"{clip.name}: expected 3840×2160")
    subprocess.run([
        "ffmpeg", "-v", "error", "-xerror", "-err_detect", "explode",
        "-i", str(clip), "-f", "null", "-",
    ], check=True)
    print(f"PASS {clip.name}: 3840×2160, both frames decode without errors")
