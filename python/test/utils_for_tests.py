def find_single_instance_of_type(instances: list, instance_type: type):
    result = None
    for instance in instances:
        if isinstance(instance, instance_type):
            if result is None:
                result = instance
            else:
                raise Exception(f"More than one instances of type '{type(instance).__name__}' found.")
    return result
