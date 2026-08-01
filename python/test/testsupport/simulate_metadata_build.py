from dataclasses import dataclass
from typing import Dict, List, Sequence, Tuple

from cli_arguments_utils import CliArgDataObject
from md2html import load_json_argument_file, register_page_metadata_handlers
from models.document import Document
from models.options import Options
from models.page_metadata_handlers import PageMetadataHandlers
from page_metadata_utils import apply_metadata_handlers, join_parsing_results
from plugins.md2html_plugin import Md2HtmlPlugin

from ..utils_for_tests import parse_argument_file_for_test


@dataclass
class SimulateMetadataBuildResult:
    output: Dict[int, str]
    deferred_pages: Dict[int, bool]
    plugins: List[Md2HtmlPlugin]
    metadata_handlers: PageMetadataHandlers
    documents: List[Document]


def wire_plugins(plugins: List[Md2HtmlPlugin],
                 documents: Sequence[Document] = None) -> PageMetadataHandlers:
    metadata_handlers = register_page_metadata_handlers(plugins)
    if documents is not None:
        for plugin in plugins:
            plugin.accept_document_list(list(documents))
    for plugin in plugins:
        plugin.accept_app_data([], Options(), metadata_handlers)
    return metadata_handlers


def documents_for_page_indices(indices: Sequence[int]) -> Dict[int, Document]:
    return {
        index: Document(
            input_file=f'page{index}.txt',
            output_file=f'page{index}.html',
        )
        for index in indices
    }


def simulate_metadata_build(
        plugins: List[Md2HtmlPlugin],
        pages: Sequence[Tuple[int, str]],
        documents_by_index: Dict[int, Document],
        *,
        run_phase_2: bool = True,
        all_documents: Sequence[Document] = None,
) -> SimulateMetadataBuildResult:
    documents_for_plugins = (list(documents_by_index.values()) if all_documents is None
                             else list(all_documents))
    metadata_handlers = wire_plugins(plugins, documents_for_plugins)

    output: Dict[int, str] = {}
    deferred = {}
    deferred_pages: Dict[int, bool] = {}

    for index, text in pages:
        doc = documents_by_index[index]
        for plugin in plugins:
            plugin.new_page(doc)
        result = apply_metadata_handlers(text, metadata_handlers, doc)
        deferred_pages[index] = result.deferPage
        if result.deferPage:
            deferred[index] = result
        else:
            output[index] = join_parsing_results(
                result.parsingResults, metadata_handlers, doc)

    if run_phase_2:
        for index, result in deferred.items():
            doc = documents_by_index[index]
            output[index] = join_parsing_results(
                result.parsingResults, metadata_handlers, doc)

    ordered_documents = [documents_by_index[index] for index, _ in pages]
    return SimulateMetadataBuildResult(
        output=output,
        deferred_pages=deferred_pages,
        plugins=plugins,
        metadata_handlers=metadata_handlers,
        documents=ordered_documents,
    )


def simulate_metadata_build_from_arg_file(
        arg_file_str: str,
        pages: Sequence[Tuple[int, str]],
        *,
        run_phase_2: bool = True,
) -> SimulateMetadataBuildResult:
    args = parse_argument_file_for_test(
        load_json_argument_file(arg_file_str), CliArgDataObject())
    documents_by_index = {index: args.documents[index] for index, _ in pages}
    return simulate_metadata_build(
        args.plugins,
        pages,
        documents_by_index,
        run_phase_2=run_phase_2,
        all_documents=args.documents,
    )
