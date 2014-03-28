package org.kframework.parser.concrete2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.kframework.kil.Ambiguity;
import org.kframework.kil.KApp;
import org.kframework.kil.KList;
import org.kframework.kil.KSorts;
import org.kframework.kil.Term;
import org.kframework.kil.Token;
import org.kframework.parser.concrete2.Grammar.EntryState;
import org.kframework.parser.concrete2.Grammar.ExitState;
import org.kframework.parser.concrete2.Grammar.NextableState;
import org.kframework.parser.concrete2.Grammar.NonTerminal;
import org.kframework.parser.concrete2.Grammar.NonTerminalId;
import org.kframework.parser.concrete2.Grammar.NonTerminalState;
import org.kframework.parser.concrete2.Grammar.PrimitiveState;
import org.kframework.parser.concrete2.Grammar.RegExState;
import org.kframework.parser.concrete2.Grammar.RuleState;
import org.kframework.parser.concrete2.Grammar.State;
import org.kframework.parser.concrete2.Grammar.StateId;
import org.kframework.parser.concrete2.Rule.ContextFreeRule;
import org.kframework.parser.concrete2.Rule.ContextSensitiveRule;
import org.kframework.parser.concrete2.Rule.MetaData;

/*
Terminology:
  entryState/exitState: the first and last states in a non-terminal

  stateCall/stateReturn: records for the entery to or exit from a state
  ntCall/ntReturn: records for the entry to or exit from a non-terminal

  stateBegin/stateEnd: the source positions for the begining and end the span of a state
  ntBegin/ntEnd: the source positions for the begining and end the span of a state

  nextState/previousState: successor/predicesor states in a state machine
*/

////////////////

/// The dynamic record for where a state starts parsing
class StateCall {
    final Function function = Function.empty();

    public static class Key {
        final NonTerminalCall ntCall;
        final int stateBegin;
        final State state;

        public Key(NonTerminalCall ntCall, int stateBegin, State state) {
//// Boilerplate after this line ////
            this.ntCall = ntCall; this.stateBegin = stateBegin; this.state = state;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (stateBegin != key.stateBegin) return false;
            if (!ntCall.equals(key.ntCall)) return false;
            if (!state.equals(key.state)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = ntCall.key.hashCode();
            result = 31 * result + stateBegin;
            result = 31 * result + state.hashCode();
            return result;
        }
    }
    final Key key;
    StateCall(Key key) { this.key = key; }
}

/// The dynamic record for where a state ends parsing
class StateReturn implements Comparable<StateReturn> {
    final Function function = Function.empty();

    public int compareTo(StateReturn that) {
        int x;
        return
            // NOTE: ntBegin is contravarient
            ((x = Integer.compare(that.key.stateCall.key.ntCall.key.ntBegin,
                                  this.key.stateCall.key.ntCall.key.ntBegin)) != 0) ? x :
            // TODO: ((x = this.key.stateCall.key.ntCall.key.nt.orderingInfo.compareTo(
            //             that.key.stateCall.key.ntCall.key.nt.orderingInfo)) != 0) ? x :
            ((x = Integer.compare(this.key.stateEnd, that.key.stateEnd)) != 0) ? x :
            ((x = this.key.stateCall.key.state.orderingInfo.compareTo(
                  that.key.stateCall.key.state.orderingInfo)) != 0) ? x :
            // NOTE: these last two comparisons are just so we don't conflate distinct values
            ((x = Integer.compare(this.key.stateCall.key.stateBegin,
                                  that.key.stateCall.key.stateBegin)) != 0) ? x :
            this.key.stateCall.key.state.compareTo(that.key.stateCall.key.state);
    }

    public static class Key {
        public final StateCall stateCall;
        public final int stateEnd;
        public Key(StateCall stateCall, int stateEnd) {
            if (stateCall.key.state instanceof NonTerminalState &&
                ((NonTerminalState)stateCall.key.state).isLookahead) {
                stateEnd = stateCall.key.stateBegin;
            }
//// Boilerplate after this line (except as noted) ////
            this.stateCall = stateCall; this.stateEnd = stateEnd;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (stateEnd != key.stateEnd) return false;
            if (!stateCall.equals(key.stateCall)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = stateCall.key.hashCode();
            result = 31 * result + stateEnd;
            return result;
        }
    }

    final Key key;
    StateReturn(Key key) {
        this.key = key;
        //// NON-BOILERPLATE CODE: ////
        if (this.key.stateCall.key.state instanceof ExitState) {
            this.key.stateCall.key.ntCall.exitStateReturns.add(this);
        }
    }

    public int hashCode() {
        return this.key.hashCode();
    }
}

class Context {
    Set<KList> contexts = new HashSet<>();
}

/// The dynamic record for where a non-terminal starts parsing
class NonTerminalCall {
    final Set<StateCall> callers = new HashSet<>();
    final Set<StateReturn> exitStateReturns = new HashSet<>();
    final Set<StateReturn> reactivations = new HashSet<>();
    final Context context = new Context();
    public static class Key {
        public final NonTerminal nt;
        public final int ntBegin;
//// Boilerplate after this line ////
        public Key(NonTerminal nt, int ntBegin) {
            // assert ntBegin == c.stateBegin for c in callers
            this.nt = nt; this.ntBegin = ntBegin;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (ntBegin != key.ntBegin) return false;
            if (!nt.equals(key.nt)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = nt.hashCode();
            result = 31 * result + ntBegin;
            return result;
        }
    }
    final Key key;
    NonTerminalCall(Key key) { this.key = key; }
}

////////////////

class StateReturnWorkList extends TreeSet<StateReturn> {
    public void enqueue(StateReturn stateReturn) { this.add(stateReturn); }
    public StateReturn dequeue() { return this.pollFirst(); }
}

class ParseState {
    final CharSequence input;
    final StateReturnWorkList stateReturnWorkList = new StateReturnWorkList();
    final int[] lines;
    final int[] columns;
    public ParseState(CharSequence input) {
        this.input = input;
        lines = new int[input.length()+1];
        columns = new int[input.length()+1];
        int l = 1;
        int c = 1;
        for (int i = 0; i < input.length(); i++) {
            lines[i] = l;
            columns[i] = c;
            switch (input.charAt(i)) {
                case '\r' :
                    if (i+1 < input.length()) {
                        if (input.charAt(i+1) == '\n') {
                            lines[i+1] = l;
                            columns[i+1] = c + 1;
                            i++;
                        }
                    }
                case '\n'      :
                case  '\u000B' :
                case  '\u000C' :
                case  '\u0085' :
                case  '\u2028' :
                case  '\u2029' :
                    l++; c = 1; break;
                default :
                    c++;
            }
        }
        lines[input.length()] = l;
        columns[input.length()] = c;
    }

    Map<NonTerminalCall.Key,NonTerminalCall> ntCalls = new HashMap<>();
    Map<StateCall.Key,StateCall> stateCalls = new HashMap<>();
    Map<StateReturn.Key,StateReturn> stateReturns = new HashMap<>();

    public NonTerminalCall getNtCall(NonTerminalCall.Key key) {
        NonTerminalCall value = ntCalls.get(key);
        if (value == null) {
            value = new NonTerminalCall(key);
            ntCalls.put(key, value);
        }
        return value;
    }

    public StateCall getStateCall(StateCall.Key key) {
        StateCall value = stateCalls.get(key);
        if (value == null) {
            value = new StateCall(key);
            stateCalls.put(key, value);
        }
        return value;
    }

    public StateReturn getStateReturn(StateReturn.Key key) {
        StateReturn value = stateReturns.get(key);
        if (value == null) {
            value = new StateReturn(key);
            stateReturns.put(key, value);
        }
        return value;
    }
}

////////////////

class Function {
    private abstract class Mapping {}
    private class Nil extends Mapping { Set<KList> values = new HashSet<>(); }
    private class One extends Mapping { Map<KList, Set<KList>> values = new HashMap<>(); }

    private Mapping mapping = new Nil();
    private boolean unknownMappingType() { assert false : "unknown mapping type"; return false; }

    public static final Function IDENTITY = new Function();
    static {
        ((Nil) IDENTITY.mapping).values.add(KList.EMPTY);
    }
    static Function empty() { return new Function(); }

    // Converts a Nil to a One with the given contexts
    private void promote(Set<KList> contexts) {
        assert this.mapping instanceof Nil;
        Set<KList> oldValues = ((Nil) this.mapping).values;
        this.mapping = new One();
        for (KList key : contexts) {
            // Java, why you no have copy constructor?!
            Set<KList> value = new HashSet<>();
            value.addAll(oldValues);
            ((One) this.mapping).values.put(key, value);
        }
    }

    // Should be method of KList
    private static KList append(KList klist, Term t) {
        KList newKList = new KList(klist);
        newKList.add(t);
        return newKList;
    }

    // for each set in that, add adder applied to that set
    boolean addAux(Function that, com.google.common.base.Function<Set<KList>, Set<KList>> adder) {
        if (this.mapping instanceof Nil && that.mapping instanceof Nil) {
            return ((Nil) this.mapping).values.addAll(adder.apply(((Nil) that.mapping).values));
        } else if (this.mapping instanceof Nil && that.mapping instanceof One) {
            this.promote(((One) that.mapping).values.keySet());
            return this.addAux(that, adder);
        } else if (this.mapping instanceof One && that.mapping instanceof Nil) {
            boolean result = false;
            Set<KList> newValues = adder.apply(((Nil) that.mapping).values);
            for (Set<KList> values : ((One) this.mapping).values.values()) {
                result |= values.addAll(newValues);
            }
            return result;
        } else if (this.mapping instanceof One && that.mapping instanceof One) {
            boolean result = false;
            for (KList key : ((One) that.mapping).values.keySet()) {
                if (!((One) this.mapping).values.containsKey(key)) {
                    ((One) this.mapping).values.put(key, new HashSet<KList>());
                }
                result |= ((One) this.mapping).values.get(key).addAll(
                    adder.apply(((One) that.mapping).values.get(key)));
            }
            return result;
        } else { return unknownMappingType(); }
    }

    // Returns the KLists that this function maps to
    Set<KList> results() {
        if (this.mapping instanceof Nil) { return ((Nil) this.mapping).values; }
        else if (this.mapping instanceof One) {
            Set<KList> result = new HashSet<>();
            for (Set<KList> value: ((One) this.mapping).values.values()) {
                result.addAll(value);
            }
            return result;
        } else { unknownMappingType(); return null; }
    }

    Set<KList> applyToNull() {
        if (this.mapping instanceof Nil) { return ((Nil) this.mapping).values; }
        else { assert false : "unimplemented"; return null; } // TODO
    }

    public boolean addIdentity() { return add(IDENTITY); }

    public boolean add(Function that) {
        return addAux(that, new com.google.common.base.Function<Set<KList>, Set<KList>>() {
            public Set<KList> apply(Set<KList> set) { return set; }
        });
    }
    boolean addToken(Function that, String string, String sort) {
        final KApp token = Token.kAppOf(sort, string);
        return addAux(that, new com.google.common.base.Function<Set<KList>, Set<KList>>() {
            public Set<KList> apply(Set<KList> set) {
                Set<KList> result = new HashSet<>();
                for (KList klist : set) {
                    result.add(append(klist, token));
                }
                return result;
            }
        });
    }

    boolean addNTCall(Function call, final Function exit) {
        return addAux(call, new com.google.common.base.Function<Set<KList>, Set<KList>>() {
            public Set<KList> apply(Set<KList> set) {
                Set<KList> result = new HashSet<>();
                for (KList context : set) {
                    // find subset of exit that matches
                    Set<KList> matches = null;
                    if (exit.mapping instanceof Nil) {
                        matches = ((Nil) exit.mapping).values;
                    } else if (exit.mapping instanceof One) {
                        matches = ((One) exit.mapping).values.get(context);
                    } else { unknownMappingType(); }
                    // if we found some, make an amb node and append them to the KList
                    if (!matches.isEmpty()) {
                        result.add(append(context, new Ambiguity(KSorts.K, new ArrayList<Term>(matches))));
                    }
                }
                return result;
            }
        });
    }

    // NOTE: also adds rule to reactivations
    boolean addRule(Function that, final Rule rule, final StateReturn stateReturn, final MetaData metaData) {
        if (rule instanceof ContextFreeRule) {
            return addAux(that, new com.google.common.base.Function<Set<KList>, Set<KList>>() {
                public Set<KList> apply(Set<KList> set) {
                    return ((ContextFreeRule) rule).apply(set, metaData);
                }
            });
        } else if (rule instanceof ContextSensitiveRule) {
            Set<KList> ntCallContexts = stateReturn.key.stateCall.key.ntCall.context.contexts;

            if (this.mapping instanceof Nil) { promote(ntCallContexts); }

            // Build a "promoted" version of "that"
            One promotedThat = null;
            if (that.mapping instanceof Nil) {
                promotedThat = new One();
                for (KList context : ntCallContexts) {
                    promotedThat.values.put(context, ((Nil) that.mapping).values);
                }
            } else if (that.mapping instanceof One) {
                promotedThat = ((One) that.mapping);
            } else { unknownMappingType(); }

            boolean result = false;
            for (KList key : promotedThat.values.keySet()) {
                if (((One) this.mapping).values.get(key) == null) {
                    ((One) this.mapping).values.put(key, new HashSet<KList>());
                }
                result |= ((One) this.mapping).values.get(key).
                    addAll(((ContextSensitiveRule) rule).apply(key, promotedThat.values.get(key), metaData));
            }

            stateReturn.key.stateCall.key.ntCall.reactivations.add(stateReturn);

            return result;
        } else { return unknownMappingType(); }
    }
}

////////////////

public class Parser {
    /*
    public static void main(String[] args) {
        try {
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }

        NonTerminalId ntistart = new NonTerminalId("StartNT");
        StateId stistart = new StateId("StartState");
        StateId stiend = new StateId("EndState");

        NonTerminal nt1 = new NonTerminal(ntistart, stistart, new State.OrderingInfo(0), stiend, new State.OrderingInfo(100));

        RegExState res1 = new RegExState(new StateId("RegExStid"), nt1, new State.OrderingInfo(1), Pattern.compile("[a-zA-Z0-9]"));

        nt1.entryState.next.add(res1);
        nt1.entryState.next.add(nt1.exitState);
        res1.next.add(nt1.exitState);
        res1.next.add(res1);

        {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 100000; i++) {
                sb.append('a');
            }
            for (int j = 0; j < 10; j++) {
                long start = getCpuTime();
                for (int i = 0; i < 1; i++) {
                    Term result = new Parser(new ParseState(sb.toString())).parse(nt1, 0);
                }
                long end = getCpuTime();
                System.out.println("Time: " + ((end - start) / 1000000.0));
            }
        }
        try {
            System.in.read();
            System.in.read();
            System.in.read();
            System.in.read();
            System.in.read();
            System.in.read();
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // with proper implementation we are slow:
        //  - for a string of length 100, we are at 9.5 us per char
        //  - for a string of length 1000, we are at 65 us per char
        //  - for a string of length 10000, we are at 620 us per char
        // but with no AST construction we are getting no quadratic behavior and 1.6 micro seconds per character
        // - regex may slow things down
        // - computing Term.hashCode inside a function slows things down quite a bit
        // - constructing ASTs with long lists slows things down
        // - calling java.Object.hashCode is SLOOOOOOW
        // - calling RTTI versions of getStateCall, etc. are slow
    }

    public static long getCpuTime( ) {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        return bean.isCurrentThreadCpuTimeSupported( ) ?
                bean.getCurrentThreadCpuTime( ) : 0L;
    }
    */

    private final ParseState s;

    public Parser(CharSequence input) {
        s = new ParseState(input);
    }

    public Term parse(NonTerminal nt, int position) {
        // This code assumes that ordering info in the grammar are between MIN_VALUE+1 and MAX_VALUE-2
        // TODO: can we do away with the <start> non-terminal?
        NonTerminal startNt = new NonTerminal(new NonTerminalId("<start>"),
                                              new StateId("<start-entry>"),
                                              new StateId("<start-exit>"));
        NonTerminalState state = new NonTerminalState(new StateId("<start>"), startNt, nt, false);
        startNt.entryState.next.add(state);
        state.next.add(startNt.exitState);

        startNt.entryState.orderingInfo = new State.OrderingInfo(Integer.MIN_VALUE);
        startNt.exitState.orderingInfo = new State.OrderingInfo(Integer.MAX_VALUE);
        state.orderingInfo = new State.OrderingInfo(Integer.MAX_VALUE - 1);

        activateStateCall(s.getStateCall(new StateCall.Key(s.getNtCall(
            new NonTerminalCall.Key(startNt, position)), position, startNt.entryState)),
            Function.IDENTITY);

        for (StateReturn stateReturn = null;
             (stateReturn = s.stateReturnWorkList.dequeue()) != null;) {
            this.workListStep(stateReturn);
        }

        Ambiguity result = new Ambiguity(KSorts.K, new ArrayList<Term>());
        for(StateReturn stateReturn : s.getNtCall(new NonTerminalCall.Key(nt,position)).exitStateReturns) {
            if (stateReturn.key.stateEnd == s.input.length()) {
                result.getContents().add(new KList(new Ambiguity(
                    KSorts.K, stateReturn.function.applyToNull())));
            }
        }
        return result;
    }

    public ParseError getErrors() {
        int current = 0;
        for (StateCall.Key key : s.stateCalls.keySet()) {
            if (key.state instanceof PrimitiveState)
                current = Math.max(current, key.stateBegin);
        }
        Set<Pair<String, Pattern>> tokens = new HashSet<>();
        for (StateCall.Key key : s.stateCalls.keySet()) {
            if (key.state instanceof RegExState && key.stateBegin == current) {
                tokens.add(new ImmutablePair<>(
                    ((RegExState) key.state).sort, ((RegExState) key.state).pattern));
            }
        }
        return new ParseError(current, s.lines[current], s.columns[current], tokens);
    }

    public static class ParseError {
        public final int position;
        public final int column;
        public final int line;
        public final Set<Pair<String, Pattern>> tokens;

        public ParseError(int position, int line, int column, Set<Pair<String, Pattern>> tokens) {
            this.position = position;
            this.tokens = tokens;
            this.column = column;
            this.line = line;
        }
    }

    private void unknownStateType() { assert false : "Unknown state type"; }

    /****************
     * State Return
     ****************/

    public void workListStep(StateReturn stateReturn) {
        if (finishStateReturn(stateReturn)) {
            State state = stateReturn.key.stateCall.key.state;
            if (state instanceof ExitState) {
                for (StateCall stateCall : stateReturn.key.stateCall.key.ntCall.callers) {
                    s.stateReturnWorkList.enqueue(
                        s.getStateReturn(
                            new StateReturn.Key(stateCall, stateReturn.key.stateEnd)));
                }
            } else if (state instanceof NextableState) {
                for (State nextState : ((NextableState) state).next) {
                    activateStateCall(s.getStateCall(new StateCall.Key(
                        stateReturn.key.stateCall.key.ntCall, stateReturn.key.stateEnd, nextState)),
                        stateReturn.function);
                }
            } else { unknownStateType(); }
        }
    }

    // get function from call to return
    private boolean finishStateReturn(StateReturn stateReturn) {
        if (stateReturn.key.stateCall.key.state instanceof EntryState) {
            return stateReturn.function.add(stateReturn.key.stateCall.function);
        } else if (stateReturn.key.stateCall.key.state instanceof ExitState) {
            return stateReturn.function.add(stateReturn.key.stateCall.function);
        } else if (stateReturn.key.stateCall.key.state instanceof PrimitiveState) {
            return stateReturn.function.addToken(stateReturn.key.stateCall.function,
                s.input.subSequence(stateReturn.key.stateCall.key.stateBegin,
                    stateReturn.key.stateEnd).toString(),
                ((PrimitiveState) stateReturn.key.stateCall.key.state).sort);
        } else if (stateReturn.key.stateCall.key.state instanceof RuleState) {
            int startPosition = stateReturn.key.stateCall.key.ntCall.key.ntBegin;
            int endPosition = stateReturn.key.stateEnd;
            return stateReturn.function.addRule(stateReturn.key.stateCall.function,
                ((RuleState) stateReturn.key.stateCall.key.state).rule, stateReturn,
                new MetaData(startPosition, s.lines[startPosition], s.columns[startPosition],
                             endPosition, s.lines[endPosition], s.columns[endPosition]));
        } else if (stateReturn.key.stateCall.key.state instanceof NonTerminalState) {
            return stateReturn.function.addNTCall(
                stateReturn.key.stateCall.function,
                s.getStateReturn(new StateReturn.Key(
                    s.getStateCall(new StateCall.Key(
                        s.getNtCall(new NonTerminalCall.Key(
                            ((Grammar.NonTerminalState) stateReturn.key.stateCall.key.state).child,
                            stateReturn.key.stateCall.key.stateBegin)),
                        stateReturn.key.stateEnd,
                        ((Grammar.NonTerminalState) stateReturn.key.stateCall.key.state).child.exitState)),
                    stateReturn.key.stateEnd)).function);
        } else { unknownStateType(); return false; }
    }

    // copy function from state return to next state call
    // also put state return in the queue if need be
    private void activateStateCall(StateCall stateCall, Function function) {
        if (!stateCall.function.add(function)) { return; }
        State nextState = stateCall.key.state;
        // instanceof SimpleState
        if (nextState instanceof EntryState) {
            s.stateReturnWorkList.enqueue(
                s.getStateReturn(
                    new StateReturn.Key(stateCall, stateCall.key.stateBegin)));
        } else if (nextState instanceof ExitState) {
            s.stateReturnWorkList.enqueue(
                s.getStateReturn(
                    new StateReturn.Key(stateCall, stateCall.key.stateBegin)));
        } else if (nextState instanceof RuleState) {
            s.stateReturnWorkList.enqueue(
                s.getStateReturn(
                    new StateReturn.Key(stateCall, stateCall.key.stateBegin)));
        } else if (nextState instanceof PrimitiveState) {
            for (PrimitiveState.MatchResult matchResult :
                    ((PrimitiveState)nextState).matches(s.input, stateCall.key.stateBegin)) {
                s.stateReturnWorkList.enqueue(
                    s.getStateReturn(
                        new StateReturn.Key(stateCall, matchResult.matchEnd)));
            }
        // not instanceof SimpleState
        } else if (nextState instanceof NonTerminalState) {
            // make sure lookaheads have a stateReturn even if empty
            if (((NonTerminalState) nextState).isLookahead) {
                s.stateReturnWorkList.enqueue(
                    s.getStateReturn(
                        new StateReturn.Key(stateCall, stateCall.key.stateBegin)));
            }
            // add to the ntCall
            NonTerminalCall ntCall = s.getNtCall(new NonTerminalCall.Key(
                ((NonTerminalState) nextState).child, stateCall.key.stateBegin));
            ntCall.callers.add(stateCall);
            if (ntCall.context.contexts.addAll(stateCall.function.results())) {
                // enqueues anything sensitive
                s.stateReturnWorkList.addAll(ntCall.reactivations);
            }
            // activate the entry state call (almost like activateStateCall but we have no stateReturn)
            StateCall entryStateCall = s.getStateCall(new StateCall.Key(
                ntCall, stateCall.key.stateBegin, ntCall.key.nt.entryState));
            activateStateCall(entryStateCall, Function.IDENTITY);
            // process existStateReturns already done in the ntCall
            for (StateReturn exitStateReturn : ntCall.exitStateReturns) {
                s.stateReturnWorkList.enqueue(
                    s.getStateReturn(
                        new StateReturn.Key(stateCall, exitStateReturn.key.stateEnd)));
            }
        } else { unknownStateType(); }
    }
}
