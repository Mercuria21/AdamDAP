import subprocess
import os

def convert_file(input_file, output_format):
    output_file = os.path.splitext(input_file)[0] + '.' + output_format
    
    command = ['ffmpeg', '-i', input_file, output_file]

    subprocess.run(command)
        
    return output_file