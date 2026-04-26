package de.vill.main;

import de.vill.model.*;
import de.vill.model.building.FeatureModelBuilder;
import de.vill.model.building.ParsingUtilities;
import de.vill.model.building.VariableReference;
import uvl.UVLJavaParser;
import uvl.UVLJavaParserBaseListener;

import de.vill.exception.ErrorCategory;
import de.vill.exception.ErrorField;
import de.vill.exception.ErrorReport;
import de.vill.exception.ParseError;
import de.vill.exception.ParseErrorList;
import de.vill.model.constraint.AndConstraint;
import de.vill.model.constraint.Constraint;
import de.vill.model.constraint.EqualEquationConstraint;
import de.vill.model.constraint.EquivalenceConstraint;
import de.vill.model.constraint.GreaterEqualsEquationConstraint;
import de.vill.model.constraint.GreaterEquationConstraint;
import de.vill.model.constraint.ImplicationConstraint;
import de.vill.model.constraint.LiteralConstraint;
import de.vill.model.constraint.LowerEqualsEquationConstraint;
import de.vill.model.constraint.LowerEquationConstraint;
import de.vill.model.constraint.NotConstraint;
import de.vill.model.constraint.NotEqualsEquationConstraint;
import de.vill.model.constraint.OrConstraint;
import de.vill.model.constraint.ParenthesisConstraint;
import de.vill.model.expression.AddExpression;
import de.vill.model.expression.AggregateFunctionExpression;
import de.vill.model.expression.AvgAggregateFunctionExpression;
import de.vill.model.expression.DivExpression;
import de.vill.model.expression.Expression;
import de.vill.model.expression.LengthAggregateFunctionExpression;
import de.vill.model.expression.LiteralExpression;
import de.vill.model.expression.MulExpression;
import de.vill.model.expression.NumberExpression;
import de.vill.model.expression.ParenthesisExpression;
import de.vill.model.expression.StringExpression;
import de.vill.model.expression.SubExpression;
import de.vill.model.expression.SumAggregateFunctionExpression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import org.antlr.v4.runtime.Token;

public class UVLListener extends UVLJavaParserBaseListener {
    public FeatureModelBuilder fmBuilder = new FeatureModelBuilder();
    private Set<LanguageLevel> importedLanguageLevels = new HashSet<>(Arrays.asList(LanguageLevel.BOOLEAN_LEVEL));
    private Stack<Feature> featureStack = new Stack<>();
    private Stack<Group> groupStack = new Stack<>();

    private final Map<String, Feature> importedFeatures = new HashMap<>();
    private final Map<String, Integer> featureLineNumbers = new HashMap<>();

    private Stack<Constraint> constraintStack = new Stack<>();

    private Stack<Expression> expressionStack = new Stack<>();

    private Stack<Map<String, Attribute<?>>> attributeStack = new Stack<>();

    private List<ParseError> errorList = new LinkedList<>();

    private int skippedFeatureDepth = 0;

    @Override
    public void enterIncludes(UVLJavaParser.IncludesContext ctx) {
        fmBuilder.getFeatureModel().setExplicitLanguageLevels(true);
    }

    @Override
    public void enterFeatureModel(UVLJavaParser.FeatureModelContext ctx) {
        super.enterFeatureModel(ctx);
    }

    @Override
    public void exitIncludeLine(UVLJavaParser.IncludeLineContext ctx) {
        String[] levels = ctx.languageLevel().getText().split("\\.");
        if (levels.length == 1) {
            LanguageLevel majorLevel = LanguageLevel.getLevelByName(levels[0]);
            importedLanguageLevels.add(majorLevel);
        } else if (levels.length == 2) {
            LanguageLevel majorLevel = LanguageLevel.getLevelByName(levels[0]);
            List<LanguageLevel> minorLevels;
            if (levels[1].equals("*")) {
                minorLevels = LanguageLevel.valueOf(majorLevel.getValue() + 1);
            } else {
                minorLevels = new LinkedList<>();
                minorLevels.add(LanguageLevel.getLevelByName(levels[1]));
            }
            importedLanguageLevels.add(majorLevel);
            for (LanguageLevel minorLevel : minorLevels) {
                if (minorLevel.getValue() - 1 != majorLevel.getValue()) {
                    throw new ParseError("Minor language level " + minorLevel.getName() + " does not correspond to major language level " + majorLevel + " but to " + LanguageLevel.valueOf(minorLevel.getValue() - 1));
                }
                importedLanguageLevels.add(minorLevel);
            }
        } else {
            int line = ctx.getStart().getLine();
            int charPos = ctx.getStart().getCharPositionInLine();
            errorList.add(new ParseError(new ErrorReport.Builder(ErrorCategory.CONTEXT,
                    "Invalid language level import: '" + ctx.languageLevel().getText() + "'")
                    .line(line).charPosition(charPos)
                    .field(ErrorField.LANGUAGE_LEVEL)
                    .reference(ctx.languageLevel().getText())
                    .cause("Invalid language level import.")
                    .hint("Use a valid language level format.")
                    .build()));
        }
    }

    @Override
    public void exitNamespace(UVLJavaParser.NamespaceContext ctx) {
        fmBuilder.setNamespace(ctx.reference().getText().replace("\"", ""));
    }

    @Override
    public void exitImportLine(UVLJavaParser.ImportLineContext ctx) {
        Import importLine = ParsingUtilities.parseImport(ctx.ns.getText(), ctx.alias != null ? ctx.alias.getText() : null);
        Token t = ctx.getStart();
        int line = t.getLine();
        importLine.setLineNumber(line);
        fmBuilder.addImport(importLine);
    }

    @Override
    public void enterFeatures(UVLJavaParser.FeaturesContext ctx) {
        groupStack.push(new Group(Group.GroupType.MANDATORY));
    }

    @Override
    public void exitFeatures(UVLJavaParser.FeaturesContext ctx) {
        Group group = groupStack.pop();
        Feature feature = group.getFeatures().get(0);
        fmBuilder.setRootFeature(feature);
        feature.setParentGroup(null);
    }

    @Override
    public void enterGroupSpec(UVLJavaParser.GroupSpecContext ctx) {

    }

    @Override
    public void enterOrGroup(UVLJavaParser.OrGroupContext ctx) {
        Group group = new Group(Group.GroupType.OR);
        Feature feature = featureStack.peek();
        feature.addChildren(group);
        group.setParentFeature(feature);
        groupStack.push(group);
    }

    @Override
    public void exitOrGroup(UVLJavaParser.OrGroupContext ctx) {
        groupStack.pop();
    }

    @Override
    public void enterAlternativeGroup(UVLJavaParser.AlternativeGroupContext ctx) {
        Group group = new Group(Group.GroupType.ALTERNATIVE);
        Feature feature = featureStack.peek();
        feature.addChildren(group);
        group.setParentFeature(feature);
        groupStack.push(group);
    }

    @Override
    public void exitAlternativeGroup(UVLJavaParser.AlternativeGroupContext ctx) {
        groupStack.pop();
    }

    @Override
    public void enterOptionalGroup(UVLJavaParser.OptionalGroupContext ctx) {
        Group group = new Group(Group.GroupType.OPTIONAL);
        Feature feature = featureStack.peek();
        feature.addChildren(group);
        group.setParentFeature(feature);
        groupStack.push(group);
    }

    @Override
    public void exitOptionalGroup(UVLJavaParser.OptionalGroupContext ctx) {
        groupStack.pop();
    }

    @Override
    public void enterMandatoryGroup(UVLJavaParser.MandatoryGroupContext ctx) {
        Group group = new Group(Group.GroupType.MANDATORY);
        Feature feature = featureStack.peek();
        feature.addChildren(group);
        group.setParentFeature(feature);
        groupStack.push(group);
    }

    @Override
    public void exitMandatoryGroup(UVLJavaParser.MandatoryGroupContext ctx) {
        groupStack.pop();
    }

    @Override
    public void enterCardinalityGroup(UVLJavaParser.CardinalityGroupContext ctx) {
        Group group = new Group(Group.GroupType.GROUP_CARDINALITY);

        group.setCardinality(ParsingUtilities.parseCardinality(ctx.CARDINALITY().getText()));

        Feature feature = featureStack.peek();
        feature.addChildren(group);
        group.setParentFeature(feature);
        groupStack.push(group);

        fmBuilder.addLanguageLevel(LanguageLevel.GROUP_CARDINALITY);
    }

    @Override
    public void exitCardinalityGroup(UVLJavaParser.CardinalityGroupContext ctx) {
        groupStack.pop();
    }

    private static final Set<String> GROUP_KEYWORDS = new HashSet<>(Arrays.asList(
            "mandatory", "optional", "alternative", "or"
    ));

    @Override
    public void enterFeature(UVLJavaParser.FeatureContext ctx) {
        String featureReference = ctx.reference().getText().replace("\"", "");
        int line = ctx.getStart().getLine();
        int charPos = ctx.getStart().getCharPositionInLine();

        if (GROUP_KEYWORDS.contains(featureReference.toLowerCase())) {
            errorList.add(new ParseError(new ErrorReport.Builder(ErrorCategory.SYNTAX,
                    "'" + featureReference + "' is a reserved group keyword and cannot be used as a feature name")
                    .line(line).charPosition(charPos)
                    .field(ErrorField.FEATURE)
                    .reference(featureReference)
                    .cause("The name '" + featureReference + "' is a reserved group type keyword.")
                    .hint("Check if the indentation is correct. Group types must be indented under a parent feature.")
                    .build()));
            skippedFeatureDepth = 1;
            return;
        }

        Feature feature = ParsingUtilities.parseFeatureInitialization(featureReference, fmBuilder.getFeatureModel().getImports());
        if (feature == null) {
            errorList.add(new ParseError(new ErrorReport.Builder(ErrorCategory.CONTEXT,
                    "Feature '" + featureReference + "' is referenced as imported, but no matching import exists")
                    .line(line).charPosition(charPos)
                    .field(ErrorField.IMPORT)
                    .reference(featureReference)
                    .cause("The feature name suggests it comes from an imported submodel, but the import was not declared.")
                    .hint("Add the corresponding import in the 'imports' section or correct the feature name.")
                    .build()));
            skippedFeatureDepth = 1;
            return;
        } else if (importedFeatures.containsKey(featureReference)) {
            int originalLine = featureLineNumbers.getOrDefault(featureReference, 0);
            errorList.add(new ParseError(new ErrorReport.Builder(ErrorCategory.CONTEXT,
                    "Duplicate feature name: '" + featureReference + "'")
                    .line(line).charPosition(charPos)
                    .field(ErrorField.FEATURE)
                    .reference(featureReference)
                    .cause("A feature with the name '" + featureReference + "' already exists in the feature tree (first defined at line " + originalLine + ").")
                    .hint("Rename one of the duplicate features to make names unique.")
                    .build()));
            skippedFeatureDepth = 1;
            return;
        }
        importedFeatures.put(featureReference, feature);
        featureLineNumbers.put(featureReference, line);
        featureStack.push(feature);
        Group parentGroup = groupStack.peek();
        fmBuilder.addFeature(feature, parentGroup);
    }

    @Override
    public void exitFeature(UVLJavaParser.FeatureContext ctx) {
        if (skippedFeatureDepth > 0) {
            skippedFeatureDepth--;
            return;
        }
        featureStack.pop();
    }

    @Override
    public void enterFeatureType(final UVLJavaParser.FeatureTypeContext ctx) {
        final Feature feature = this.featureStack.peek();
        feature.setFeatureType(FeatureType.fromString(ctx.getText().toLowerCase()));
        fmBuilder.addLanguageLevel(LanguageLevel.TYPE_LEVEL);
    }

    @Override
    public void exitFeatureCardinality(UVLJavaParser.FeatureCardinalityContext ctx) {
        Cardinality cardinality = ParsingUtilities.parseCardinality(ctx.CARDINALITY().getText());
        Feature feature = featureStack.peek();
        feature.setCardinality(cardinality);
        fmBuilder.addLanguageLevel(LanguageLevel.ARITHMETIC_LEVEL);
        fmBuilder.addLanguageLevel(LanguageLevel.FEATURE_CARDINALITY);
    }

    @Override
    public void enterAttributes(UVLJavaParser.AttributesContext ctx) {
        attributeStack.push(new HashMap<>());
    }

    @Override
    public void exitAttributes(UVLJavaParser.AttributesContext ctx) {
        if (attributeStack.size() == 1) {
            Feature feature = featureStack.peek();
            feature.getAttributes().putAll(attributeStack.pop());
        }
    }


    @Override
    public void exitValueAttribute(UVLJavaParser.ValueAttributeContext ctx) {
        String attributeName = ctx.key().getText().replace("\"", "");
        Feature feature = featureStack.peek();
        if (ctx.value() == null) {
            Attribute<Boolean> attribute = new Attribute<>(attributeName, true, feature);
            attributeStack.peek().put(attributeName, attribute);
        } else if (ctx.value().BOOLEAN() != null) {
            Attribute<Boolean> attribute = new Attribute<>(attributeName, Boolean.parseBoolean(ctx.value().getText().replace("'", "")), feature);
            attributeStack.peek().put(attributeName, attribute);
        } else if (ctx.value().INTEGER() != null) {
            Attribute<Long> attribute = new Attribute<>(attributeName, Long.parseLong(ctx.value().getText().replace("'", "")), feature);
            attributeStack.peek().put(attributeName, attribute);
        } else if (ctx.value().FLOAT() != null) {
            Attribute<Double> attribute = new Attribute<>(attributeName, Double.parseDouble(ctx.value().getText().replace("'", "")), feature);
            attributeStack.peek().put(attributeName, attribute);
        } else if (ctx.value().STRING() != null) {
            Attribute<String> attribute = new Attribute<>(attributeName, ctx.value().getText().replace("'", ""), feature);
            attributeStack.peek().put(attributeName, attribute);
        } else if (ctx.value().vector() != null) {
            String vectorString = ctx.value().getText();
            vectorString = vectorString.substring(1, vectorString.length() - 1);
            Attribute<List<String>> attribute = new Attribute<>(attributeName, Arrays.asList(vectorString.split(",")), feature);
            attributeStack.peek().put(attributeName, attribute);
        } else if (ctx.value().attributes() != null) {
            Map<String, Attribute<?>> attributes = attributeStack.pop();
            Attribute<Map<String, Attribute<?>>> attribute = new Attribute<>(attributeName, attributes, feature);
            attributeStack.peek().put(attributeName, attribute);
        } else {
            int line = ctx.getStart().getLine();
            int charPos = ctx.getStart().getCharPositionInLine();
            errorList.add(new ParseError(new ErrorReport.Builder(ErrorCategory.CONTEXT,
                    "Unsupported attribute value type: '" + ctx.value().getText() + "'")
                    .line(line).charPosition(charPos)
                    .field(ErrorField.ATTRIBUTE)
                    .reference(attributeName)
                    .cause("The value '" + ctx.value().getText() + "' does not match any supported attribute type (Boolean, Integer, Float, String, Vector, Attributes).")
                    .hint("Use a supported value type for the attribute.")
                    .build()));
        }
    }

    @Override
    public void exitSingleConstraintAttribute(UVLJavaParser.SingleConstraintAttributeContext ctx) {
    	attributeStack.peek().put("constraint",
    			new Attribute<>("constraint", constraintStack.pop(), featureStack.peek()));
    }

    @Override
    public void exitListConstraintAttribute(UVLJavaParser.ListConstraintAttributeContext ctx) {
        List<Constraint> constraintList = new ArrayList<>(constraintStack.size());
        while (!constraintStack.empty()) {
            constraintList.add(constraintStack.pop());
        }
        attributeStack.peek().put("constraints",
        		new Attribute<>("constraints", constraintList, featureStack.peek()));
    }


    @Override
    public void exitLiteralConstraint(UVLJavaParser.LiteralConstraintContext ctx) {
        String referenceName = ctx.reference().getText();
        int line = ctx.getStart().getLine();
        int charPos = ctx.getStart().getCharPositionInLine();
        VariableReference reference = ParsingUtilities.resolveReference(referenceName, fmBuilder.getFeatureModel());
        if (reference == null) {
            errorList.add(new ParseError(new ErrorReport.Builder(ErrorCategory.CONTEXT,
                    "Reference '" + referenceName + "' in constraint could not be resolved")
                    .line(line).charPosition(charPos)
                    .field(ErrorField.CONSTRAINT)
                    .reference(referenceName)
                    .cause("The feature or attribute '" + referenceName + "' is used in a constraint but does not exist in the feature tree.")
                    .hint("Check if the feature name is spelled correctly or add it to the feature tree.")
                    .build()));
            reference = new Feature(referenceName);
        }
        LiteralConstraint constraint = new LiteralConstraint(reference);

        fmBuilder.getFeatureModel().getLiteralConstraints().add(constraint);
        constraintStack.push(constraint);
        constraint.setLineNumber(line);
    }

    @Override
    public void exitParenthesisConstraint(UVLJavaParser.ParenthesisConstraintContext ctx) {
        Constraint constraint = new ParenthesisConstraint(constraintStack.pop());
        constraintStack.push(constraint);
        Token t = ctx.getStart();
        int line = t.getLine();
        constraint.setLineNumber(line);
    }

    @Override
    public void exitNotConstraint(UVLJavaParser.NotConstraintContext ctx) {
        Constraint constraint = new NotConstraint(constraintStack.pop());
        constraintStack.push(constraint);
        Token t = ctx.getStart();
        int line = t.getLine();
        constraint.setLineNumber(line);
    }

    @Override
    public void exitAndConstraint(UVLJavaParser.AndConstraintContext ctx) {
        Constraint rightConstraint = constraintStack.pop();
        Constraint leftConstraint = constraintStack.pop();
        Constraint constraint = new AndConstraint(leftConstraint, rightConstraint);
        constraintStack.push(constraint);
        Token t = ctx.getStart();
        int line = t.getLine();
        constraint.setLineNumber(line);
    }

    @Override
    public void exitOrConstraint(UVLJavaParser.OrConstraintContext ctx) {
        Constraint rightConstraint = constraintStack.pop();
        Constraint leftConstraint = constraintStack.pop();
        Constraint constraint = new OrConstraint(leftConstraint, rightConstraint);
        constraintStack.push(constraint);
        Token t = ctx.getStart();
        int line = t.getLine();
        constraint.setLineNumber(line);
    }

    @Override
    public void exitImplicationConstraint(UVLJavaParser.ImplicationConstraintContext ctx) {
        Constraint rightConstraint = constraintStack.pop();
        Constraint leftConstraint = constraintStack.pop();
        Constraint constraint = new ImplicationConstraint(leftConstraint, rightConstraint);
        constraintStack.push(constraint);
        Token t = ctx.getStart();
        int line = t.getLine();
        constraint.setLineNumber(line);
    }

    @Override
    public void exitEquivalenceConstraint(UVLJavaParser.EquivalenceConstraintContext ctx) {
        Constraint rightConstraint = constraintStack.pop();
        Constraint leftConstraint = constraintStack.pop();
        Constraint constraint = new EquivalenceConstraint(leftConstraint, rightConstraint);
        constraintStack.push(constraint);
        Token t = ctx.getStart();
        int line = t.getLine();
        constraint.setLineNumber(line);
    }

    @Override
    public void exitEqualEquation(UVLJavaParser.EqualEquationContext ctx) {
        Expression right = expressionStack.pop();
        Expression left = expressionStack.pop();
        Constraint constraint = new EqualEquationConstraint(left, right);
        constraintStack.push(constraint);
        Token t = ctx.getStart();
        int line = t.getLine();
        constraint.setLineNumber(line);
        fmBuilder.addLanguageLevel(LanguageLevel.ARITHMETIC_LEVEL);
    }

    @Override
    public void exitLowerEquation(UVLJavaParser.LowerEquationContext ctx) {
        Expression right = expressionStack.pop();
        Expression left = expressionStack.pop();
        Constraint constraint = new LowerEquationConstraint(left, right);
        constraintStack.push(constraint);
        Token t = ctx.getStart();
        int line = t.getLine();
        constraint.setLineNumber(line);
        fmBuilder.addLanguageLevel(LanguageLevel.ARITHMETIC_LEVEL);
    }

    @Override
    public void exitGreaterEquation(UVLJavaParser.GreaterEquationContext ctx) {
        Expression right = expressionStack.pop();
        Expression left = expressionStack.pop();
        Constraint constraint = new GreaterEquationConstraint(left, right);
        constraintStack.push(constraint);
        Token t = ctx.getStart();
        int line = t.getLine();
        constraint.setLineNumber(line);
        fmBuilder.addLanguageLevel(LanguageLevel.ARITHMETIC_LEVEL);
    }

    @Override
    public void exitLowerEqualsEquation(UVLJavaParser.LowerEqualsEquationContext ctx) {
        Expression right = expressionStack.pop();
        Expression left = expressionStack.pop();
        Constraint constraint = new LowerEqualsEquationConstraint(left, right);
        constraintStack.push(constraint);
        Token t = ctx.getStart();
        int line = t.getLine();
        constraint.setLineNumber(line);
        fmBuilder.addLanguageLevel(LanguageLevel.ARITHMETIC_LEVEL);
    }

    @Override
    public void exitGreaterEqualsEquation(UVLJavaParser.GreaterEqualsEquationContext ctx) {
        Expression right = expressionStack.pop();
        Expression left = expressionStack.pop();
        Constraint constraint = new GreaterEqualsEquationConstraint(left, right);
        constraintStack.push(constraint);
        Token t = ctx.getStart();
        int line = t.getLine();
        constraint.setLineNumber(line);
        fmBuilder.addLanguageLevel(LanguageLevel.ARITHMETIC_LEVEL);
    }

    @Override
    public void exitNotEqualsEquation(UVLJavaParser.NotEqualsEquationContext ctx) {
        Expression right = expressionStack.pop();
        Expression left = expressionStack.pop();
        Constraint constraint = new NotEqualsEquationConstraint(left, right);
        constraintStack.push(constraint);
        Token t = ctx.getStart();
        int line = t.getLine();
        constraint.setLineNumber(line);
        fmBuilder.addLanguageLevel(LanguageLevel.ARITHMETIC_LEVEL);
    }

    @Override
    public void exitBracketExpression(UVLJavaParser.BracketExpressionContext ctx) {
        ParenthesisExpression expression = new ParenthesisExpression(expressionStack.pop());
        expressionStack.push(expression);
        Token t = ctx.getStart();
        int line = t.getLine();
        expression.setLineNumber(line);
    }

    @Override
    public void exitIntegerLiteralExpression(UVLJavaParser.IntegerLiteralExpressionContext ctx) {
        Expression expression = new NumberExpression(Integer.parseInt(ctx.INTEGER().getText()));
        expressionStack.push(expression);
        Token t = ctx.getStart();
        int line = t.getLine();
        expression.setLineNumber(line);
    }

    @Override
    public void exitStringLiteralExpression(UVLJavaParser.StringLiteralExpressionContext ctx) {
        Expression expression = new StringExpression(ctx.STRING().getText().replace("'", ""));
        expressionStack.push(expression);
        if (expressionStack.peek() instanceof LiteralExpression) {
            fmBuilder.addLanguageLevel(LanguageLevel.TYPE_LEVEL);
            fmBuilder.addLanguageLevel(LanguageLevel.STRING_CONSTRAINTS);
        }
        Token t = ctx.getStart();
        int line = t.getLine();
        expression.setLineNumber(line);
    }

    @Override
    public void exitFloatLiteralExpression(UVLJavaParser.FloatLiteralExpressionContext ctx) {
        Expression expression = new NumberExpression(Double.parseDouble(ctx.FLOAT().getText()));
        expressionStack.push(expression);
        Token t = ctx.getStart();
        int line = t.getLine();
        expression.setLineNumber(line);
    }

    @Override
    public void exitLiteralExpression(UVLJavaParser.LiteralExpressionContext ctx) {
        String referenceName = ctx.reference().getText().replace("\"", "");
        int line = ctx.getStart().getLine();
        int charPos = ctx.getStart().getCharPositionInLine();
        VariableReference variable = ParsingUtilities.resolveReference(referenceName, fmBuilder.getFeatureModel());
        if (variable == null) {
            errorList.add(new ParseError(new ErrorReport.Builder(ErrorCategory.CONTEXT,
                    "Variable '" + referenceName + "' in expression could not be resolved")
                    .line(line).charPosition(charPos)
                    .field(ErrorField.EXPRESSION)
                    .reference(referenceName)
                    .cause("The feature or attribute '" + referenceName + "' is used in an expression but does not exist in the feature tree.")
                    .hint("Check if the variable name is spelled correctly or add it to the feature tree.")
                    .build()));
            variable = new Feature(referenceName);
        }
        LiteralExpression expression = new LiteralExpression(variable);
        if (variable instanceof Attribute<?>) {
            fmBuilder.addLanguageLevel(LanguageLevel.ARITHMETIC_LEVEL);
        }
        expressionStack.push(expression);
        fmBuilder.getFeatureModel().getLiteralExpressions().add(expression);
        expression.setLineNumber(line);
    }

    @Override
    public void exitAddExpression(UVLJavaParser.AddExpressionContext ctx) {
        Expression right = expressionStack.pop();
        Expression left = expressionStack.pop();
        Expression expression = new AddExpression(left, right);
        expressionStack.push(expression);
        Token t = ctx.getStart();
        int line = t.getLine();
        expression.setLineNumber(line);
    }

    @Override
    public void exitSubExpression(UVLJavaParser.SubExpressionContext ctx) {
        Expression right = expressionStack.pop();
        Expression left = expressionStack.pop();
        Expression expression = new SubExpression(left, right);
        expressionStack.push(expression);
        Token t = ctx.getStart();
        int line = t.getLine();
        expression.setLineNumber(line);
    }

    @Override
    public void exitMulExpression(UVLJavaParser.MulExpressionContext ctx) {
        Expression right = expressionStack.pop();
        Expression left = expressionStack.pop();
        Expression expression = new MulExpression(left, right);
        expressionStack.push(expression);
        Token t = ctx.getStart();
        int line = t.getLine();
        expression.setLineNumber(line);
    }

    @Override
    public void exitDivExpression(UVLJavaParser.DivExpressionContext ctx) {
        Expression right = expressionStack.pop();
        Expression left = expressionStack.pop();
        Expression expression = new DivExpression(left, right);
        expressionStack.push(expression);
        Token t = ctx.getStart();
        int line = t.getLine();
        expression.setLineNumber(line);
    }

    @Override
    public void exitSumAggregateFunction(UVLJavaParser.SumAggregateFunctionContext ctx) {
        fmBuilder.addLanguageLevel(LanguageLevel.ARITHMETIC_LEVEL);
        fmBuilder.addLanguageLevel(LanguageLevel.AGGREGATE_FUNCTION);
        int line = ctx.getStart().getLine();
        int charPos = ctx.getStart().getCharPositionInLine();
        AggregateFunctionExpression expression;
        if (ctx.reference().size() > 1) {
            VariableReference rootFeature = ParsingUtilities.resolveReference(ctx.reference().get(1).getText(), fmBuilder.getFeatureModel());
            GlobalAttribute attribute = ParsingUtilities.getGlobalAttribute(ctx.reference().get(0).getText(), fmBuilder.getFeatureModel());
            boolean smellyInput = false;
            if (attribute.getType() == null) {
                errorList.add(new ParseError(new ErrorReport.Builder(ErrorCategory.CONTEXT,
                        "Attribute '" + attribute.getIdentifier() + "' does not exist in the feature model")
                        .line(line).charPosition(charPos)
                        .field(ErrorField.ATTRIBUTE)
                        .reference(attribute.getIdentifier())
                        .cause("The attribute '" + attribute.getIdentifier() + "' is used in a sum() aggregate function but is not defined on any feature.")
                        .hint("Define the attribute on the relevant features or correct the attribute name.")
                        .build()));
                smellyInput = true;
            }
            if (rootFeature == null || !(rootFeature instanceof Feature)) {
                String refName = ctx.reference().get(1).getText();
                errorList.add(new ParseError(new ErrorReport.Builder(ErrorCategory.CONTEXT,
                        "'" + refName + "' is not a valid feature for sum() aggregate function")
                        .line(line).charPosition(charPos)
                        .field(ErrorField.FEATURE)
                        .reference(refName)
                        .cause("The parameter '" + refName + "' must be a feature but could not be found in the feature tree.")
                        .hint("Check if the feature name is spelled correctly or add it to the feature tree.")
                        .build()));
                smellyInput = true;
            }
            if (smellyInput) {
                expressionStack.push(new NumberExpression(0));
                return;
            }
            expression = new SumAggregateFunctionExpression(attribute, (Feature) rootFeature);
            fmBuilder.getFeatureModel().getAggregateFunctionsWithRootFeature().add(expression);
        } else {
            GlobalAttribute attribute = ParsingUtilities.getGlobalAttribute(ctx.reference().get(0).getText(), fmBuilder.getFeatureModel());
            if (attribute.getType() == null) {
                errorList.add(new ParseError(new ErrorReport.Builder(ErrorCategory.CONTEXT,
                        "Attribute '" + attribute.getIdentifier() + "' does not exist in the feature model")
                        .line(line).charPosition(charPos)
                        .field(ErrorField.ATTRIBUTE)
                        .reference(attribute.getIdentifier())
                        .cause("The attribute '" + attribute.getIdentifier() + "' is used in a sum() aggregate function but is not defined on any feature.")
                        .hint("Define the attribute on the relevant features or correct the attribute name.")
                        .build()));
                expressionStack.push(new NumberExpression(0));
                return;
            }
            expression = new SumAggregateFunctionExpression(attribute);
        }
        expressionStack.push(expression);
        expression.setLineNumber(line);
    }

    @Override
    public void exitAvgAggregateFunction(UVLJavaParser.AvgAggregateFunctionContext ctx) {
        fmBuilder.addLanguageLevel(LanguageLevel.ARITHMETIC_LEVEL);
        fmBuilder.addLanguageLevel(LanguageLevel.AGGREGATE_FUNCTION);
        int line = ctx.getStart().getLine();
        int charPos = ctx.getStart().getCharPositionInLine();
        AggregateFunctionExpression expression;
        if (ctx.reference().size() > 1) {
            VariableReference rootFeature = ParsingUtilities.resolveReference(ctx.reference().get(1).getText(), fmBuilder.getFeatureModel());
            GlobalAttribute attribute = ParsingUtilities.getGlobalAttribute(ctx.reference().get(0).getText(), fmBuilder.getFeatureModel());
            boolean smellyInput = false;
            if (attribute.getType() == null) {
                errorList.add(new ParseError(new ErrorReport.Builder(ErrorCategory.CONTEXT,
                        "Attribute '" + attribute.getIdentifier() + "' does not exist in the feature model")
                        .line(line).charPosition(charPos)
                        .field(ErrorField.ATTRIBUTE)
                        .reference(attribute.getIdentifier())
                        .cause("The attribute '" + attribute.getIdentifier() + "' is used in an avg() aggregate function but is not defined on any feature.")
                        .hint("Define the attribute on the relevant features or correct the attribute name.")
                        .build()));
                smellyInput = true;
            }
            if (rootFeature == null || !(rootFeature instanceof Feature)) {
                String refName = ctx.reference().get(1).getText();
                errorList.add(new ParseError(new ErrorReport.Builder(ErrorCategory.CONTEXT,
                        "'" + refName + "' is not a valid feature for avg() aggregate function")
                        .line(line).charPosition(charPos)
                        .field(ErrorField.FEATURE)
                        .reference(refName)
                        .cause("The parameter '" + refName + "' must be a feature but could not be found in the feature tree.")
                        .hint("Check if the feature name is spelled correctly or add it to the feature tree.")
                        .build()));
                smellyInput = true;
            }
            if (smellyInput) {
                expressionStack.push(new NumberExpression(0));
                return;
            }
            expression = new AvgAggregateFunctionExpression(attribute, (Feature) rootFeature);
            fmBuilder.getFeatureModel().getAggregateFunctionsWithRootFeature().add(expression);
        } else {
            GlobalAttribute attribute = ParsingUtilities.getGlobalAttribute(ctx.reference().get(0).getText(), fmBuilder.getFeatureModel());
            if (attribute.getType() == null) {
                errorList.add(new ParseError(new ErrorReport.Builder(ErrorCategory.CONTEXT,
                        "Attribute '" + attribute.getIdentifier() + "' does not exist in the feature model")
                        .line(line).charPosition(charPos)
                        .field(ErrorField.ATTRIBUTE)
                        .reference(attribute.getIdentifier())
                        .cause("The attribute '" + attribute.getIdentifier() + "' is used in an avg() aggregate function but is not defined on any feature.")
                        .hint("Define the attribute on the relevant features or correct the attribute name.")
                        .build()));
                expressionStack.push(new NumberExpression(0));
                return;
            }
            expression = new AvgAggregateFunctionExpression(attribute);
        }
        expressionStack.push(expression);
        expression.setLineNumber(line);
    }

    @Override
    public void exitLengthAggregateFunction(UVLJavaParser.LengthAggregateFunctionContext ctx) {
        fmBuilder.addLanguageLevel(LanguageLevel.TYPE_LEVEL);
        fmBuilder.addLanguageLevel(LanguageLevel.STRING_CONSTRAINTS);
        String referenceName = ctx.reference().getText();
        int line = ctx.getStart().getLine();
        int charPos = ctx.getStart().getCharPositionInLine();

        VariableReference reference = ParsingUtilities.resolveReference(referenceName, fmBuilder.getFeatureModel());
        if (reference == null) {
            errorList.add(new ParseError(new ErrorReport.Builder(ErrorCategory.CONTEXT,
                    "Reference '" + referenceName + "' in length() could not be resolved")
                    .line(line).charPosition(charPos)
                    .field(ErrorField.EXPRESSION)
                    .reference(referenceName)
                    .cause("The feature '" + referenceName + "' does not exist in the feature tree.")
                    .hint("Check if the feature name is spelled correctly or add it to the feature tree.")
                    .build()));
            return;
        }
        if (!(reference instanceof Feature) || !((Feature) reference).getFeatureType().equals(FeatureType.STRING)) {
            errorList.add(new ParseError(new ErrorReport.Builder(ErrorCategory.CONTEXT,
                    "length() can only be used with String features, but '" + referenceName + "' is not a String feature")
                    .line(line).charPosition(charPos)
                    .field(ErrorField.EXPRESSION)
                    .reference(referenceName)
                    .cause("The feature '" + referenceName + "' is not of type String.")
                    .hint("Change the feature type to 'String' or use a different aggregate function.")
                    .build()));
            return;
        }

        LengthAggregateFunctionExpression expression = new LengthAggregateFunctionExpression(reference);
        fmBuilder.getFeatureModel().getAggregateFunctionsWithRootFeature().add(expression);
        expressionStack.push(expression);
        expression.setLineNumber(line);
    }

    @Override
    public void exitConstraints(UVLJavaParser.ConstraintsContext ctx) {
        while (!constraintStack.isEmpty()) {
            fmBuilder.addConstraintAtPosition(constraintStack.pop(), 0);
        }
    }

    public Constraint getConstraint() {
        if (!errorList.isEmpty()) {
            ParseErrorList parseErrorList = new ParseErrorList("Multiple Errors occurred during parsing!");
            parseErrorList.getErrorList().addAll(errorList);
            throw parseErrorList;
        }
        return constraintStack.pop();
    }

    public FeatureModel getFeatureModel() {
        if (errorList.size() > 0) {
            ParseErrorList parseErrorList = new ParseErrorList("Multiple Errors occurred during parsing!");
            parseErrorList.getErrorList().addAll(errorList);
            throw parseErrorList;
        }
        return fmBuilder.getFeatureModel();
    }


    @Override
    public void exitFeatureModel(UVLJavaParser.FeatureModelContext ctx) {
        if (fmBuilder.doesFeatureModelSatisfyLanguageLevels(importedLanguageLevels)) {
            errorList.add(new ParseError(new ErrorReport.Builder(ErrorCategory.CONTEXT,
                    "Imported and actually used language levels do not match")
                    .field(ErrorField.LANGUAGE_LEVEL)
                    .cause("Imported levels: " + importedLanguageLevels + ". Actually used levels: " + fmBuilder.getLanguageLevels() + ".")
                    .hint("Update the 'include' section to match the language features used in the model, or remove unsupported constructs.")
                    .build()));
        }
    }
}
