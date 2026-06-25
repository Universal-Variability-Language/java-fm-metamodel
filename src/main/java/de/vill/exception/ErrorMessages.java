// This class is for mapping specific error messages, making UVLListener and UVLModelFactory easier to read.

package de.vill.exception;

import de.vill.model.LanguageLevel;
import java.util.Set;

public final class ErrorMessages {
    
    public static ParseError build(
        ErrorCategory category,
        String message,
        int line, 
        int charPosition,
        ErrorField field,
        String reference,
        String cause,
        String hint
    ){
        return new ParseError(
            new ErrorReport.Builder(category, message)
            .line(line)
            .charPosition(charPosition)
            .field(field)
            .reference(reference)
            .cause(cause)
            .hint(hint)
            .build()
        );
    }
    
    // Invalid language level import
    public static ParseError invalidLanguageLevel(
        String languageLevel,
        int line,
        int charPos
    ){
        return build(
            ErrorCategory.CONTEXT,
            "Invalid language level import: '" + languageLevel + "'",
            line,
            charPos,
            ErrorField.LANGUAGE_LEVEL,
            languageLevel,
            "Invalid language level import.",
            "Use a valid language level format."
        );
    }

    // No Import
    public static ParseError noImport(
        String featureReference,
        int line,
        int charPos
    ){
        return build(
            ErrorCategory.CONTEXT,
            "Feature '" + featureReference + "' is referenced as imported, but no matching import exists",
            line,
            charPos,
            ErrorField.IMPORT,
            featureReference,
            "The feature name suggests it comes from an imported submodel, but the import was not declared.",
            "Add the corresponding import in the 'imports' section or correct the feature name."
        );
    }

    // Duplicate Feature
    public static ParseError duplicateFeature(
        String featureReference,
        int originalLine,
        int line,
        int charPos
    ){
        return build(
            ErrorCategory.CONTEXT,
            "Duplicate feature name: '" + featureReference + "'",
            line,
            charPos,
            ErrorField.FEATURE,
            featureReference,
            "A feature with the name '" + featureReference + "' already exists in the feature tree (first defined at line " + originalLine + ").",
            "Rename one of the duplicate features to make names unique."
        );
    }

    // Unsupported Attribute Value Type
    public static ParseError unsupportedAttributeValueType(
        String attributeName,
        String value,
        int line,
        int charPos
    ){
        return build(
            ErrorCategory.CONTEXT,
            "Unsupported attribute value type: '" + value + "'",
            line,
            charPos,
            ErrorField.ATTRIBUTE,
            attributeName,
            "The value '" + value + "' does not match any supported attribute type (Boolean, Integer, Float, String, Vector, Attributes).",
            "Use a supported value type for the attribute."
        );
    }

    // Feature not in Tree but in Constraint
    public static ParseError featureNotInTreeButConstraint(
        String referenceName,
        int line,
        int charPos
    ){
        return build(
            ErrorCategory.CONTEXT,
            "Reference '" + referenceName + "' in constraint could not be resolved",
            line,
            charPos,
            ErrorField.CONSTRAINT,
            referenceName,
            "The feature or attribute '" + referenceName + "' is used in a constraint but does not exist in the feature tree.",
            "Check if the feature name is spelled correctly or add it to the feature tree."
        );
    }    

    // Feature not in Tree but in Expression
    public static ParseError featureNotInTreeButExpression(
        String referenceName,
        int line,
        int charPos
    ){
        return build(
            ErrorCategory.CONTEXT,
            "Variable '" + referenceName + "' in expression could not be resolved",
            line,
            charPos,
            ErrorField.EXPRESSION,
            referenceName,
            "The feature or attribute '" + referenceName + "' is used in an expression but does not exist in the feature tree.",
            "Check if the variable name is spelled correctly or add it to the feature tree."
        );
    }   

    // Attribute used with sum() does not exist
    public static ParseError sumAttributeNotDefined(
        String attributeIdentifier,
        int line,
        int charPos
    ){
        return build(
            ErrorCategory.CONTEXT,
            "Attribute '" + attributeIdentifier + "' does not exist in the feature model",
            line,
            charPos,
            ErrorField.ATTRIBUTE,
            attributeIdentifier,
            "The attribute '" + attributeIdentifier + "' is used in a sum() aggregate function but is not defined on any feature.",
            "Define the attribute on the relevant features or correct the attribute name."
        );
    }

    // Feature used with sum() invalid or does not exist
    public static ParseError sumFeatureNotDefined(
        String referenceName,
        int line,
        int charPos
    ){
        return build(
            ErrorCategory.CONTEXT,
            "'" + referenceName + "' is not a valid feature for sum() aggregate function",
            line,
            charPos,
            ErrorField.FEATURE,
            referenceName,
            "The parameter '" + referenceName + "' must be a feature but could not be found in the feature tree.",
            "Check if the feature name is spelled correctly or add it to the feature tree."
        );
    } 

    // Attribute used with avg() does not exist
    public static ParseError avgAttributeNotDefined(
        String attributeIdentifier,
        int line,
        int charPos
    ){
        return build(
            ErrorCategory.CONTEXT,
            "Attribute '" + attributeIdentifier + "' does not exist in the feature model",
            line,
            charPos,
            ErrorField.ATTRIBUTE,
            attributeIdentifier,
            "The attribute '" + attributeIdentifier + "' is used in a avg() aggregate function but is not defined on any feature.",
            "Define the attribute on the relevant features or correct the attribute name."
        );
    }

    // Feature used with avg() invalid or does not exist
    public static ParseError avgFeatureNotDefined(
        String referenceName,
        int line,
        int charPos
    ){
        return build(
            ErrorCategory.CONTEXT,
            "'" + referenceName + "' is not a valid feature for sum() aggregate function",
            line,
            charPos,
            ErrorField.FEATURE,
            referenceName,
            "The parameter '" + referenceName + "' must be a feature but could not be found in the feature tree.",
            "Check if the feature name is spelled correctly or add it to the feature tree."
        );
    } 

    // Feature used with length() does not exist
    public static ParseError lenFeatureNotDefined(
        String referenceName,
        int line,
        int charPos
    ){
        return build(
            ErrorCategory.CONTEXT,
            "Reference '" + referenceName + "' in length() could not be resolved",
            line,
            charPos,
            ErrorField.EXPRESSION,
            referenceName,
            "The feature '" + referenceName + "' does not exist in the feature tree.",
            "Check if the feature name is spelled correctly or add it to the feature tree."
        );
    } 


    // length() can only be used with String features
    public static ParseError lenFeatureNotString(
        String referenceName,
        int line,
        int charPos
    ){
        return build(
            ErrorCategory.CONTEXT,
            "length() can only be used with String features, but '" + referenceName + "' is not a String feature",
            line,
            charPos,
            ErrorField.EXPRESSION,
            referenceName,
            "The feature '" + referenceName + "' is not of type String.",
            "Change the feature type to 'String' or use a different aggregate function."
        );
    } 

    // Imported language levels do not match
    public static ParseError languageLevelsDoNotMatch(
        Set<LanguageLevel> importedLanguageLevels,
        Set<LanguageLevel> actualLanguageLevels,
        int line,
        int charPos
    ){
        return build(
            ErrorCategory.CONTEXT,
            "Imported and actually used language levels do not match",
            line,
            charPos,
            ErrorField.LANGUAGE_LEVEL,
            importedLanguageLevels.toString(),
            "Imported levels: " + importedLanguageLevels + ". Actually used levels: " + actualLanguageLevels + ".",
            "Update the 'include' section to match the language features used in the model, or remove unsupported constructs."
        );
    } 

    // Imported feature not found
    public static ParseError importedFeatureNotFound(
        String fullRef
    ){
        return build(
            ErrorCategory.CONTEXT,
            "Imported feature '" + fullRef + "' could not be resolved",
            0,
            0,
            ErrorField.IMPORT,
            fullRef,
            "The feature '" + fullRef + "' does not exist in the imported submodel.",
            "Check that the feature exists in the imported model or correct the reference."
        );
    }

    // Imported feature attribute reference does not exist
    public static ParseError importFeatureAttributeReferenceMissing(
        String fullRef
    ){
        return build(
            ErrorCategory.CONTEXT,
            "Imported feature for attribute reference '" + fullRef + "' could not be resolved",
            0,
            0,
            ErrorField.IMPORT,
            fullRef,
            "The feature '" + fullRef + "' does not exist in the imported submodel.",
            "Check that the feature exists in the imported model or correct the reference."
        );
    }

    // Attribute not found on imported feature
    public static ParseError missingAttributewithImportedFeature(
        String fullRef,
        String attrName
    ){
        return build(
            ErrorCategory.CONTEXT,
            "Attribute '" + attrName + "' not found on imported feature '" + fullRef + "'",
            0,
            0,
            ErrorField.ATTRIBUTE,
            fullRef,
            "The attribute '" + attrName + "' does not exist on the feature referenced by '" + fullRef + "'.",
            "Check the attribute name or define it on the feature in the imported model."
        );
    }

    // Reference couldn't be resolved to a feature or attribute
    public static ParseError unresolvedImportedReference(
        String fullRef
    ){
        return build(
            ErrorCategory.CONTEXT,
            "Could not resolve imported reference '" + fullRef + "'",
            0,
            0,
            ErrorField.IMPORT,
            fullRef,
            "The reference could not be resolved to a feature or attribute in the imported submodel.",
            "Check the import path and ensure the referenced element exists."
        );
    }
}
