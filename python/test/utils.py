from pathlib import Path
from datetime import datetime

CURRENT_DIR = Path(__file__).parent
TEMP_DIR = CURRENT_DIR.joinpath('test_output')


def create_temp_file(content: str):
    file_path = TEMP_DIR.joinpath(datetime.today().strftime('%Y_%m_%d_T_%H_%M_%S') + '.txt')
    with open(file_path, 'w') as f:
        f.write(content)
    return file_path
