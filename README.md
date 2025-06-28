# UVL - Universal Variability Language Java Feature Model Metamodel

This is a small default library used to manipulate UVL metamodels in JAVA. It uses the [UVL-Grammar](https://github.com/Universal-Variability-Language/) to parse the a given metamodel. If parsed the model can be modified in JAVA. It is possible to also convert the models to different levels.

**How to use UVL** can be found in this paper
[![DOI](https://img.shields.io/badge/DOI-10.1016%2Fj.jss.2024.112326-blue)](https://doi.org/10.1016/j.jss.2024.112326)

## ✨ Key Features

- full support of UVL-Syntax (nampaces, feature hircharcies and constraints)
- convert between different language levels
- direct access to feature model components like features, groups and attributes

## ⚙️ Setup

To clone this repository **with all submodules**, use:

```bash
git clone --recurse-submodules https://github.com/Universal-Variability-Language/java-fm-metamodel.git
```

If you have already cloned the repository **without** submodules, you can initialize and update them afterwards with:

```bash
git submodule update --init --recursive
```

This ensures that all required submodules (e.g., test resources) are available and the project will build and test correctly

## 💡 Usage

### 📦 Getting Started

First, **clone this repository** to your local machine as explained in `⚙️ Setup`.
To use the Java-fm-metamodel, make sure you are in the project directory before running any further commands. Build the project with [Maven](https://maven.apache.org/):

```bash
mvn clean compile
```

This will compile all sources and ensure all dependencies (including submodules) are available.  
Now you can execute code for manipulating UVL feature models as shown in the `Examples` below

### 🧩 Using as a Maven Dependency

You can also use the Java-fm-metamodel as a dependency in your own Maven project.  
Add the following to your `pom.xml`:

```xml
<dependency>
  <groupId>io.github.universal-variability-language</groupId>
  <artifactId>fm-metamodel</artifactId>
  <version>1.1</version>
</dependency>
```

Make sure that the version matches the latest release.

---

Now you can use the classes from this library in your own Java code!

## Examples

Some usage examples that show how to use the acquired UVLModel object can be found in [src/main/java/de/vill/main/Example.java](https://github.com/Universal-Variability-Language/java-fm-metamodel/blob/main/src/main/java/de/vill/main/Example.java)

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

## 🧪 Running Tests

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
