import json
import logging
import os
from pathlib import Path
from typing import List, Optional

from models.document import Document
from utils import UserError


logger = logging.getLogger(__name__)


def get_empty_build_cache(arg_file_mtime):
    return {
        'arg_file_mtime': arg_file_mtime,
        'primary_documents': {},
        'standalone_derived_documents': set()
    }


class _BuildCacheManager:
    def __init__(self):
        self.previous_cache: Optional[dict] = None
        self.current_cache: Optional[dict] = None
        self.current_arg_file_mtime = -1.0
        self.build_cache_file = ''
        self.build_cache_file_existed_on_start = True

    def _is_build_cache_used(self) -> bool:
        return bool(self.previous_cache)

    def load_build_cache(self, build_cache_file, argument_file):
        if not argument_file:
            raise UserError("Cache file specified but no argument file provided. "
                            "Argument file must exist if cache file is used.")
        self.current_arg_file_mtime = os.path.getmtime(argument_file)
        self.build_cache_file = build_cache_file
        build_cache_file_path = Path(self.build_cache_file)
        if build_cache_file_path.exists():
            with open(build_cache_file_path, 'r', encoding="utf-8") as file:
                self.previous_cache = json.load(file)
            self.previous_cache.setdefault('primary_documents', {})
            self.previous_cache.setdefault('standalone_derived_documents', set())
        else:
            self.previous_cache = get_empty_build_cache(self.current_arg_file_mtime)
            self.build_cache_file_existed_on_start = False
            logger.info("Build cache file does not exist - all documents will be regenerated")
        self.current_cache = get_empty_build_cache(self.current_arg_file_mtime)

    def get_force_all(self, documents: List[Document]) -> bool:
        force_all = not self.build_cache_file_existed_on_start

        previous_arg_file_mtime = self.previous_cache['arg_file_mtime']
        if self.current_arg_file_mtime > previous_arg_file_mtime:
            force_all = True
            logger.info('Argument file changed - all documents will be regenerated')

        prev_documents = self.previous_cache.get('primary_documents', {})
        new_documents = {d.input_file: d for d in documents}
        removed_input_files = set(prev_documents.keys())

        for input_file in prev_documents.keys():
            new_documents.pop(input_file, None)
        for document in documents:
            removed_input_files.discard(document.input_file)

        if new_documents:
            force_all = True
            logger.info('New source files appeared - all documents will be regenerated')
        if removed_input_files:
            force_all = True
            logger.info('Some source files deleted - all documents will be regenerated')

        return force_all

    def record_dependency(self, input_file: str, dependency_path: str):
        if not self._is_build_cache_used():
            return
        doc = self.current_cache['primary_documents'].setdefault(input_file, {})
        doc.setdefault('dependencies', set()).add(dependency_path)

    def has_stale_dependencies(self, input_file: str, output_mtime: float) -> bool:
        if not self._is_build_cache_used():
            return False

        prev_doc = self.previous_cache.get('primary_documents', {}).get(input_file)
        if prev_doc is None:
            return True

        for dep in prev_doc.get('dependencies', []):
            try:
                if os.path.getmtime(dep) > output_mtime:
                    logger.info('Dependency changed: %s', dep)
                    return True
            except OSError:
                logger.info('ERROR: Dependency missing or unreadable: %s', dep)
                return True
        return False

    def record_primary_document(self, input_file: str, output_file: str, skipped: bool):
        if not self._is_build_cache_used():
            return

        doc = self.current_cache['primary_documents'].setdefault(input_file, {})
        doc['output_file'] = output_file
        if skipped:
            prev_doc = self.previous_cache.get('primary_documents', {}).get(input_file, {})
            old_derived = prev_doc.get('derived_documents', [])
            doc['derived_documents'] = set(old_derived)
            doc['dependencies'] = set(prev_doc.get('dependencies', []))
        else:
            doc.setdefault('dependencies', set())

    def record_derived_document_for_primary(self, primary_input: str, derived_output: str):
        if not self._is_build_cache_used():
            return
        
        doc = self.current_cache['primary_documents'].setdefault(primary_input, {})
        doc.setdefault("output_file", '')
        doc.setdefault("derived_documents", set()).add(derived_output)

    def record_standalone_derived_document(self, derived_output: str):
        if not self._is_build_cache_used():
            return
        self.current_cache['standalone_derived_documents'].add(derived_output)

    def delete_obsolete_files(self):
        if not self._is_build_cache_used():
            return

        new_primary_outputs = set()
        new_derived_outputs = set()

        for new_doc_info in self.current_cache['primary_documents'].values():
            new_primary_outputs.add(new_doc_info['output_file'])
            new_derived_outputs.update(new_doc_info.get('derived_documents', []))

        old_derived_outputs = set()

        for old_doc_info in self.previous_cache['primary_documents'].values():
            old_derived_outputs.update(old_doc_info.get('derived_documents', []))
            old_primary_output_file = old_doc_info['output_file']
            if old_primary_output_file not in new_primary_outputs and os.path.exists(
                    old_primary_output_file):
                os.remove(old_primary_output_file)
                logger.info('Unused primary file deleted: %s', old_primary_output_file)

        obsolete_derived_outputs = old_derived_outputs - new_derived_outputs

        for obsolete_derived_output in obsolete_derived_outputs:
            if os.path.exists(obsolete_derived_output):
                os.remove(obsolete_derived_output)
                logger.info('Obsolete derived file deleted: %s', obsolete_derived_output)

        old_standalone = set(self.previous_cache['standalone_derived_documents'])
        new_standalone = set(self.current_cache['standalone_derived_documents'])
        for standalone_file in old_standalone - new_standalone:
            if os.path.exists(standalone_file):
                os.remove(standalone_file)
                logger.info('Obsolete standalone derived file deleted: %s', standalone_file)

    def save_build_cache(self):
        if not self._is_build_cache_used():
            return

        for doc in self.current_cache['primary_documents'].values():
            if not doc.get('derived_documents'):
                doc.pop('derived_documents', None)
            else:
                doc['derived_documents'] = sorted(doc['derived_documents'])
            if not doc.get('dependencies'):
                doc.pop('dependencies', None)
            else:
                doc['dependencies'] = sorted(doc['dependencies'])
        self.current_cache["standalone_derived_documents"] = sorted(
            self.current_cache['standalone_derived_documents'])

        with open(self.build_cache_file, 'w', encoding="utf-8") as file:
            json.dump(self.current_cache, file, indent=2)
        logger.info('Build cache saved: %s', self.build_cache_file)


build_cache_manager = _BuildCacheManager()
