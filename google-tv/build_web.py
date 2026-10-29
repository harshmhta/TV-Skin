"""Derive the Google TV page from the upstream HTML, keeping its UI and motion."""
from pathlib import Path
import re
import json

root = Path(__file__).resolve().parent
html = (root.parent / "prototype/index.html").read_text()
html = html.replace('<head>', '<head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1"><meta http-equiv="Content-Security-Policy" content="default-src \'none\'; script-src \'self\' \'unsafe-inline\'; style-src \'self\' \'unsafe-inline\'; img-src https: data:; media-src https:; font-src \'self\'; base-uri \'none\'; form-action \'none\'">', 1)
html = html.replace("media-src https:;", "media-src https:; connect-src https://api.open-meteo.com;")
profile = json.loads((root / 'app/src/main/assets/profile.json').read_text())
html = re.sub(r'<link[^>]+fonts\.googleapis\.com[^>]*>', '', html)
start = html.index('/* ---------- artwork engine')
end = html.index('/* ---------- data ---------- */', start)
html = html[:start] + (root / 'web/backdrop.js').read_text() + '\n' + html[end:]
# The prototype's harness stays in the DOM because its handlers reference it, but is hidden.
html = html.replace('</head>', '<style>' + (root / 'web/android.css').read_text() + '</style></head>')
html = html.replace('<script>\nwindow.addEventListener', '<script>\n' + (root / 'web/catalog.js').read_text() + '\nwindow.addEventListener', 1)
icon_map = json.loads((root / 'web/icon-map.json').read_text())
icons = {pkg: (root / 'web/icons' / (name + '.svg')).read_text() for pkg, name in icon_map.items() if name != 'settings'}
html = html.replace('<script>\nfunction cleanPins', '<script>\nconst WHITE_ICONS=' + json.dumps(icons) + ';\nfunction cleanPins', 1)
html = html.replace('const WHITE_ICONS=', 'const DEFAULT_PROFILE=' + json.dumps(profile) + ';\nconst WHITE_ICONS=', 1)
# Keep upstream branding, but create every visible app from the installed catalog.
html = html.replace('const APPS={', 'const BRANDS={', 1)
html = html.replace('const MAX_PINNED=7;', 'const APPS={};\nconst MAX_PINNED=7;', 1)
html = html.replace("let pinned=['youtube','netflix','xbox','viki','prime'];", 'let pinned=[];')
html = html.replace("tick();setInterval(tick,10000);\nbuildDock(false);", 'tick();setInterval(tick,10000);')
html = html.replace('<span class="pt" id="pilltext">Netflix</span>', '<span class="pt" id="pilltext"></span>')
review_start = html.index('/* ---------- review hooks:')
review_end = html.index('</script>', review_start)
html = html[:review_start] + html[review_end:]
clock_start = html.index('/* ---------- clock ---------- */')
clock_end = html.index('</script>', clock_start)
html = html[:clock_start] + (root / 'web/header.js').read_text() + '\n' + html[clock_end:]
html = html.replace('<div class="clock" id="clock">9:41</div>', '<div class="status-line"><div class="clock" id="clock"></div><div class="weather" id="weather" role="status" hidden></div></div>')
html = html.replace("{id:'sound',kind:'sound'}", "{id:'inputs',kind:'inputs'}")
html = html.replace("if(d.id==='sound'){", "if(d.id==='inputs'){")
html = html.replace("'item'+(d.id==='sound'?' wide':'')", "'item'")
html = html.replace("<span class=\"ic\" style=\"width:34px;height:34px\">${ICONS.speaker}</span><span class=\"lbl\">Sound</span>", '<span class="ic" style="width:44px;height:44px" aria-label="Inputs"><svg viewBox="0 0 24 24" fill="none" stroke="#ffffff" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M10 4h10v16H10M3 12h11m-4-4 4 4-4 4"/></svg></span>')
html = html.replace("d.id==='all'?'All apps':'Sound'", "d.id==='all'?'All apps':'Inputs'")
html = html.replace('</body>', '<script>' + (root / 'web/android.js').read_text() + '</script></body>')
out = root / 'app/src/main/assets/index.html'
out.write_text(html)
print(f'Generated {out.relative_to(root)} from prototype/index.html')
