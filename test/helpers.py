import os
import shutil
from pathlib import Path
import subprocess
from bs4 import BeautifulSoup
import time

WORKING_DIR = Path(__file__).resolve().parent

INPUT_DIR = str(WORKING_DIR.joinpath('test_input'))
OUTPUT_DIR = None

EXEC = None


def run_with_parameters(params):
    subprocess.run(EXEC + params)


def prepare_output_directory(dir_name):
    if not Path(OUTPUT_DIR).exists():
        os.makedirs(OUTPUT_DIR)
    test_output_dir = Path(OUTPUT_DIR).joinpath(dir_name)
    if test_output_dir.exists():
        shutil.rmtree(test_output_dir)
        time.sleep(0.2)  # this must prevent "Access is denied" error
    os.mkdir(test_output_dir)
    return str(test_output_dir)


def execute(params, output_file):
    run_with_parameters(params)
    with open(output_file) as html_file:
        root = BeautifulSoup(html_file, 'html.parser')
    return root


def execute_simple(input_file, output_file, template, *args):
    cli_args = ['-f', '-i', input_file, '-o', output_file, '--template', template]
    for arg in args:
        cli_args.append(arg)
    return execute(cli_args, output_file)


IMPLEMENTATION = os.environ['IMPLEMENTATION']
if IMPLEMENTATION == 'py':
    EXEC = ['python' if os.name == 'nt' else 'python3', 
            os.environ['MD2HTML_HOME'] + '/python/md2html.py']
elif IMPLEMENTATION == 'java':
    EXEC = ['java', '-jar', os.environ['MD2HTML_HOME'] + '/java/target/md2html-0.1.3-bin.jar']
else:
    raise Exception('Unknown implementation')

OUTPUT_DIR = str(WORKING_DIR.joinpath(f'test_output/{IMPLEMENTATION}'))
