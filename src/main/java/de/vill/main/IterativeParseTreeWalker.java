package de.vill.main;

import java.util.ArrayDeque;
import java.util.Deque;

import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * Iterative variant of ANTLR's ParseTreeWalker.
 *
 * ANTLR's default ParseTreeWalker recursively walks the parse tree.
 * Deeply nested constraints such as F0 | F1 | ... | F6999 can therefore
 * overflow the Java call stack before the UVL model is constructed.
 */
final class IterativeParseTreeWalker extends ParseTreeWalker {

    @Override
    public void walk(ParseTreeListener listener, ParseTree tree) {
        final Deque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame(tree));

        while (!stack.isEmpty()) {
            final Frame frame = stack.peek();
            final ParseTree current = frame.tree;

            if (current instanceof ErrorNode) {
                listener.visitErrorNode((ErrorNode) current);
                stack.pop();
                continue;
            }

            if (current instanceof TerminalNode) {
                listener.visitTerminal((TerminalNode) current);
                stack.pop();
                continue;
            }

            final RuleNode ruleNode = (RuleNode) current;

            if (!frame.entered) {
                enterRule(listener, ruleNode);
                frame.entered = true;
            }

            if (frame.nextChildIndex < current.getChildCount()) {
                stack.push(new Frame(current.getChild(frame.nextChildIndex)));
                frame.nextChildIndex++;
            } else {
                exitRule(listener, ruleNode);
                stack.pop();
            }
        }
    }

    private static final class Frame {
        private final ParseTree tree;
        private boolean entered;
        private int nextChildIndex;

        private Frame(ParseTree tree) {
            this.tree = tree;
        }
    }
}
