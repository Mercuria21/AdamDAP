import subprocess
import os

def convert_file(input_file, output_format):
    # If the input file already has the desired extension, skip conversion
    current_ext = os.path.splitext(input_file)[1].lstrip('.').lower()
    if current_ext == output_format.lower():
        return input_file

    base = os.path.splitext(input_file)[0]
    output_file = f"{base}.{output_format}"

    command = ['ffmpeg', '-i', input_file, output_file]

    subprocess.run(command, check=True)

    return output_file