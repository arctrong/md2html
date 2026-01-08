import json
import os
from pathlib import Path
from typing import List, Optional

from models.document import Document

class BuildCacheManager:
    def __init__(self):
        self.build_cache: Optional[dict] = None
        self.current_arg_file_mtime = -1.0
        self.build_cache_file = ''

    def is_build_cache_used(self) -> bool:
        return bool(self.build_cache)

    def load_build_cache(self, build_cache_file, argument_file):
        self.current_arg_file_mtime = os.path.getmtime(argument_file)
        self.build_cache_file = build_cache_file
        build_cache_file_path = Path(self.build_cache_file)
        if build_cache_file_path.exists():
            with open(build_cache_file_path, 'r', encoding="utf-8") as file:
                self.build_cache = json.load(file)
        else:
            self.build_cache = {'arg_file_mtime': self.current_arg_file_mtime, 'documents': {}}

    def process_build_cache(self, verbose: bool, documents: List[Document]):
        force_all = False

        previous_arg_file_mtime = self.build_cache['arg_file_mtime']
        if self.current_arg_file_mtime > previous_arg_file_mtime:
            force_all = True
            self.build_cache['arg_file_mtime'] = self.current_arg_file_mtime
            if verbose:
                print('Argument file changed - all documents will be regenerated')
    
        new_documents = {d.input_file: d for d in documents}
        removed_input_files = set(self.build_cache['documents'].keys())
        unused_output_files = set(d['output_file'] for d in self.build_cache['documents'].values())
        for input_file in self.build_cache['documents'].keys():
            new_documents.pop(input_file, None)
        for document in documents:
            unused_output_files.discard(document.output_file)
            removed_input_files.discard(document.input_file)
    
        if new_documents:
            force_all = True
            for new_document in new_documents.values():
                self.build_cache['documents'][new_document.input_file] = {
                    'output_file': new_document.output_file}
            if verbose:
                print('New source files appeared - all documents will be regenerated')
        if removed_input_files:
            force_all = True
            for removed_input_file in removed_input_files:
                self.build_cache['documents'].pop(removed_input_file, None)
            if verbose:
                print('Some source files deleted - all documents will be regenerated')
    
        for outputfile in unused_output_files:
            if os.path.exists(outputfile):
                os.remove(outputfile)
                if verbose:
                    print(f'Unused output file deleted: {outputfile}')
    
        if force_all:
            for document in documents:
                document.force = True
    
    def save_build_cache(self, verbose: bool):
        if self.is_build_cache_used():
            with open(self.build_cache_file, 'w', encoding="utf-8") as file:
                json.dump(self.build_cache, file, indent=2)
            if verbose:
                print(f'Build cache file saved: {self.build_cache_file}')


build_cache_manager_singleton = BuildCacheManager()
