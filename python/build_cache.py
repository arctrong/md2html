import json
from pathlib import Path
from typing import Dict, List, Optional, Any


def _create_empty_cache() -> Dict[str, Any]:
    return {"files": {}}


class BuildCache:

    def __init__(self, cache_file: Path):
        self.cache_file = cache_file
        self.data = self._load_cache()

    def _load_cache(self) -> Dict[str, Any]:
        if not self.cache_file.exists():
            return _create_empty_cache()
        try:
            with open(self.cache_file, 'r', encoding='utf-8') as f:
                data = json.load(f)
            return data
        except (json.JSONDecodeError, OSError):
            return _create_empty_cache()

    def save(self) -> None:
        try:
            self.cache_file.parent.mkdir(parents=True, exist_ok=True)
            with open(self.cache_file, 'w', encoding='utf-8') as f:
                json.dump(self.data, f, indent=2)
        except OSError as e:
            print(f"Warning: Could not save cache file {self.cache_file}: {e}")

    def get_file_info(self, input_file: str) -> Optional[Dict[str, Any]]:
        """Get cached information for a file."""
        return self.data['files'].get(input_file)

    def update_file_info(self, input_file: str, info: Dict[str, Any]) -> None:
        """Update cached information for a file."""
        self.data['files'][input_file] = info

    def get_dependencies(self, input_file: str) -> List[str]:
        """Get list of files that this file depends on."""
        file_info = self.get_file_info(input_file)
        if file_info:
            return file_info.get('dependencies', [])
        return []

    def set_dependencies(self, input_file: str, dependencies: List[str]) -> None:
        """Set dependencies for a file."""
        if input_file not in self.data['files']:
            self.data['files'][input_file] = {}
        self.data['files'][input_file]['dependencies'] = dependencies

    def add_dependency(self, input_file: str, dependency: str) -> None:
        """Add a single dependency for a file."""
        deps = self.get_dependencies(input_file)
        if dependency not in deps:
            deps.append(dependency)
            self.set_dependencies(input_file, deps)

    def remove_file(self, input_file: str) -> None:
        """Remove a file from the cache."""
        self.data['files'].pop(input_file, None)

    def get_all_cached_files(self) -> List[str]:
        """Get list of all input files in cache."""
        return list(self.data['files'].keys())

    def clear(self) -> None:
        """Clear all cached data."""
        self.data = _create_empty_cache()
