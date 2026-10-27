"""Builds the TV launcher motion prototype. Usage: proto.py ICON_DIR ART_PREFIX OUT_FILE"""
import json, re, sys, pathlib

ICONS = pathlib.Path(sys.argv[1])
ART = sys.argv[2]          # path prefix for artwork (absolute for inline render, relative for the saved copy)
OUT = pathlib.Path(sys.argv[3])


def svg_file(name):
    s = (ICONS / f"{name}.svg").read_text()
    s = re.sub(r"<title>.*?</title>", "", s)
    s = re.sub(r'\s(width|height)="[^"]*"', "", s.split(">", 1)[0]) + ">" + s.split(">", 1)[1]
    return s.replace("#fff\"", "#ffffff\"")


def stroke(inner, w=1.7):
    return (f'<svg viewBox="0 0 24 24" fill="none" stroke="#ffffff" stroke-width="{w}" '
            f'stroke-linecap="round" stroke-linejoin="round">{inner}</svg>')


icons = {n: svg_file(n) for n in ["youtube", "netflix", "xbox", "apps", "settings", "disneyplus",
                                  "hbo", "tubi", "appletv", "primevideo"]}
icons.update({
    "speaker": stroke('<path d="M11 5 6 9H2v6h4l5 4V5z"/><path d="M15.5 8.5a5 5 0 0 1 0 7"/><path d="M19 5a10 10 0 0 1 0 14"/>'),
    "soundbar": stroke('<rect x="2" y="9" width="20" height="6" rx="2"/><circle cx="7" cy="12" r=".8" fill="#fff"/><circle cx="17" cy="12" r=".8" fill="#fff"/>'),
    "tvspk": stroke('<rect x="2" y="4" width="20" height="13" rx="2"/><path d="M8 21h8"/>'),
    "headphones": stroke('<path d="M3 14h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a9 9 0 0 1 18 0v7a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3"/>'),
    "plus": stroke('<path d="M12 5v14M5 12h14"/>'),
    "chev": stroke('<path d="M9 6l6 6-6 6"/>', 2),
    "hdd": stroke('<path d="M22 12H2"/><path d="M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z"/><path d="M6 16h.01"/><path d="M10 16h.01"/>'),
    "tv": stroke('<rect width="20" height="15" x="2" y="7" rx="2"/><path d="m17 2-5 5-5-5"/>'),
    "pc": stroke('<rect x="2" y="4" width="20" height="13" rx="2"/><path d="M8 21h8M12 17v4"/>'),
})

html = (pathlib.Path(__file__).parent / "proto.template.html").read_text()
html = html.replace("__ICONS__", json.dumps(icons)).replace("__ART__", ART)
OUT.write_text(html)
print(OUT, len(html))
