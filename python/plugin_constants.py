from plugins.index_plugin import IndexPlugin
from plugins.page_flows_plugin import PageFlowsPlugin
from plugins.page_variables_plugin import PageVariablesPlugin
from plugins.relative_paths_plugin import RelativePathsPlugin
from plugins.variables_plugin import VariablesPlugin

PLUGIN_PROVIDERS = {
    'relative-paths': lambda: RelativePathsPlugin(),
    "page-flows": lambda: PageFlowsPlugin(),
    'page-variables': lambda: PageVariablesPlugin(),
    "variables": lambda: VariablesPlugin(),
    'index': lambda: IndexPlugin()
}
