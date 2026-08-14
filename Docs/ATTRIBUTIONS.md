# Port attributions

## Tinkers' Construct static and table slices

The static registry sweep adapts registration names, English strings, and
selected item/block texture assets from
References/TinkersConstruct-1.20.1, primarily its `TinkerMaterials`,
`TinkerToolParts`, `TinkerModifiers`, `TinkerTables`, and `TinkerCommons`
registrations, table recipe provider, and resource data. The reference project identifies its code,
textures, and binaries as MIT-licensed and credits SlimeKnights. The selected
assets were copied into the new `moderntinkers` namespace; no legacy build
files or source files were modified. Placeholder blocks and items are
port-specific registrations, the Crafting Station delegates to vanilla's
crafting-table behavior, and the new recipe JSON adapts the reference pattern
with vanilla tags. The Part Builder systems slice adapts the reference
`PartBuilderBlockEntity`, `PartBuilderContainerMenu`, `Pattern`, and
`ItemPartRecipe` behavior into a smaller NeoForge implementation: current
ingot/nugget inputs are mapped to explicit unit costs, patterns store a selected
part in `DataComponents.CUSTOM_DATA`, and output parts store a namespaced
material key. The full Tinkers material registry and recipe codecs are not
claimed by this slice.

The reference license notice follows:

MIT License

Copyright (c) 2022 SlimeKnights

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
