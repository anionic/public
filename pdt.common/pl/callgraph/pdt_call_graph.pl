/*****************************************************************************
 * This file is part of the Prolog Development Tool (PDT)
 * 
 * Author: Andreas Becker
 * WWW: http://sewiki.iai.uni-bonn.de/research/pdt/start
 * Mail: pdt@lists.iai.uni-bonn.de
 * Copyright (C): 2012, CS Dept. III, University of Bonn
 * 
 * All rights reserved. This program is  made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 ****************************************************************************/

:- module(pdt_call_graph, [ensure_call_graph_generated/0, calls/7, pdt_walk_code/1]).

:- use_module(pdt_prolog_codewalk).
:- use_module(library(lists)).

pdt_walk_code(Options) :-
	ensure_call_graph_generated,
	pdt_prolog_walk_code(Options).

:- dynamic(first_run/0).
first_run.

ensure_call_graph_generated :-
	first_run,
	!,
	generate_call_graph,
	retractall(first_run).
ensure_call_graph_generated.

%% calls(CalleeModule, CalleeName, CalleeArity, CallerModule, CallerName, CallerArity, NumberOfCalls)
calls(CalleeModule, CalleeName, CalleeArity, CallerModule, CallerName, CallerArity, NumberOfCalls) :-
	calls_(CalleeModule, CalleeName, CalleeArity, CallerModule, CallerName, CallerArity, NumberOfCalls).

:- dynamic(calls_/7).

clear([]).
clear([Module:Name/Arity|Predicates]) :-
	retractall(calls_(_,_,_,Module,Name,Arity,_)),
	clear(Predicates).

:- dynamic(predicates_to_walk/1).

generate_call_graph :-
	pdt_prolog_walk_code([ trace_reference(_),
			on_trace(pdt_call_graph:assert_edge),
			new_meta_specs(pdt_call_graph:generate_call_graph_new_meta_specs),
			source(false),
			reiterate(false)
			]),
	(	predicates_to_walk(NewPredicates)
	->	clear(NewPredicates),
		generate_call_graph(NewPredicates)
	;	true
	).

generate_call_graph(Predicates) :-
	pdt_prolog_walk_code([ trace_reference(_),
			on_trace(pdt_call_graph:assert_edge),
			new_meta_specs(pdt_call_graph:generate_call_graph_new_meta_specs),
			source(false),
			reiterate(false),
			predicates(Predicates)
			]),
	(	predicates_to_walk(NewPredicates)
	->	clear(NewPredicates),
		generate_call_graph(NewPredicates)
	;	true
	).

generate_call_graph_new_meta_specs(MetaSpecs) :-
	retractall(predicates_to_walk(_)),
	findall(Module:Name/Arity, (
		member(MetaSpec, MetaSpecs),
		pi_of_head(MetaSpec, M, N, A),
		calls_(M, N, A, Module, Name, Arity, _)
	), Predicates),
	(	Predicates \== []
	->	sort(Predicates, PredicatesUnique),
		assertz(predicates_to_walk(PredicatesUnique))
	;	true
	).

assert_edge(M1:Callee, M2:Caller, _, _) :-
	(	predicate_property(M1:Callee,built_in)
	->	true
	;	functor(Callee,F1,N1),
		(	predicate_property(M1:Callee, imported_from(M0))
		->	M = M0
		;	M = M1
		),
		functor(Caller,F2,N2), 
		assert_edge_(M,F1,N1, M2,F2,N2)
	). 
assert_edge(_, '<initialization>', _, _) :- !.

assert_edge_(M1,F1,N1, M2,F2,N2) :-
	retract( calls_(M1,F1,N1, M2,F2,N2, Counter) ), 
	!,
	Cnt_plus_1 is Counter + 1,
	assertz(calls_(M1,F1,N1, M2,F2,N2, Cnt_plus_1)).
assert_edge_(M1,F1,N1, M2,F2,N2) :-
	assertz(calls_(M1,F1,N1, M2,F2,N2, 1)).

:- multifile(pdt_reload:pdt_reload_listener/1).
pdt_reload:pdt_reload_listener(_Files) :-
	(	first_run
	->	true
	;	setof(Module:Name/Arity, Head^(
			(	pdt_reload:reloaded_file(File),
				source_file(Head, File),
				pi_of_head(Head, Module, Name, Arity)
			;	retract(predicate_to_clear(Module, Name, Arity))
			)
		), Predicates),
		clear(Predicates),
		generate_call_graph(Predicates)
	).

:- multifile(user:message_hook/3).
:- dynamic(user:message_hook/3).
user:message_hook(load_file(start(_, file(_, File))),_,_) :-
	\+ first_run,
	source_file(Head, File),
	pi_of_head(Head, Module, Name, Arity),
	\+ predicate_to_clear(Module, Name, Arity),
	assertz(predicate_to_clear(Module, Name, Arity)),
	fail.

pi_of_head(Module:Head, Module, Name, Arity) :-
	!,
	functor(Head, Name, Arity).
pi_of_head(Head, user, Name, Arity) :-
	functor(Head, Name, Arity).

:- dynamic(predicate_to_clear/3).

