package de.vill.main;

import de.vill.model.*;
import de.vill.model.building.VariableReference;
import de.vill.model.constraint.*;
import de.vill.model.expression.*;
import de.vill.util.Util;
import uvl.UVLJavaLexer;
import uvl.UVLJavaParser;

import de.vill.conversion.ConvertAggregateFunction;
import de.vill.conversion.ConvertFeatureCardinality;
import de.vill.conversion.ConvertGroupCardinality;
import de.vill.conversion.ConvertSMTLevel;
import de.vill.conversion.ConvertStringConstraints;
import de.vill.conversion.ConvertTypeLevel;
import de.vill.conversion.DropAggregateFunction;
import de.vill.conversion.DropFeatureCardinality;
import de.vill.conversion.DropGroupCardinality;
import de.vill.conversion.DropStringConstraints;
import de.vill.conversion.DropSMTLevel;
import de.vill.conversion.DropTypeLevel;
import de.vill.conversion.IConversionStrategy;
import de.vill.exception.ParseError;
import de.vill.exception.ParseErrorList;
import de.vill.util.Constants;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class UVLModelFactory {

    private final Map<LanguageLevel, Class<? extends IConversionStrategy>> conversionStrategiesDrop;
    private final Map<LanguageLevel, Class<? extends IConversionStrategy>> conversionStrategiesConvert;

    private final List<ParseError> errorList = new LinkedList<>();

    public UVLModelFactory() {
        this.conversionStrategiesDrop = new HashMap<>();
        this.conversionStrategiesDrop.put(LanguageLevel.GROUP_CARDINALITY, DropGroupCardinality.class);
        this.conversionStrategiesDrop.put(LanguageLevel.FEATURE_CARDINALITY, DropFeatureCardinality.class);
        this.conversionStrategiesDrop.put(LanguageLevel.AGGREGATE_FUNCTION, DropAggregateFunction.class);
        this.conversionStrategiesDrop.put(LanguageLevel.ARITHMETIC_LEVEL, DropSMTLevel.class);
        this.conversionStrategiesDrop.put(LanguageLevel.TYPE_LEVEL, DropTypeLevel.class);
        this.conversionStrategiesDrop.put(LanguageLevel.STRING_CONSTRAINTS, DropStringConstraints.class);
        this.conversionStrategiesConvert = new HashMap<>();
        this.conversionStrategiesConvert.put(LanguageLevel.GROUP_CARDINALITY, ConvertGroupCardinality.class);
        this.conversionStrategiesConvert.put(LanguageLevel.FEATURE_CARDINALITY, ConvertFeatureCardinality.class);
        this.conversionStrategiesConvert.put(LanguageLevel.AGGREGATE_FUNCTION, ConvertAggregateFunction.class);
        this.conversionStrategiesConvert.put(LanguageLevel.ARITHMETIC_LEVEL, ConvertSMTLevel.class);
        this.conversionStrategiesConvert.put(LanguageLevel.TYPE_LEVEL, ConvertTypeLevel.class);
        this.conversionStrategiesConvert.put(LanguageLevel.STRING_CONSTRAINTS, ConvertStringConstraints.class);
    }

    /**
     * This method parses an UVL model given a path to the UVL model. For imported submodels (if applicable) the directory of the UVL model is used
     * @param uvlModelPath path to uvl model
     * @return
     * @throws ParseError
     */
    public FeatureModel parse(Path uvlModelPath) throws ParseError {
        String content = Util.readFileContent(uvlModelPath);
        String projectRootForImports = uvlModelPath.getParent().toString();

        return parse(content, projectRootForImports);
    }

    /**
     * This method parses the given text and returns a {@link FeatureModel} if everything is fine or throws a {@link ParseError} if something went wrong.
     *
     * @param text A String that describes a feature model in UVL notation.
     * @param rootPath Path to the directory where all submodels are stored.
     * @return A {@link FeatureModel} based on the uvl text
     * @throws ParseError If there is an error during parsing or the construction of the feature model
     */
    public FeatureModel parse(String text, String rootPath) throws ParseError {
        FeatureModel featureModel = parseFeatureModelWithImports(text, rootPath, new HashMap<>());
        composeFeatureModelFromImports(featureModel);
        validateTypeLevelConstraints(featureModel);
        return featureModel;
    }

    /**
     * This method parses the given text and returns a {@link FeatureModel} if everything is fine or throws a {@link ParseError} if something went wrong.
     * It assumes that all the necessary submodels are in the current working directory.
     *
     * @param text A String that describes a feature model in UVL notation.
     * @return A {@link FeatureModel} based on the uvl text
     * @throws ParseError If there is an error during parsing or the construction of the feature model
     */
    public FeatureModel parse(String text) throws ParseError {
        return parse(text, System.getProperty("user.dir"));
    }

    public Constraint parseConstraint(String constraintString) throws ParseError {
        constraintString = constraintString.trim();
        UVLJavaLexer UVLJavaLexer = new UVLJavaLexer(CharStreams.fromString(constraintString));
        CommonTokenStream tokens = new CommonTokenStream(UVLJavaLexer);
        UVLJavaParser UVLJavaParser = new UVLJavaParser(tokens);
        UVLJavaParser.removeErrorListener(ConsoleErrorListener.INSTANCE);
        UVLJavaLexer.removeErrorListener(ConsoleErrorListener.INSTANCE);

        UVLJavaLexer.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                errorList.add(new ParseError(line, charPositionInLine, "failed to parse at line " + line + ":" + charPositionInLine + " due to: " + msg, e));
            }
        });
        UVLJavaParser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                errorList.add(new ParseError(line, charPositionInLine, "failed to parse at line " + line + ":" + charPositionInLine + " due to " + msg, e));
            }
        });

        UVLListener uvlListener = new UVLListener();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(uvlListener, UVLJavaParser.constraintLine());

        return uvlListener.getConstraint();
    }

    //TODO If the level set is not consistent e.g. remove SMT_LEVEL but the feature model has AGGREGATE_FUNCTION level? -> remove automatically all related constraints (auch in der BA schrieben (In conversion strats chaper diskutieren))

    /**
     * This method takes a {@link FeatureModel} and transforms it so that it does not use any of the specified
     * {@link LanguageLevel}. This method applies the conversion strategy on the featuremodel and on all
     * submodels that import the corresponding language level.
     * It does that by removing the concepts of the level without any conversion strategies. This means information
     * is lost and the configuration space of the feature model will most likely change. It can be used, if the actual
     * conversion strategies are not performant enough.
     *
     * @param featureModel A reference to the feature model which should be transformed. The method operates directly on this object, not on a clone!
     * @param levelsToDrop All levels that should be removed from the feature model.
     */
    public void dropLanguageLevel(FeatureModel featureModel, Set<LanguageLevel> levelsToDrop) {
        convertFeatureModel(featureModel, featureModel, levelsToDrop, conversionStrategiesDrop);
    }

    /**
     * This method takes a {@link FeatureModel} and transforms it so that it does not use any of the specified
     * {@link UVLModelFactory#dropLanguageLevel(FeatureModel, Set)}. This method applies the conversion strategy on the
     * featuremodel and on all submodels that import the corresponding language level.
     * It does that applying different conversion strategies, trying to keep as much information as possible. This means
     * that the conversion can take a long time and my not be feasible for large models. If so try to just drop the levels instead.
     *
     * @param featureModel    A reference to the feature model which should be transformed. The method operates directly on this object, not on a clone!
     * @param levelsToConvert All levels that should be removed from the feature model.
     */
    public void convertLanguageLevel(FeatureModel featureModel, Set<LanguageLevel> levelsToConvert) {
        convertFeatureModel(featureModel, featureModel, levelsToConvert, conversionStrategiesConvert);
    }

    /**
     * This method takes a {@link FeatureModel} and transforms it so that it only uses the specified {@link LanguageLevel}.
     * It just inverts the Set and calls {@link UVLModelFactory#dropLanguageLevel(FeatureModel, Set)}.
     *
     * @param featureModel           A reference to the feature model which should be transformed. The method operates directly on this object, not on a clone!
     * @param supportedLanguageLevel All levels that can stay in the feature model.
     */
    public void dropExceptAcceptedLanguageLevel(FeatureModel featureModel, Set<LanguageLevel> supportedLanguageLevel) {
        Set<LanguageLevel> allLevels = new HashSet<>(Arrays.asList(LanguageLevel.values()));
        allLevels.removeAll(supportedLanguageLevel);

        dropLanguageLevel(featureModel, allLevels);
    }

    /**
     * This method takes a {@link FeatureModel} and transforms it so that it only uses the specified {@link LanguageLevel}.
     * It just inverts the Set and calls {@link UVLModelFactory#convertLanguageLevel(FeatureModel, Set)}.
     *
     * @param featureModel           A reference to the feature model which should be transformed. The method operates directly on this object, not on a clone!
     * @param supportedLanguageLevel All levels that can stay in the feature model.
     */
    public void convertExceptAcceptedLanguageLevel(FeatureModel featureModel, Set<LanguageLevel> supportedLanguageLevel) {
        Set<LanguageLevel> allLevels = new HashSet<>(Arrays.asList(LanguageLevel.values()));
        allLevels.removeAll(supportedLanguageLevel);

        convertLanguageLevel(featureModel, allLevels);
    }

    /**
     * This method takes a {@link FeatureModel} converts all language levels higher than the specified {@link LanguageLevel}.
     * For instance, providing the SMT major level will remove all SMT minor and type levels.
     * @param featureModel Reference to feature model to be converted
     * @param supportedLanguageLevel Highest language level to be maintained
     */
    public void convertAllMoreComplexLanguageLevels(FeatureModel featureModel, LanguageLevel supportedLanguageLevel) {
        Set<LanguageLevel> levelsToConvert = new HashSet<>();

        for (LanguageLevel level : LanguageLevel.values()) {
            if (level.getValue() > supportedLanguageLevel.getValue()) {
                levelsToConvert.add(level);
            }
        }

        convertLanguageLevel(featureModel, levelsToConvert);

    }

    private void convertFeatureModel(FeatureModel rootFeatureModel, FeatureModel featureModel, Set<LanguageLevel> levelsToRemove, Map<LanguageLevel, Class<? extends IConversionStrategy>> conversionStrategies) {
        List<LanguageLevel> levelsToRemoveActually = getActualLanguageLevelsToRemoveInOrder(featureModel, levelsToRemove);
        while (!levelsToRemoveActually.isEmpty()) {
            LanguageLevel levelToDropNow = levelsToRemoveActually.get(0);
            levelsToRemoveActually.remove(0);
            try {
                IConversionStrategy conversionStrategy = conversionStrategies.get(levelToDropNow).getDeclaredConstructor().newInstance();
                conversionStrategy.convertFeatureModel(rootFeatureModel, featureModel);
                featureModel.getUsedLanguageLevels().removeAll(conversionStrategy.getLevelsToBeRemoved());
                featureModel.getUsedLanguageLevels().addAll(conversionStrategy.getTargetLevelsOfConversion());
                for (Import importLine : featureModel.getImports()) {
                    //only consider sub feature models from imports that are actually used
                    if (importLine.isReferenced()) {
                        convertFeatureModel(rootFeatureModel, importLine.getFeatureModel(), levelsToRemove, conversionStrategies);
                        featureModel.getUsedLanguageLevels().removeAll(conversionStrategy.getLevelsToBeRemoved());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private LanguageLevel getMaxLanguageLevel(Set<LanguageLevel> languageLevels) {
        LanguageLevel max = LanguageLevel.BOOLEAN_LEVEL;
        for (LanguageLevel languageLevel : languageLevels) {
            if (languageLevel.getValue() > max.getValue()) {
                max = languageLevel;
            }
        }
        return max;
    }

    /**
     * First Language Level in the list (index 0) should be removed first and so on. calculates based on a set levels
     * that should be removed which levels actually needs to be removed. E.g. if a major level should be removed, all
     * its corresponding minor levels must be removed too. Moreover, the levels must be removed in the correct order,
     * so the "highest" level must be removed first. Furthermore, levels that are not used by the featureModel must
     * not be removed.
     *
     * @param featureModel   The featureModel that does use language levels
     * @param levelsToRemove The levels that a user thinks should be removed.
     * @return a list with the language levels that actually need to be removed in the order of the list (first element of the list removed first)
     */
    private List<LanguageLevel> getActualLanguageLevelsToRemoveInOrder(FeatureModel featureModel, Set<LanguageLevel> levelsToRemove) {
        Set<LanguageLevel> levelsToRemoveClone = new HashSet<>(levelsToRemove);
        List<LanguageLevel> completeOrderedLevelsToRemove = new LinkedList<>();
        while (!levelsToRemoveClone.isEmpty()) {
            LanguageLevel highestLevel = getMaxLanguageLevel(levelsToRemoveClone);
            if (LanguageLevel.isMajorLevel(highestLevel)) {
                //highestLevel is major level
                int numberCorrespondingMinorLevels = highestLevel.getValue() + 1;
                List<LanguageLevel> correspondingMinorLevels = LanguageLevel.valueOf(numberCorrespondingMinorLevels);
                if (correspondingMinorLevels != null) {
                    correspondingMinorLevels.retainAll(featureModel.getUsedLanguageLevelsRecursively());
                    completeOrderedLevelsToRemove.addAll(correspondingMinorLevels);
                }
                completeOrderedLevelsToRemove.add(highestLevel);
                levelsToRemoveClone.remove(highestLevel);
            } else {
                //highestLevel is minor level
                if (featureModel.getUsedLanguageLevelsRecursively().contains(highestLevel)) {
                    completeOrderedLevelsToRemove.add(highestLevel);
                }
                levelsToRemoveClone.remove(highestLevel);
            }
        }
        //SAT-level can not be removed
        completeOrderedLevelsToRemove.remove(LanguageLevel.BOOLEAN_LEVEL);

        return completeOrderedLevelsToRemove;
    }

    private String getPath(String rootPath, Import referencedImport) {
        return rootPath + FileSystems.getDefault().getSeparator() + referencedImport.getNamespace().replace(".", FileSystems.getDefault().getSeparator()) + ".uvl";
    }

    private FeatureModel parseFeatureModelWithImports(String text, String rootPath, Map<String, Import> visitedImports) {
        //remove leading and trailing spaces (to be more robust)
        text = text.trim();
        UVLJavaLexer UVLJavaLexer = new UVLJavaLexer(CharStreams.fromString(text));
        CommonTokenStream tokens = new CommonTokenStream(UVLJavaLexer);
        UVLJavaParser UVLJavaParser = new UVLJavaParser(tokens);
        UVLJavaParser.removeErrorListener(ConsoleErrorListener.INSTANCE);
        UVLJavaLexer.removeErrorListener(ConsoleErrorListener.INSTANCE);

        UVLJavaLexer.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                errorList.add(new ParseError(line, charPositionInLine, "failed to parse at line " + line + ":" + charPositionInLine + " due to: " + msg, e));
                //throw new ParseError(line, charPositionInLine,"failed to parse at line " + line + ":" + charPositionInLine + " due to: " + msg, e);
            }
        });
        UVLJavaParser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                errorList.add(new ParseError(line, charPositionInLine, "failed to parse at line " + line + ":" + charPositionInLine + " due to " + msg, e));
                //throw new ParseError(line, charPositionInLine,"failed to parse at line " + line + ":" + charPositionInLine + " due to " + msg, e);
            }
        });


        UVLListener uvlListener = new UVLListener();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(uvlListener, UVLJavaParser.featureModel());
        FeatureModel featureModel = null;

        try {
            featureModel = uvlListener.getFeatureModel();
        } catch (ParseErrorList e) {
            errorList.addAll(e.getErrorList());
        }

        if (errorList.size() > 0) {
            ParseErrorList parseErrorList = new ParseErrorList("Multiple Errors occurred during parsing!");
            parseErrorList.getErrorList().addAll(errorList);
            throw parseErrorList;
        }

        //if featuremodel has not namespace and no root feature getNamespace returns null
        if(featureModel.getNamespace() != null) {
            visitedImports.put(featureModel.getNamespace(), null);

            for (Import importLine : featureModel.getImports()) {
                if (visitedImports.containsKey(importLine.getNamespace()) && visitedImports.get(importLine.getNamespace()) == null) {
                    throw new ParseError("Cyclic import detected! " + "The import of " + importLine.getNamespace() + " in " + featureModel.getNamespace() + " creates a cycle", importLine.getLineNumber());
                } else {
                    try {
                        String path = getPath(rootPath, importLine);
                        Path filePath = Paths.get(path);
                        String content = new String(Files.readAllBytes(filePath));
                        FeatureModel subModel = parseFeatureModelWithImports(content, filePath.getParent().toString(), visitedImports);
                        importLine.setFeatureModel(subModel);
                        subModel.getRootFeature().setRelatedImport(importLine);
                        visitedImports.put(importLine.getNamespace(), importLine);

                        //adjust namespaces of imported features
                        for (Map.Entry<String, Feature> entry : subModel.getFeatureMap().entrySet()) {
                            Feature feature = entry.getValue();
                            if (feature.getNameSpace().equals("")) {
                                feature.setNameSpace(importLine.getAlias());
                            } else {
                                feature.setNameSpace(importLine.getAlias() + "." + feature.getNameSpace());
                            }
                        }

                        //check if submodel is actually used
                        if (featureModel.getFeatureMap().containsKey(subModel.getRootFeature().getReferenceFromSpecificSubmodel(""))) {
                            importLine.setReferenced(true);
                        }
                        // if submodel is used add features
                        if (importLine.isReferenced()) {
                            for (Map.Entry<String, Feature> entry : subModel.getFeatureMap().entrySet()) {
                                Feature feature = entry.getValue();
                                if (!featureModel.getFeatureMap().containsKey(feature.getNameSpace() + "." + entry.getValue().getFeatureName())) {
                                    if (importLine.isReferenced()) {
                                        featureModel.getFeatureMap().put(feature.getNameSpace() + "." + entry.getValue().getFeatureName(), feature);
                                    }
                                }
                            }
                        }


                    } catch (IOException e) {
                        throw new ParseError("Could not resolve import: " + e.getMessage(), importLine.getLineNumber());
                    }
                }
            }
        }
        for (Constraint constraint : featureModel.getOwnConstraints()) {
            resolveImportPlaceholders(constraint, featureModel);
        }
        return featureModel;
    }

    private void resolveImportPlaceholders(Constraint constraint, FeatureModel featureModel) {
        if (constraint instanceof AndConstraint || constraint instanceof OrConstraint || constraint instanceof NotConstraint || constraint instanceof ImplicationConstraint || constraint instanceof ParenthesisConstraint || constraint instanceof EquivalenceConstraint) {
            for (Constraint subPart : constraint.getConstraintSubParts()) {
                resolveImportPlaceholders(subPart, featureModel);
            }
        }  else if (constraint instanceof ExpressionConstraint) {
            ExpressionConstraint expressionConstraint = (ExpressionConstraint) constraint;
            resolveImportPlaceholders(expressionConstraint.getLeft(), featureModel);
            resolveImportPlaceholders(expressionConstraint.getRight(), featureModel);
        } else if (constraint instanceof LiteralConstraint) {
            LiteralConstraint literalConstraint = (LiteralConstraint) constraint;
            if (literalConstraint.getReference() instanceof ImportedVariablePlaceholder) {
                ImportedVariablePlaceholder placeholder = (ImportedVariablePlaceholder) literalConstraint.getReference();
                literalConstraint.setReference(resolvePlaceholder(placeholder, featureModel));
            }
        }
    }

    private void resolveImportPlaceholders(Expression expression, FeatureModel featureModel) {
        if (expression instanceof BinaryExpression) {
            BinaryExpression binaryExpression = (BinaryExpression) expression;
            resolveImportPlaceholders(binaryExpression.getLeft(), featureModel);
            resolveImportPlaceholders(binaryExpression.getRight(), featureModel);
        } else if (expression instanceof ParenthesisExpression) {
            ParenthesisExpression parenthesisExpression = (ParenthesisExpression) expression;
            resolveImportPlaceholders(parenthesisExpression.getContent(), featureModel);
        } else if (expression instanceof LengthAggregateFunctionExpression) {
            LengthAggregateFunctionExpression lengthAggregateFunctionExpression = (LengthAggregateFunctionExpression) expression;
            if (lengthAggregateFunctionExpression.getReference() instanceof ImportedVariablePlaceholder) {
                ImportedVariablePlaceholder placeholder = (ImportedVariablePlaceholder) lengthAggregateFunctionExpression.getReference();
                lengthAggregateFunctionExpression.setReference(resolvePlaceholder(placeholder, featureModel));
            }
        } else if (expression instanceof LiteralExpression) {
            LiteralExpression literalExpression = (LiteralExpression) expression;
            if (literalExpression.getContent() instanceof ImportedVariablePlaceholder) {
                ImportedVariablePlaceholder placeholder = (ImportedVariablePlaceholder) literalExpression.getContent();
                literalExpression.setContent(resolvePlaceholder(placeholder, featureModel));
            }
        }
    }

    /**
     * Very whacky code currently
     * @param placeholder
     * @param featureModel
     * @return
     */
    private VariableReference resolvePlaceholder(ImportedVariablePlaceholder placeholder, FeatureModel featureModel) {
        boolean isCurrentPartAnImport;
        int currentIndex = 0;
        Import lastImport = placeholder.mainImport;
        List<String> relativeNamespaces = new ArrayList<>();
        do {
            if (currentIndex == placeholder.unidentifiedImportParts.size() - 1) { // Last part should never be an import
                break;
            }
            if (lastImport.getFeatureModel().getImports().isEmpty()) { // If current part has no further import we can stop looking
                break;
            } else {
                isCurrentPartAnImport = false;
                for (Import currentLevelImport : lastImport.getFeatureModel().getImports()) {
                    if (currentLevelImport.getAlias().equals(placeholder.unidentifiedImportParts.get(currentIndex))) { // next part is one of the available imports
                        isCurrentPartAnImport = true;
                        lastImport = currentLevelImport;
                        relativeNamespaces.add(currentLevelImport.getAlias());
                    }
                }
            }
            currentIndex++;
        } while(isCurrentPartAnImport);

        if (currentIndex == placeholder.unidentifiedImportParts.size() - 1) { // Feature
            Feature feat = lastImport.getFeatureModel().getFeatureMap().get(placeholder.unidentifiedImportParts.get(currentIndex));
            if (!relativeNamespaces.isEmpty()) {
                lastImport.setRelativeImportPath(String.join(".", relativeNamespaces));
            }
            feat.setRelatedImport(lastImport);
            return feat;
        } else if (currentIndex == placeholder.unidentifiedImportParts.size() - 2) { // Attribute
            Feature feat = lastImport.getFeatureModel().getFeatureMap().get(placeholder.unidentifiedImportParts.get(currentIndex));
            if (!relativeNamespaces.isEmpty()) {
                lastImport.setRelativeImportPath(String.join(".", relativeNamespaces));
            }
            feat.setRelatedImport(lastImport);
            return feat.getAttributes().get(placeholder.unidentifiedImportParts.get(currentIndex + 1));
        } else { // Should never happen for a valid reference
            return null;
        }
    }

    private boolean isPlaceholderFeature(String fullReference, FeatureModel featureModel) {
        return featureModel.getFeatureMap().containsKey(fullReference);
    }

    private boolean isPlaceholderAttribute(String fullReference, FeatureModel featureModel) {
        return featureModel.getFeatureMap().containsKey(fullReference.substring(0, fullReference.lastIndexOf(".")));
    }

    private void composeFeatureModelFromImports(FeatureModel featureModel) {
        for (Map.Entry<String, Feature> entry : featureModel.getFeatureMap().entrySet()) {
            if (entry.getValue().isSubmodelRoot()) {
                Feature featureInMainFeatureTree = entry.getValue();
                Import relatedImport = featureInMainFeatureTree.getRelatedImport();
                Feature featureInSubmodelFeatureTree = relatedImport.getFeatureModel().getRootFeature();
                featureInMainFeatureTree.getChildren().addAll(featureInSubmodelFeatureTree.getChildren());
                for (Group group : featureInMainFeatureTree.getChildren()) {
                    group.setParentFeature(featureInMainFeatureTree);
                }
                featureInMainFeatureTree.getAttributes().putAll(featureInSubmodelFeatureTree.getAttributes());
                relatedImport.getFeatureModel().setRootFeature(featureInMainFeatureTree);
            }
        }
    }

    private List<FeatureModel> createSubModelList(FeatureModel featureModel) {
        List<FeatureModel> subModelList = new LinkedList<FeatureModel>();
        for (Import importLine : featureModel.getImports()) {
            subModelList.add(importLine.getFeatureModel());
            subModelList.addAll(createSubModelList(importLine.getFeatureModel()));
        }
        return subModelList;
    }


    private void validateTypeLevelConstraints(final FeatureModel featureModel) {
        final List<Constraint> constraints = featureModel.getOwnConstraints();
        for (final Constraint constraint: constraints) {
            if (!validateTypeLevelConstraint(constraint)) {
                throw new ParseError("Invalid Constraint in line - " + constraint.getLineNumber());
            }
        }
    }

    private boolean validateTypeLevelConstraint(final Constraint constraint) {
        boolean result = true;
        if (constraint instanceof ExpressionConstraint) {
            String leftReturnType = ((ExpressionConstraint) constraint).getLeft().getReturnType();
            String rightReturnType = ((ExpressionConstraint) constraint).getRight().getReturnType();

            if (!(leftReturnType.equalsIgnoreCase(Constants.TRUE) || rightReturnType.equalsIgnoreCase(Constants.TRUE))) {
                // if not attribute constraint
                result = result && ((ExpressionConstraint) constraint).getLeft().getReturnType().equalsIgnoreCase(((ExpressionConstraint) constraint).getRight().getReturnType());
            }
            if (!result) {
                return false;
            }
            for (final Expression expr: ((ExpressionConstraint) constraint).getExpressionSubParts()) {
                result = result && validateTypeLevelExpression(expr);
            }
        }

        for (final Constraint subCons: constraint.getConstraintSubParts()) {
            result = result && validateTypeLevelConstraint(subCons);
        }

        return result;
    }

    private boolean validateTypeLevelExpression(final Expression expression) {
        final String initial = expression.getReturnType();
        boolean result = true;

        for (final Expression expr: expression.getExpressionSubParts()) {
            result = result && validateTypeLevelExpression(expr) && initial.equalsIgnoreCase(expr.getReturnType());
        }

        return result;
    }
}
