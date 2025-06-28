# UVL - Universal Variability Language Java Feature Model Metamodel

This is a small default library used to manipulate UVL metamodels in JAVA. It uses the [UVL-Grammar](https://github.com/Universal-Variability-Language/) to parse the a given metamodel. If parsed the model can be modified in JAVA. It is possible to also convert the models to different levels.

**How to use UVL** can be found in this paper
[![DOI](https://img.shields.io/badge/DOI-10.1016%2Fj.jss.2024.112326-blue)](https://doi.org/10.1016/j.jss.2024.112326)

## ✨ Key Features

- full support of UVL-Syntax (nampaces, feature hircharcies and constraints)
- convert between different language levels
- direct access to feature model components like features, groups and attributes

## 💡 Usages/Examples

To use the Java-fm-metamodel it needs to be built with [Maven](https://maven.apache.org/):

```bash
mvn clean compile
```

More usage examples that also show how to use the acquired UVLModel object can be found in [src/main/java/de/vill/main/Example.java](https://github.com/Universal-Variability-Language/java-fm-metamodel/blob/main/src/main/java/de/vill/main/Example.java)

### Parsing

```Java
// First option:
UVLModelFactory uvlModelFactory = new UVLModelFactory();
FeatureModel featureModel = uvlModelFactory.parse(Paths.get("path/to/your/file.uvl"));

// Second option:
Path filePath = Paths.get(pathAsString);
String content = new String(Files.readAllBytes(filePath));
UVLModelFactory uvlModelFactory = new UVLModelFactory();
FeatureModel featureModel = uvlModelFactory.parse(content);
```

The class `de.vill.main.UVLModelFactory` exposes the static method `parse(String)` which will return an instance of a `de.vill.model.FeatureModel` class. If there is something wrong, a `de.vill.exception.ParseError` is thrown. The parser tries to parse the whole model, even if there are errors. If there are multiple errors, a `de.vill.exception.ParseErrorList` is returned which contains all errors that occurred.

### Modifing

```Java
Feature feature = featureModel.getFeatureMap().get(featureName);
if (feature != null) {
  Attribute<?> attribute = feature.getAttributes().get(attributeName);
  if (attribute != null) {
    System.out.println("Name of the feature" + feature.getFeatureName());
    System.out.println("Name of the attribute" + attribute.getName());
  }
}

// Conversion
ConvertTypeLevel converter = new ConvertTypeLevel();
converter.convertFeatureModel(featureModel, convertedModel);
```

### Writing into a file

```Java
// Write
String uvlModel = featureModel.toString();
Path filePath = Paths.get(featureModel.getNamespace() + ".uvl");
Files.write(filePath, uvlModel.getBytes());
```

A model can be printed with the `toString()` method of the `de.vill.model.FeatureModel` object.
The following snippet shows a minimal example to read and write UVL models using the jar.

## ⚙️ Running Tests

The Java-fm-metamodel can be tested by running:

```bash
mvn test
```

## 📖 Citation

If you use UVL in your research, please cite:

```bibtex
@article{UVL2024,
  title     = {UVL: Feature modelling with the Universal Variability Language},
  journal   = {Journal of Systems and Software},
  volume    = {225},
  pages     = {112326},
  year      = {2025},
  issn      = {0164-1212},
  doi       = {https://doi.org/10.1016/j.jss.2024.112326},
  url       = {https://www.sciencedirect.com/science/article/pii/S0164121224003704},
  author    = {David Benavides and Chico Sundermann and Kevin Feichtinger and José A. Galindo and Rick Rabiser and Thomas Thüm},
  keywords  = {Feature model, Software product lines, Variability}
}
```

## Links

UVL models:

- https://github.com/Universal-Variability-Language/uvl-models

UVL parser:

- https://github.com/Universal-Variability-Language/uvl-parser

Other parsers:

- https://github.com/Universal-Variability-Language/uvl-parser _deprecated, Initial UVL Parser, based on Clojure and instaparse_ **UVL-Parser**
- https://github.com/diverso-lab/uvl-diverso/ _Under development, Antlr4 Parser_ **Diverso Lab**

Usage of UVL:

- https://github.com/FeatureIDE/FeatureIDE _Feature modelling tool_
