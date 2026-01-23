import re
import xml.etree.ElementTree as ET

svg_path = 'app.icon.source.svg'
launcher_output = 'ic_launcher_logo.xml'
notification_output = 'ic_notification_logo.xml'

def hex_to_android(hex_color):
    # #RRGGBB -> #FFRRGGBB
    if hex_color.startswith('#'):
        return '#FF' + hex_color.lstrip('#')
    return hex_color

def parse_svg_paths(svg_file):
    with open(svg_file, 'r') as f:
        content = f.read()
    
    # Simple regex to find paths and their attributes
    # We assume well-formed attributes from previous script
    paths = []
    path_matches = re.finditer(r'<path(.*?)/>', content, re.DOTALL)
    
    for match in path_matches:
        attrs_str = match.group(1)
        d_match = re.search(r'd="([^"]+)"', attrs_str)
        fill_match = re.search(r'fill="([^"]+)"', attrs_str)
        
        if d_match:
            path_data = {
                'd': d_match.group(1),
                'fill': fill_match.group(1) if fill_match else None
            }
            paths.append(path_data)
    return paths

paths = parse_svg_paths(svg_path)

# Header
vector_header = '''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="512dp"
    android:height="512dp"
    android:viewportWidth="512"
    android:viewportHeight="512">
'''

vector_footer = '</vector>'

# 1. Launcher Icon (Color + Gradients)
launcher_body = ''
for p in paths:
    d = p['d']
    fill = p['fill']
    
    launcher_body += '  <path\n'
    launcher_body += f'      android:pathData="{d}"'
    
    if fill and 'url(#left_grad)' in fill:
        # Add Gradient Complex Property
        launcher_body += '>\n'
        launcher_body += '    <aapt:attr name="android:fillColor">\n'
        launcher_body += '      <gradient\n'
        launcher_body += '          android:startX="0"\n'
        launcher_body += '          android:startY="0"\n'
        launcher_body += '          android:endX="0"\n'
        launcher_body += '          android:endY="512"\n'
        launcher_body += '          android:type="linear">\n'
        launcher_body += '        <item android:offset="0.0" android:color="#FF8DC9F3"/>\n'
        launcher_body += '        <item android:offset="1.0" android:color="#FF377DEA"/>\n'
        launcher_body += '      </gradient>\n'
        launcher_body += '    </aapt:attr>\n'
        launcher_body += '  </path>\n'
    elif fill and fill.startswith('#'):
        # Solid Color
        android_color = hex_to_android(fill)
        launcher_body += f'\n      android:fillColor="{android_color}"/>\n'
    else:
        # Fallback or transparent
        launcher_body += '/>\n'

with open(launcher_output, 'w') as f:
    f.write(vector_header + launcher_body + vector_footer)

# 2. Notification Icon (Monochrome White)
notif_body = ''
for p in paths:
    d = p['d']
    # Ignore original fill, use white
    notif_body += '  <path\n'
    notif_body += f'      android:pathData="{d}"\n'
    notif_body += '      android:fillColor="#FFFFFFFF"/>\n'

with open(notification_output, 'w') as f:
    f.write(vector_header + notif_body + vector_footer)

print(f"Generated {launcher_output} and {notification_output}")
