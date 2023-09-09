# yaml-ops

YAML support for DynamicOps / DataFixerUpper based on SnakeYaml.

## Usage

Create a new instance of `SnakeYamlOps` and use it just like you would `JsonOps`.

No `INSTANCE` field exists, as it is not thread safe due to some internal state stored by SnakeYaml.

### Dumping / Loading

See methods in `YamlHelper`. Use `YamlHelper.sortMappingKeys` to ensure a stable sort for Minecraft Datagen.
