from plugins.page_flows_plugin import PageFlowsPlugin
from plugins.page_variables_plugin import PageVariablesPlugin
from plugins.relative_paths_plugin import RelativePathsPlugin
from plugins.variables_plugin import VariablesPlugin

DEFAULT_TEMPLATE_PATH = '../doc_src/templates/default.html'
DEFAULT_CSS_FILE_PATH = '../doc/styles.css'
EXEC_NAME = 'md2html_py'
EXEC_VERSION = '1.0.0'

PLUGINS = {'relative-paths': RelativePathsPlugin(), "page-flows": PageFlowsPlugin(),
           'page-variables': PageVariablesPlugin(), "variables": VariablesPlugin()}
