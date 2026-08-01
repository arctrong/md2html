import unittest

from page_metadata_utils import register_page_metadata_handlers
from utils import UserError
from .testsupport.stub_metadata_plugin import MarkerSpec, StubMetadataPlugin


class PageMetadataHandlersRegistrationTest(unittest.TestCase):

    def _assert_duplicate_marker(self, error: UserError, marker: str) -> None:
        message = str(error)
        self.assertIn('duplication', message.lower())
        self.assertIn(marker, message)

    def test_duplicate_markers_within_one_plugin(self):
        with self.assertRaises(UserError) as cm:
            register_page_metadata_handlers([
                StubMetadataPlugin(
                    MarkerSpec.anywhere("marker1"),
                    MarkerSpec.anywhere("marker2"),
                    MarkerSpec.anywhere("Marker1"),
                )])
        self._assert_duplicate_marker(cm.exception, "MARKER1")

    def test_duplicate_markers_across_plugins(self):
        with self.assertRaises(UserError) as cm:
            register_page_metadata_handlers([
                StubMetadataPlugin(MarkerSpec.anywhere("shared")),
                StubMetadataPlugin(MarkerSpec.anywhere("SHARED")),
            ])
        self._assert_duplicate_marker(cm.exception, "SHARED")

    def test_duplicate_markers_case_insensitively_within_one_plugin(self):
        with self.assertRaises(UserError) as cm:
            register_page_metadata_handlers([
                StubMetadataPlugin(
                    MarkerSpec.anywhere("red"),
                    MarkerSpec.anywhere("RED"),
                )])
        self._assert_duplicate_marker(cm.exception, "RED")

    def test_duplicate_same_marker_different_only_at_page_start(self):
        with self.assertRaises(UserError) as cm:
            register_page_metadata_handlers([
                StubMetadataPlugin(
                    MarkerSpec.at_page_start("VARIABLES"),
                    MarkerSpec.anywhere("variables"),
                )])
        self._assert_duplicate_marker(cm.exception, "VARIABLES")

    def test_distinct_markers_allowed(self):
        register_page_metadata_handlers([
            StubMetadataPlugin(
                MarkerSpec.anywhere("marker1"),
                MarkerSpec.anywhere("marker2"),
            )])
