package de.vill.main;

import uvl.UVLJavaParser;
import uvl.UVLJavaBaseListener;

import de.vill.exception.ParseError;
import de.vill.exception.ParseErrorList;
import de.vill.model.Attribute;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import de.vill.model.FeatureType;
import de.vill.model.Group;
import de.vill.model.Import;
import de.vill.model.LanguageLevel;
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
import de.vill.model.expression.CeilAggregateFunctionExpression;
import de.vill.model.expression.DivExpression;
import de.vill.model.expression.Expression;
import de.vill.model.expression.FloorAggregateFunctionExpression;
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

public class UVLListener extends UVLJavaBaseListener {
    private FeatureModel featureModel = new FeatureModel();
    private Set<LanguageLevel> importedLanguageLevels = new HashSet<>(Arrays.asList(LanguageLevel.BOOLEAN_LEVEL));
    private Stack<Feature> featureStack = new Stack<>();
    private Stack<Group> groupStack = new Stack<>();

    private Stack<Constraint> constraintStack = new Stack<>();

    private Stack<Expression> expressionStack = new Stack<>();

    private Stack<Map<String, Attribute<?>>> attributeStack = new Stack<>();

    private boolean featureCardinality = false;

    private List<ParseError> errorList = new LinkedList<>();

    @Override
    public void enterIncludes(UVLJavaParser.IncludesContext ctx) {
        featureModel.setExplicitLanguageLevels(true);
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
            errorList.add(new ParseError("Invalid import Statement: " + ctx.languageLevel().getText()));
            //throw new ParseError("Invalid import Statement: " + ctx.LANGUAGELEVEL().getText());
        }
    }

    @Override
    public void exitNamespace(UVLJavaParser.NamespaceContext ctx) {
        featureModel.setNamespace(ctx.reference().getText().replace("\"", ""));
    }

    @Override
    public void exitImportLine(UVLJavaParser.ImportLineContext ctx) {
        Import importLine;
        if (ctx.alias != null) {
            importLine = new Import(ctx.ns.getText().replace("\"", ""), ctx.alias.getText().replace("\"", ""));
        } else {
            importLine = new Import(ctx.ns.getText().replace("\"", ""), null);
        }
        Token t = ctx.getStart();
        int line = t.getLine();
        importLine.setLineNumber(line);
        featureModel.getImports().add(importLine);
    }

    @Override
    public void enterFeatures(UVLJavaParser.FeaturesContext ctx) {
        groupStack.push(new Group(Group.GroupType.MANDATORY));
    }

    @Override
    public void exitFeatures(UVLJavaParser.FeaturesContext ctx) {
        Group group = groupStack.pop();
        Feature feature = group.getFeatures().get(0);
        featureModel.setRootFeature(feature);
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
        String lowerBound;
        String upperBound;
        if (ctx.getText().contains("..")) {
            lowerBound = ctx.CARDINALITY().getText().replace("[", "").replace("]", "").split("\\.\\.")[0];
            upperBound = ctx.CARDINALITY().getText().replace("[", "").replace("]", "").split("\\.\\.")[1];
        } else {
            lowerBound = ctx.getText().replace("[", "").replace("]", "");
            upperBound = lowerBound;
        }
        group.setLowerBound(lowerBound);
        group.setUpperBound(upperBound);

        Feature feature = featureStack.peek();
        feature.addChildren(group);
        group.setParentFeature(feature);
        groupStack.push(group);

        featureModel.getUsedLanguageLevels().add(LanguageLevel.GROUP_CARDINALITY);
    }

    @Override
    public void exitCardinalityGroup(UVLJavaParser.CardinalityGroupContext ctx) {
        groupStack.pop();
    }

    @Override
    public void enterFeature(UVLJavaParser.FeatureContext ctx) {
        String featureReference = ctx.reference().getText().replace("\"", "");
        String[] featureReferenceParts = featureReference.split("\\.");
        String featureName;
        String featureNamespace;
        if (featureReferenceParts.length > 1) {
            featureName = featureReferenceParts[featureReferenceParts.length - 1];
            featureNamespace = featureReference.substring(0, featureReference.length() - featureName.length() - 1);
        } else {
            featureName = featureReferenceParts[0];
            featureNamespace = null;
        }

        Feature feature = new Feature(featureName);
        if (featureNamespace != null) {
            feature.setNameSpace(featureNamespace);
            feature.setSubmodelRoot(true);
            for (Import importLine : featureModel.getImports()) {
                if (importLine.getAlias().equals(featureNamespace)) {
                    feature.setRelatedImport(importLine);
                    break;
                }
            }
            if (feature.getRelatedImport() == null) {
                errorList.add(new ParseError("Feature " + featureReference + " is imported, but there is no import named " + featureNamespace));
                //throw new ParseError("Feature " + featureReference + " is imported, but there is no import named " + featureNamespace);
            }
        }

        featureStack.push(feature);
        Group parentGroup = groupStack.peek();
        parentGroup.getFeatures().add(feature);
        feature.setParentGroup(parentGroup);
        if (featureNamespace == null) {
            featureModel.getFeatureMap().put(featureName, feature);
        } else {
            featureModel.getFeatureMap().put(featureNamespace + "." + featureName, feature);
        }

    }

    @Override
    public void exitFeature(UVLJavaParser.FeatureContext ctx) {
        featureStack.pop();
    }

    @Override
    public void enterFeatureType(final UVLJavaParser.FeatureTypeContext ctx) {
        final Feature feature = this.featureStack.peek();
        feature.setFeatureType(FeatureType.fromString(ctx.getText().toLowerCase()));
        this.featureModel.getUsedLanguageLevels().add(LanguageLevel.TYPE_LEVEL);
    }

    @Override
    public void exitFeatureCardinality(UVLJavaParser.FeatureCardinalityContext ctx) {
        String lowerBound;
        String upperBound;
        if (ctx.getText().contains("..")) {
            lowerBound = ctx.CARDINALITY().getText().replace("[", "").replace("]", "").split("\\.\\.")[0];
            upperBound = ctx.CARDINALITY().getText().replace("[", "").replace("]", "").split("\\.\\.")[1];
        } else {
            lowerBound = ctx.getText().replace("[", "").replace("]", "");
            upperBound = lowerBound;
        }
        if (upperBound.equals("*")) {
            errorList.add(new ParseError("Feature Cardinality must not have * as upper bound! (" + ctx.CARDINALITY().getText() + ")"));
            //throw new ParseError("Feature Cardinality must not have * as upper bound! (" + ctx.CARDINALITY().getText() + ")");
        }

        Feature feature = featureStack.peek();
        feature.setLowerBound(lowerBound);
        feature.setUpperBound(upperBound);

        featureModel.getUsedLanguageLevels().add(LanguageLevel.ARITHMETIC_LEVEL);
        featureModel.getUsedLanguageLevels().add(LanguageLevel.FEATURE_CARDINALITY);
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

        if (ctx.value() == null) {
            Attribute<Boolean> attribute = new Attribute<>(attributeName, true);
            attributeStack.peek().put(attributeName, attribute);
        } else if (ctx.value().BOOLEAN() != null) {
            Attribute<Boolean> attribute = new Attribute<>(attributeName, Boolean.parseBoolean(ctx.value().getText().replace("'", "")));
            attributeStack.peek().put(attributeName, attribute);
        } else if (ctx.value().INTEGER() != null) {
            Attribute<Integer> attribute = new Attribute<>(attributeName, Integer.parseInt(ctx.value().getText().replace("'", "")));
            attributeStack.peek().put(attributeName, attribute);
        } else if (ctx.value().FLOAT() != null) {
            Attribute<Double> attribute = new Attribute<>(attributeName, Double.parseDouble(ctx.value().getText().replace("'", "")));
            attributeStack.peek().put(attributeName, attribute);
        } else if (ctx.value().STRING() != null) {
            Attribute<String> attribute = new Attribute<>(attributeName, ctx.value().getText().replace("'", ""));
            attributeStack.peek().put(attributeName, attribute);
        } else if (ctx.value().vector() != null) {
            String vectorString = ctx.value().getText();
            vectorString = vectorString.substring(1, vectorString.length() - 1);
            Attribute<List<String>> attribute = new Attribute<>(attributeName, Arrays.asList(vectorString.split(",")));
            attributeStack.peek().put(attributeName, attribute);
        } else if (ctx.value().attributes() != null) {
            Map<String, Attribute<?>> attributes = attributeStack.pop();
            Attribute<Map<String, Attribute<?>>> attribute = new Attribute<>(attributeName, attributes);
            attributeStack.peek().put(attributeName, attribute);
        } else {
            errorList.add(new ParseError(ctx.value().getText() + " is no value of any supported attribute type!"));
            //throw new ParseError(ctx.value().getText() + " is no value of any supported attribute type!");
        }
    }

    @Override
    public void exitSingleConstraintAttribute(UVLJavaParser.SingleConstraintAttributeContext ctx) {
    	attributeStack.peek().put("constraint",
    			new Attribute<>("constraint", constraintStack.pop()));
    }

    @Override
    public void exitListConstraintAttribute(UVLJavaParser.ListConstraintAttributeContext ctx) {
        List<Constraint> constraintList = new ArrayList<>(constraintStack.size());
        while (!constraintStack.empty()) {
            constraintList.add(constraintStack.pop());
        }
        attributeStack.peek().put("constraints",
        		new Attribute<>("constraints", constraintList));
    }


    @Override
    public void exitLiteralConstraint(UVLJavaParser.LiteralConstraintContext ctx) {
        String featureReference = ctx.reference().getText().replace("\"", "");

        LiteralConstraint constraint = new LiteralConstraint(featureReference);

        if (featureReference.contains(".")) {
            int lastDotIndex = featureReference.lastIndexOf(".");
            String subModelName = featureReference.substring(0, lastDotIndex);
            String featureName = featureReference.substring(lastDotIndex + 1, featureReference.length());
            for (Import importLine : featureModel.getImports()) {
                if (importLine.getAlias().equals(subModelName)) {
                    constraint.setRelatedImport(importLine);
                    break;
                }
            }
        }
        featureModel.getLiteralConstraints().add(constraint);
        constraintStack.push(constraint);
        Token t = ctx.getStart();
        int line = t.getLine();
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
            LiteralExpression literalExpression = (LiteralExpression) expressionStack.peek();
            if (literalExpression.getAttributeName() == null) {
                featureModel.getUsedLanguageLevels().add(LanguageLevel.TYPE_LEVEL);
                featureModel.getUsedLanguageLevels().add(LanguageLevel.STRING_CONSTRAINTS);
            } else {
                featureModel.getUsedLanguageLevels().add(LanguageLevel.ARITHMETIC_LEVEL);
            }
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
        String reference = ctx.reference().getText().replace("\"", "");
        LiteralExpression expression = new LiteralExpression(reference);
        String[] splitReference = reference.split("\\.");
        if (splitReference.length > 1) {
            featureModel.getUsedLanguageLevels().add(LanguageLevel.ARITHMETIC_LEVEL);
        }
        expressionStack.push(expression);
        featureModel.getLiteralExpressions().add(expression);
        Token t = ctx.getStart();
        int line = t.getLine();
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
        featureModel.getUsedLanguageLevels().add(LanguageLevel.ARITHMETIC_LEVEL);
        featureModel.getUsedLanguageLevels().add(LanguageLevel.AGGREGATE_FUNCTION);
        AggregateFunctionExpression expression;
        if (ctx.reference().size() > 1) {
            expression = new SumAggregateFunctionExpression(ctx.reference().get(1).getText().replace("\"", ""), ctx.reference().get(0).getText().replace("\"", ""));
            featureModel.getAggregateFunctionsWithRootFeature().add(expression);
        } else {
            expression = new SumAggregateFunctionExpression(ctx.reference().get(0).getText().replace("\"", ""));
        }
        expressionStack.push(expression);
        Token t = ctx.getStart();
        int line = t.getLine();
        expression.setLineNumber(line);
    }

    @Override
    public void exitAvgAggregateFunction(UVLJavaParser.AvgAggregateFunctionContext ctx) {
        featureModel.getUsedLanguageLevels().add(LanguageLevel.ARITHMETIC_LEVEL);
        featureModel.getUsedLanguageLevels().add(LanguageLevel.AGGREGATE_FUNCTION);
        AggregateFunctionExpression expression;
        if (ctx.reference().size() > 1) {
            expression = new AvgAggregateFunctionExpression(ctx.reference().get(1).getText().replace("\"", ""), ctx.reference().get(0).getText().replace("\"", ""));
            featureModel.getAggregateFunctionsWithRootFeature().add(expression);
        } else {
            expression = new AvgAggregateFunctionExpression(ctx.reference().get(0).getText().replace("\"", ""));
        }
        expressionStack.push(expression);
        Token t = ctx.getStart();
        int line = t.getLine();
        expression.setLineNumber(line);
    }

    @Override
    public void exitLengthAggregateFunction(UVLJavaParser.LengthAggregateFunctionContext ctx) {
        featureModel.getUsedLanguageLevels().add(LanguageLevel.TYPE_LEVEL);
        featureModel.getUsedLanguageLevels().add(LanguageLevel.STRING_CONSTRAINTS);

        String reference = ctx.reference().getText().replace("\"", "");
        if (!(featureModel.getFeatureMap().containsKey(reference) && FeatureType.STRING.equals(featureModel.getFeatureMap().get(reference).getFeatureType()))) {
            errorList.add(new ParseError("Length Aggregate Function can only be used with String features"));
            return;
        }

        AggregateFunctionExpression expression = new LengthAggregateFunctionExpression(reference);
        featureModel.getAggregateFunctionsWithRootFeature().add(expression);
        expressionStack.push(expression);
        Token t = ctx.getStart();
        int line = t.getLine();
        expression.setLineNumber(line);
    }

    @Override public void exitFloorAggregateFunction(UVLJavaParser.FloorAggregateFunctionContext ctx) {
        featureModel.getUsedLanguageLevels().add(LanguageLevel.TYPE_LEVEL);
        featureModel.getUsedLanguageLevels().add(LanguageLevel.NUMERIC_CONSTRAINTS);

        String reference = ctx.reference().getText().replace("\"", "");
        if (!(featureModel.getFeatureMap().containsKey(reference) && (
            FeatureType.INT.equals(featureModel.getFeatureMap().get(reference).getFeatureType())
                || FeatureType.REAL.equals(featureModel.getFeatureMap().get(reference).getFeatureType())
        ))) {
            errorList.add(new ParseError("Floor Aggregate Function can only be used with Integer or Real features"));
            return;
        }

        AggregateFunctionExpression expression = new FloorAggregateFunctionExpression(reference);
        featureModel.getAggregateFunctionsWithRootFeature().add(expression);
        expressionStack.push(expression);
        Token t = ctx.getStart();
        int line = t.getLine();
        expression.setLineNumber(line);
    }

    @Override public void exitCeilAggregateFunction(UVLJavaParser.CeilAggregateFunctionContext ctx) {
        featureModel.getUsedLanguageLevels().add(LanguageLevel.TYPE_LEVEL);
        featureModel.getUsedLanguageLevels().add(LanguageLevel.NUMERIC_CONSTRAINTS);

        String reference = ctx.reference().getText().replace("\"", "");
        if (!(featureModel.getFeatureMap().containsKey(reference) && (
            FeatureType.INT.equals(featureModel.getFeatureMap().get(reference).getFeatureType())
                || FeatureType.REAL.equals(featureModel.getFeatureMap().get(reference).getFeatureType())
        ))) {
            errorList.add(new ParseError("Ceil Aggregate Function can only be used with Integer or Real features"));
            return;
        }

        AggregateFunctionExpression expression = new CeilAggregateFunctionExpression(reference);
        featureModel.getAggregateFunctionsWithRootFeature().add(expression);
        expressionStack.push(expression);
        Token t = ctx.getStart();
        int line = t.getLine();
        expression.setLineNumber(line);
    }

    @Override
    public void exitConstraints(UVLJavaParser.ConstraintsContext ctx) {
        while (!constraintStack.isEmpty()) {
            featureModel.getOwnConstraints().add(0, constraintStack.pop());
        }
    }

    public Constraint getConstraint() {
        if (errorList.size() > 0) {
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
        return featureModel;
    }


    @Override
    public void exitFeatureModel(UVLJavaParser.FeatureModelContext ctx) {
        if (featureModel.isExplicitLanguageLevels() && !featureModel.getUsedLanguageLevels().equals(importedLanguageLevels)) {
            errorList.add(new ParseError("Imported and actually used language levels do not match! \n Imported: " + importedLanguageLevels.toString() + "\nAcutally Used: " + featureModel.getUsedLanguageLevels().toString()));
            //throw new ParseError("Imported and actually used language levels do not match! \n Imported: " + importedLanguageLevels.toString() + "\nAcutally Used: " + featureModel.getUsedLanguageLevels().toString());
        }
    }

    /*

    @Override public void exitFeatures(UVLJavaParser.FeaturesContext ctx) {
        System.out.println("features");
    }

    @Override public void exitRootFeature(UVLJavaParser.RootFeatureContext ctx) {
        System.out.println("rootFeature");
    }

    @Override public void exitGroup(UVLJavaParser.GroupContext ctx) {
        System.out.println("group");
    }

    @Override public void exitFeature(UVLJavaParser.FeatureContext ctx) {
        System.out.println("feature");
    }

 */


}
