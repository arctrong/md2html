import glob
from pathlib import Path


def main():
    print(Path().absolute())
    result = glob.glob('../**/*.txt', recursive=True)
    print('\n'.join(result))
    print(f'len(result)={len(result)}')


if __name__ == '__main__':
    main()

