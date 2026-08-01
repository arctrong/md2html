import unittest
from typing import Dict, Tuple, Union

from models.document import Document
from models.options import Options
from models.page_metadata_handlers import PageMetadataHandlers
from page_metadata_utils import (
    MetadataHandlersApplicationResult,
    apply_metadata_handlers,
    join_parsing_results,
    process_nested_metadata,
)
from plugins.md2html_plugin import Md2HtmlPlugin, MetadataProcessingResult
from plugins.plugin_utils import list_from_string_or_array
from utils import UserError, VariableReplacer

from .testsupport.simulate_metadata_build import (
    documents_for_page_indices,
    simulate_metadata_build,
    wire_plugins,
)
from .utils_for_tests import ANY_DOCUMENT


class DeferPhaseTestPlugin(Md2HtmlPlugin):

    MARKER = 'DEFER'

    def is_blank(self) -> bool:
        return False

    def page_metadata_handlers(self):
        return [(self, self.MARKER, False)]

    def accept_page_metadata(self, doc: Document, marker: str, metadata: str,
                             metadata_section: str,
                             visited_markers: Union[Dict[str, None], None] = None,
                             phase: int = 1, data_from_prev_phase=None
                             ) -> MetadataProcessingResult:
        trimmed = metadata.strip()
        if phase == 1:
            if trimmed == 'immediate':
                return MetadataProcessingResult(trimmed)
            return MetadataProcessingResult(trimmed, defer=True)
        else:
            if trimmed == 'missing' or data_from_prev_phase == 'missing':
                raise UserError("Source 'missing' referenced but not defined")
            return MetadataProcessingResult(f'resolved:{data_from_prev_phase}')


class RecursiveExpandTestPlugin(Md2HtmlPlugin):
    
    DEFAULT_EXPANSIONS = {
            'EXPAND': ('<!--DEFER ${1}-->', True),
            'OUTER': ('A [<!--INNER ${1}-->] B', True),
            'INNER': ('C (<!--DEFER ${1}-->) D', True),
            'TWIN': ('start <!--DEFER ${1}--> mid <!--DEFER ${2}--> end', True),
        }

    def __init__(self, expansions: Dict[str, Tuple[str, bool]] = None):
        super().__init__()
        self._expansions = expansions if expansions is not None else self.DEFAULT_EXPANSIONS
        self.all_metadata_handlers = None

    def is_blank(self) -> bool:
        return False

    def accept_app_data(self, plugins: list, options: Options,
                        metadata_handlers: PageMetadataHandlers):
        self.all_metadata_handlers = metadata_handlers

    def page_metadata_handlers(self):
        return [(self, marker, False) for marker in self._expansions]

    def accept_page_metadata(self, doc: Document, marker: str, metadata_str: str,
                             metadata_section: str,
                             visited_markers: Union[Dict[str, None], None] = None,
                             phase: int = 1, data_from_prev_phase=None
                             ) -> MetadataProcessingResult:
        metadata_str = metadata_str.lstrip()
        try:
            metadata = list_from_string_or_array(metadata_str)
        except UserError as e:
            raise UserError(f'Error in recursive expand test entry: {str(e)}')

        template, recursive = self._expansions[marker]
        result = VariableReplacer(template).replace(metadata)
        if recursive:
            return process_nested_metadata(
                result, self.all_metadata_handlers, doc,
                visited_markers=visited_markers,
                recursive_marker=marker)
        return MetadataProcessingResult(result)


def _mechanism_plugins():
    return [RecursiveExpandTestPlugin(), DeferPhaseTestPlugin()]


class DeferPhaseTestPluginTest(unittest.TestCase):

    def test_immediate_metadata_is_processed_in_one_pass(self):
        metadata_handlers = wire_plugins([DeferPhaseTestPlugin()])
        result = apply_metadata_handlers(
            'before <!--DEFER immediate--> after', metadata_handlers, ANY_DOCUMENT)
        self.assertFalse(result.deferPage)
        self.assertEqual(
            'before immediate after',
            join_parsing_results(result.parsingResults, metadata_handlers, ANY_DOCUMENT))

    def test_deferred_metadata_defers_page_until_join(self):
        metadata_handlers = wire_plugins([DeferPhaseTestPlugin()])
        result = apply_metadata_handlers(
            'before <!--DEFER payload--> after', metadata_handlers, ANY_DOCUMENT)
        self.assertTrue(result.deferPage)
        self.assertEqual(
            'before resolved:payload after',
            join_parsing_results(result.parsingResults, metadata_handlers, ANY_DOCUMENT))

    def test_same_level_two_deferred_markers_on_page(self):
        result = simulate_metadata_build(
            [DeferPhaseTestPlugin()],
            [(0, 'start <!--DEFER a--> mid <!--DEFER b--> end')],
            documents_for_page_indices([0]),
        )
        self.assertTrue(result.deferred_pages[0])
        self.assertEqual('start resolved:a mid resolved:b end',
                         result.output[0])

class NestedMetadataMechanismTest(unittest.TestCase):

    @staticmethod
    def _run(pages, *, run_phase_2=True):
        indices = [index for index, _ in pages]
        return simulate_metadata_build(
            _mechanism_plugins(),
            pages,
            documents_for_page_indices(indices),
            run_phase_2=run_phase_2,
        )

    def test_one_recursion_level_then_defer(self):
        result = self._run([(0, 'before <!--EXPAND foo--> after')])
        self.assertTrue(result.deferred_pages[0])
        self.assertEqual('before resolved:foo after', result.output[0])

    def test_two_recursion_levels_then_defer(self):
        result = self._run([(0, '<!--OUTER x-->')])
        self.assertTrue(result.deferred_pages[0])
        self.assertEqual('A [C (resolved:x) D] B', result.output[0])

    def test_same_level_two_deferred_markers(self):
        result = self._run([(0, '<!--TWIN ["a", "b"]-->')])
        self.assertTrue(result.deferred_pages[0])
        self.assertEqual(
            'start resolved:a mid resolved:b end',
            result.output[0])

    def test_unresolved_defer_raises_on_phase_2_join(self):
        with self.assertRaises(UserError) as cm:
            self._run([(0, 'See <!--EXPAND missing-->.')])
        self.assertIn('referenced but not defined', str(cm.exception).lower())

    def test_deferred_page_without_phase_2_join_stays_unresolved(self):
        metadata_handlers = wire_plugins(_mechanism_plugins())
        result = apply_metadata_handlers(
            'before <!--EXPAND foo--> after', metadata_handlers, ANY_DOCUMENT)
        self.assertTrue(result.deferPage)
        deferred_item = result.parsingResults[1]
        self.assertEqual('EXPAND', deferred_item.marker)
        self.assertIsInstance(deferred_item.result, MetadataHandlersApplicationResult)
        inner_deferred = [
            item for item in deferred_item.result.parsingResults if item.marker_key]
        self.assertEqual(1, len(inner_deferred))
        self.assertEqual('DEFER', inner_deferred[0].marker)
