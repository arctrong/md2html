from plugins.index_plugin import IndexPlugin
from plugins.page_flows_plugin import PageFlowsPlugin
from plugins.page_variables_plugin import PageVariablesPlugin
from plugins.relative_paths_plugin import RelativePathsPlugin
from plugins.variables_plugin import VariablesPlugin

PLUGINS = {'relative-paths': RelativePathsPlugin(), "page-flows": PageFlowsPlugin(),
           'page-variables': PageVariablesPlugin(), "variables": VariablesPlugin(),
           'index': IndexPlugin()}
