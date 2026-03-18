import os
import re

def process_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()
    
    # We want to replace catch blocks that only do e.printStackTrace() 
    # to also show a JOptionPane and then return in order to avoid NullPointerExceptions later.

    imports_added = False
    if 'javax.swing.JOptionPane' not in content:
        # Add import
        content = re.sub(r'^(import\s+[^;]+;\n)', r'\1import javax.swing.JOptionPane;\n', content, count=1, flags=re.MULTILINE)
        imports_added = True

    # Find catch blocks: catch (ExceptionType e) { ... e.printStackTrace(); ... }
    # This regex looks for e.printStackTrace(); or e1.printStackTrace(); and turns it into a more robust block
    
    def replacer(match):
        var_name = match.group(1)
        return f'{var_name}.printStackTrace();\n            JOptionPane.showMessageDialog(null, "An error occurred: " + {var_name}.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);\n            return;'

    new_content = re.sub(r'(\w+)\.printStackTrace\(\);', replacer, content)

    if new_content != content:
        with open(file_path, 'w') as f:
            f.write(new_content)
        return True
    return False

root_dir = 'src/main/java'
modified_files = []
for dirpath, dirnames, filenames in os.walk(root_dir):
    for filename in filenames:
        if filename.endswith('.java'):
            filepath = os.path.join(dirpath, filename)
            if process_file(filepath):
                modified_files.append(filepath)

print("Modified files:")
for f in modified_files:
    print(f)
