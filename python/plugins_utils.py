from typing import Dict

from plugins.index_plugin import IndexPlugin
from plugins.md2html_plugin import Md2HtmlPlugin
from plugins.page_flows_plugin import PageFlowsPlugin
from plugins.page_variables_plugin import PageVariablesPlugin
from plugins.relative_paths_plugin import RelativePathsPlugin
from plugins.variables_plugin import VariablesPlugin
from utils import UserError


PLUGIN_PROVIDERS = {
    'relative-paths': lambda: RelativePathsPlugin(),
    "page-flows": lambda: PageFlowsPlugin(),
    'page-variables': lambda: PageVariablesPlugin(),
    "variables": lambda: VariablesPlugin(),
    'index': lambda: IndexPlugin()
}


def process_plugins(plugins_item) -> dict:
    plugins = {}
    for k, v in plugins_item.items():
        plugin = PLUGIN_PROVIDERS.get(k)()
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
