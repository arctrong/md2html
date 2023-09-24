from typing import Dict, List

from models.document import Document
from plugins.ignore_plugin import IgnorePlugin
from plugins.include_file_plugin import IncludeFilePlugin
from plugins.index_plugin import IndexPlugin
from plugins.md2html_plugin import Md2HtmlPlugin
from plugins.page_flows_plugin import PageFlowsPlugin
from plugins.page_links_plugin import PageLinksPlugin
from plugins.page_variables_plugin import PageVariablesPlugin
from plugins.relative_paths_plugin import RelativePathsPlugin
from plugins.replace_plugin import ReplacePlugin
from plugins.variables_plugin import VariablesPlugin
from plugins.wrap_code_plugin import WrapCodePlugin
from utils import UserError

PLUGIN_PROVIDERS = {
    'relative-paths': lambda: RelativePathsPlugin(),
    "page-flows": lambda: PageFlowsPlugin(),
    'page-variables': lambda: PageVariablesPlugin(),
    "variables": lambda: VariablesPlugin(),
    'index': lambda: IndexPlugin(),
    'page-links': lambda: PageLinksPlugin(),
    'ignore': lambda: IgnorePlugin(),
    'wrap-code': lambda: WrapCodePlugin(),
    'include-file': lambda: IncludeFilePlugin(),
    'replace': lambda: ReplacePlugin(),
}


def instantiate_plugins(plugins_item) -> dict:
    plugins = {}
    for k, v in plugins_item.items():
        plugin_provider = PLUGIN_PROVIDERS.get(k)
        if plugin_provider is None:
            raise UserError(f"Unknown plugin: {k}")
        plugin = plugin_provider()
        if plugin:
            try:
                plugin.accept_data(v)
                plugins[k] = plugin
            except UserError as e:
                raise UserError(f"Error initializing plugin '{k}': {type(e).__name__}: {e}")
    return plugins


def filter_non_blank_plugins(plugins: Dict[str, Md2HtmlPlugin]) -> Dict[str, Md2HtmlPlugin]:
    non_blank_plugins = {}
    for k, v in plugins.items():
        if not v.is_blank():
            non_blank_plugins[k] = v
    return non_blank_plugins


def add_extra_plugin_data(extra_plugin_data, plugins):
    for plugin_name, plugin_data in extra_plugin_data.items():
        plugin = plugins.get(plugin_name)
        if plugin is not None:
            plugin.accept_data(plugin_data)


def complete_plugins_initialization(argument_file_dict, cli_args, plugins):
    extra_plugin_data_dict = {}
    for plugin in plugins.values():
        extra_plugin_data = plugin.pre_initialize(argument_file_dict, cli_args, plugins)
        for k, v in extra_plugin_data.items():
            data_for_plugin = extra_plugin_data_dict.setdefault(k, [])
            data_for_plugin.append(v)
    for name, plugin in plugins.items():
        data_for_plugin = extra_plugin_data_dict.get(name, [None])
        for data in data_for_plugin:
            plugin.initialize(data)


def feed_plugins_with_documents(plugins: Dict[str, Md2HtmlPlugin], documents: List[Document]):
    for plugin in plugins.values():
        plugin.accept_document_list(documents)


def feed_plugins_with_app_data(plugins, arguments, metadata_handlers):
    for plugin in plugins.values():
        plugin.accept_app_data(arguments.plugins, arguments.options, metadata_handlers)
