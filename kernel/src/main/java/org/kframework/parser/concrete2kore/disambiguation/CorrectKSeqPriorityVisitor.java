// Copyright (c) 2015 K Team. All Rights Reserved.
package org.kframework.parser.concrete2kore.disambiguation;

import com.google.common.collect.Sets;
import org.kframework.definition.NonTerminal;
import org.kframework.parser.SetsTransformerWithErrors;
import org.kframework.parser.Term;
import org.kframework.parser.TermCons;
import org.kframework.utils.errorsystem.KException;
import org.kframework.utils.errorsystem.ParseFailedException;
import org.kframework.utils.errorsystem.PriorityException;
import scala.util.Either;
import scala.util.Left;
import scala.util.Right;


/**
 * Make sure that KSequence binds greedy (has least priority after rewrite).
 */
public class CorrectKSeqPriorityVisitor extends SetsTransformerWithErrors<ParseFailedException> {

    @Override
    public Either<java.util.Set<ParseFailedException>, Term> apply(TermCons tc) {
        assert tc.production() != null : this.getClass() + ":" + " production not found." + tc;
        if (!tc.production().isSyntacticSubsort()
                && !tc.production().klabel().get().name().equals("#KRewrite")
                && !tc.production().klabel().get().name().equals("#KSequence")) {
            // match only on the outermost elements
            if (tc.production().items().head() instanceof NonTerminal) {
                Either<java.util.Set<ParseFailedException>, Term> rez =
                        new PriorityVisitor2(tc).apply(tc.items().get(0));
                if (rez.isLeft())
                    return rez;
                tc.items().set(0, rez.right().get());
            }
            if (tc.production().items().last() instanceof NonTerminal) {
                int last = tc.items().size() - 1;
                Either<java.util.Set<ParseFailedException>, Term> rez =
                        new PriorityVisitor2(tc).apply(tc.items().get(last));
                if (rez.isLeft())
                    return rez;
                tc.items().set(last, rez.right().get());
            }
        }
        return super.apply(tc);
    }

    private static class PriorityVisitor2 extends SetsTransformerWithErrors<ParseFailedException> {
        private final TermCons parent;
        public PriorityVisitor2(TermCons parent) {
            this.parent = parent;
        }

        public Either<java.util.Set<ParseFailedException>, Term> apply(TermCons tc) {
            // TODO: add location information
            if (tc.production().klabel().isDefined() && tc.production().klabel().get().name().equals("#KSequence")) {
                String msg = "Could not parse ~> as a child of " + parent.production() +
                        "    Use parentheses to set the proper scope of the ~> operation.";
                KException kex = new KException(KException.ExceptionType.ERROR, KException.KExceptionGroup.CRITICAL, msg, null, tc.location().get());
                return Left.apply(Sets.newHashSet(new PriorityException(kex)));
            }
            return Right.apply(tc);
        }
    }
}
