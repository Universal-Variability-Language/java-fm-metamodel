package de.vill.parsing;

import de.vill.main.UVLModelFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class LongConstraintParsingTest {

    @Test
    void parsesLongOrConstraint() {
        final int numberOfLiterals = 7000;
        final String model = createModel(numberOfLiterals);

        final UVLModelFactory factory = new UVLModelFactory();

        assertDoesNotThrow(() -> factory.parse(model));
    }

    private String createModel(int numberOfLiterals) {
        final StringBuilder builder = new StringBuilder();

        builder.append("features\n");
        builder.append("    Root\n");
        builder.append("        optional\n");

        for (int i = 0; i < numberOfLiterals; i++) {
            builder.append("            F").append(i).append("\n");
        }

        builder.append("constraints\n");
        builder.append("    ");

        for (int i = 0; i < numberOfLiterals; i++) {
            if (i > 0) {
                builder.append(" | ");
            }
            builder.append("F").append(i);
        }

        builder.append("\n");
        return builder.toString();
    }
}
