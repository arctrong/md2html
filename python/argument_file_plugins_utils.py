from plugin_constants import PLUGINS
from utils import UserError


def process_plugins(plugins_item):
    plugins = []
    for k, v in plugins_item.items():
        plugin = PLUGINS.get(k)
        if plugin:
            try:
                if plugin.accept_data(v):
                    plugins.append(plugin)
            except UserError as e:
                raise UserError(f"Error initializing plugin '{k}': {type(e).__name__}: {e}")
    return plugins
