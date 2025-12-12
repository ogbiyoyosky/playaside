#!/usr/bin/env python3
import os
import re
import glob

def fix_datetime_in_file(filepath):
    """Replace LocalDateTime with OffsetDateTime in a file"""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()

        # Replace imports
        content = re.sub(r'import java\.time\.LocalDateTime;', 'import java.time.OffsetDateTime;', content)

        # Replace all other occurrences
        content = content.replace('LocalDateTime', 'OffsetDateTime')

        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)

        print(f"Fixed {filepath}")
        return True
    except Exception as e:
        print(f"Error processing {filepath}: {e}")
        return False

def main():
    # Find all Java files in repo
    pattern = "src/main/java/com/playvora/playvora_api/**/repo/*.java"
    files = glob.glob(pattern, recursive=True)

    print(f"Found {len(files)} repository files to process")

    success_count = 0
    for filepath in files:
        if fix_datetime_in_file(filepath):
            success_count += 1

    print(f"Successfully processed {success_count} out of {len(files)} files")

if __name__ == "__main__":
    main()