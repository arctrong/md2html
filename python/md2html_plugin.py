from abc import ABC, abstractmethod


class PluginDataError(ValueError):
    pass


class Md2HtmlPlugin(ABC):

    @abstractmethod
    def accept_data(self, data):
        pass

    def variables(self, doc: dict) -> dict:
        return {}
