:- module(pdt_builder,
	[	%pdt_request_target/1, 
		%pdt_release_target/1,		
		pdt_invalidate_target/1,
		pdt_with_targets/2,
		pdt_restart_arbiter/0,
		debugme/0
	]
). 
 
:- use_module(library(pif_observe)).


/* hooks */
:- dynamic 
	build_hook/1,
	invalidate_hook/1,
	delete_hook/1.
:- multifile
	build_hook/1,
	invalidate_hook/1,
	delete_hook/1.

/* associate state with targets */
:- dynamic target_state/2.


:- module_transparent pdt_with_targets/2.

%% pdt_with_targets(+Targets,+Goal).
% holds read locks for the specified list of Targets while executing Goal.
%
% IMPORTANT NOTE: This predicate uses call_cleanup/2 to take care of releasing 
% the locks. Bear in mind that as long as there are any choice points left in Goal, 
% the locks are NOT released. 
%

pdt_with_targets([],Goal):-
	call(Goal).
pdt_with_targets([Target|Targets],Goal):-
    pdt_builder:pdt_request_target(Target),
    call_cleanup(
    	pdt_with_targets(Targets,Goal),
    	pdt_builder:pdt_release_target(Target)
    ).
	


%%
% pdt_request_target(+Target)
% request a target.
%
% @deprecated: In most situations, you really want to use pdt_with_targets/2. 
%
% Make sure the information associated with Target is present and up to date.
% If necessary, the calling thread will wait until the information is built.
%
% The calling thread obtains a "read lock" on the specified target.
% As long as a target is locked, it can not be built.
% The lock can be released using pdt_release_target/1.
%
pdt_request_target(Target):-
    thread_self(Me),
   	thread_send_message(build_arbiter,msg(Target,request(Me))),
	thread_get_message(Msg),
	debug(builder(debug),"Thread ~w received message ~w.~n",[Me,Msg]),
    (	Msg==grant(Target)
    ->	true
    ;	Msg==rebuild(Target)
    ->  build_target(Target),
    	pdt_request_target(Target)
    ;	Msg=error(Target,E)
    ->	throw(error(target_error(Target,E)))
    ;	Msg=fail(Target)
    ->	throw(error(target_failed(Target)))
    ;	Msg=cycle(Target)
    ->	throw(error(cycle(Target)))    
    ;	throw(error(unexpected_message(Msg,wait_for_read_lock(Target))))
    ).


build_target(Target):-
    (	catch(
    		forall(build_hook(Target),true),
    		E,
    		(	thread_send_message(build_arbiter,msg(Target,error(E))),
    			throw(E)
    		)
    	)    		
    ->	thread_send_message(build_arbiter,msg(Target,mark_clean))
    ;	thread_send_message(build_arbiter,msg(Target,fail))
    ).

%%
% pdt_release_target(+Target).
% release a target.
% 
% @deprecated: In most situations, you really want to use pdt_with_targets/2.
%
% Release a read lock the current thread holds on the Target.
% TODO: currently we do not check wether the thread actually has a lock.
pdt_release_target(Target):-
	thread_send_message(build_arbiter,msg(Target,release)).

%%
% pdt_invalidate_target(+Target)
% invalidate a target.
% 
% Marks the information associated with Target as obsolete.
pdt_invalidate_target(Target):-
	thread_send_message(build_arbiter,msg(Target,mark_dirty)).



current_target_state(Target,State):-
    target_state(Target,_),
    !,
    target_state(Target,State).
current_target_state(_Target,state(idle,outdated,[],[])).

update_target_state(Target,state(idle,outdated,[],[])):-
    !,
    retractall(target_state(Target,_)).
update_target_state(Target,NewState):-
    retractall(target_state(Target,_)),
    assert(target_state(Target,NewState)).

    

stop_arbiter:-
    current_thread(build_arbiter,Status),
    !,
    (	Status==running	
    ->  thread_send_message(build_arbiter,msg(all,stop))
    ;	true
    ),
    thread_join(build_arbiter,ExitStatus),
    debug(builder(info),"build_arbiter stopped with status ~w~n",[ExitStatus]).
stop_arbiter.    
    
start_arbiter:-
    current_thread(build_arbiter,running),
    !.
start_arbiter:-    
    thread_create(run_arbiter,_,[alias(build_arbiter)]).

pdt_restart_arbiter:-
    stop_arbiter,
    start_arbiter.



run_arbiter:-       
	repeat,
		thread_get_message(msg(Target,Event)),
		catch(
			process_message(Target,Event),
			Error,
			(	%trace,
				report_error(Error),
				retractall(target_state(_,_)),
				throw(Error)
			)
		),		
		Event==stop,
	!,
	report_error(arbiter_quits),
	retractall(target_state(_,_)).
	
report_error(Error):-
    forall(
    	target_state(_,state(_,TargetStatus,_,Threads)),
    	(	TargetStatus=pending(Thread)
    	->	report_error([Thread|Threads],Error)
    	;	report_error(Threads,Error)
    	)
    ).

report_error([],_Error).
report_error([Thread|Threads],Error):-
    thread_send_message(Thread,arbiter_error(Error)),
    report_error(Threads,Error).
    
process_message(all,stop):-!.
process_message(Target,Event):-
    current_target_state(Target,State),    
    (	target_transition(State,Event,Action,NewState,Target)
    ->  debug(builder(debug),"Target: ~w, Transition: ~w, ~w ---> ~w,~w~n",[Target,State,Event,Action,NewState]),
    	update_target_state(Target,NewState),
	    (	execute_action(Action,Target)
	    ->	true
	    ;	debug(builder(error),"action failed ~w (target: ~w)~n",[Action,Target]),
	    	throw(error(action_failed(Target,State,Event,Action)))
	    )
	;	debug(builder(error),"no transition for state ~w, event ~w (target: ~w)~n",[State,Event,Target]),
		throw(error(no_transition(Target,State,Event)))
	).
 
debugme:-
	debug(builder(debug),"ouch~n",[]).

execute_action([],_).
execute_action([Action|Actions],Target):-
    execute_action(Action,Target),
    execute_action(Actions,Target).
execute_action(grant([]),_Target).
execute_action(grant([Thread|Threads]),Target):-
    thread_send_message(Thread,grant(Target)),
    execute_action(grant(Threads),Target).
execute_action(report_failure([]),_Target).
execute_action(report_failure([Thread|Threads]),Target):-
    thread_send_message(Thread,fail(Target)),
    execute_action(report_failure(Threads),Target).
execute_action(report_error([],_E),_Target).
execute_action(report_error([Thread|Threads],E),Target):-
    thread_send_message(Thread,error(Target,E)),
    execute_action(report_error(Threads,E),Target).
/*
 FIXME: 
 Probably it would be best to make sure that all
 cascading invalidation messages get processed before any
 other messages, (requests in particular).
 
 This is not that difficult. 
 when starting to invalidate, perform a state change into some
 state invalidating. within this state, all incoming messages are frozen.
  After calling the invalidate_hook, enqueue some "invalidation_done" marker.
  When this marker is read, the waiting messages are unfrozen.
  
  Problem is, that this cuts through all the target states.
  We have to add a global state component. So
  I decided to ignore the "problem", as long as possible.
  
  Possible workaround: when invalidation is requested, check if the 
  request comes in from the arbiter thread. IN this case, process it right away, 
  i.e. without using the message loop. This way we make sure that cascading 
  invalidation requests are processed before any other requests.
  I don't know how to moddel this in our state chart diagram...
*/
execute_action(invalidate,Target):-
	debug(builder(debug),"invalidating target: ~w~n",[Target]),
    pif_notify(builder(Target),invalid),
    forall(invalidate_hook(Target),true).
execute_action(rebuild(Thread),Target):-
	debug(builder(debug),"rebuilding target: ~w~n",[Target]),
	pif_notify(builder(Target),start(Thread)),
	thread_send_message(Thread,rebuild(Target)).
execute_action(report_cycle(Thread),Target):-
	thread_send_message(Thread,	cycle(Target)).
execute_action(notify_done,Target):-
	debug(builder(debug),"target done: ~w~n",[Target]),
    pif_notify(builder(Target),done).

target_transition(state(A, outdated, C,W),		 		request(T), 	report_cycle(T),		state(A, outdated, C,W) ,Target):-
    closes_cycle(T,Target).
target_transition(state(A, pending(P), C,W),		 		request(T), 	report_cycle(T),	state(A, pending(P), C,W) ,Target):-
    closes_cycle(T,Target).
target_transition(state(idle, available, [],[]), 		request(T), 	grant([T]), 			state(reading, available, [T],[]) ,_Target).
target_transition(state(reading, available, Ts,[]),		request(T), 	grant([T]), 			state(reading, available, [T|Ts],[]),_Target).
target_transition(state(reading, S, [_,T|Ts],Ws),		release, 		[], 					state(reading, S, [T|Ts],Ws),_Target).
target_transition(state(reading, available, [_],[]), 	release,		[], 					state(idle, available, [],[]),_Target).
target_transition(state(idle, pending(_) , [],Ts), 		fail,		 	report_failure(Ts),		state(idle, outdated, [],[]),_Target).
target_transition(state(idle, pending(_) , [],Ts), 		error(E),	 	report_error(Ts,E),		state(idle, outdated, [],[]),_Target).
%FIXME: do we need to react some way when invalidation is requested during pending?
target_transition(state(idle, pending(P) , [],Ts), 		mark_dirty,	 	[],						state(idle, pending(P) , [],Ts),_Target).
target_transition(state(idle, available , [],[]), 		mark_clean, 	[],						state(idle, available, [],[]),_Target).
target_transition(state(idle, _ , [],[]), 				mark_clean, 	[notify_done],			state(idle, available, [],[]),_Target).
target_transition(state(idle, _ , [],Ts), 				mark_clean, 	[notify_done,grant(Ts)],state(reading, available, Ts,[]),_Target).
target_transition(state(A, available, Ls,[]), 			mark_dirty, 	invalidate,				state(A, outdated , Ls,[]),_Target).
target_transition(state(A, outdated, [],[]), 			mark_dirty, 	[], 					state(A, outdated , [],[]),_Target).
target_transition(state(idle, outdated, [],[]), 		request(T), 	rebuild(T),				state(idle, pending(T) , [], []),_Target). 
target_transition(state(reading, outdated, Ls,Ts), 		request(T), 	[],						state(reading, outdated , Ls, [T|Ts]),_Target).
target_transition(state(reading, outdated, [_,L|Ls],Ts),release,	 	[],						state(reading, outdated , [L|Ls], Ts),_Target).
target_transition(state(reading, outdated, [_],[T|Ts]),	release,	 	rebuild(T),				state(idle, pending(T) , [], Ts),_Target).
target_transition(state(reading, outdated, [_],[]),		release,	 	[],						state(idle, outdated , [], []),_Target).
target_transition(state(idle, pending(P) , [], Ts),		request(T), 	[], 					state(idle, pending(P) , [], [T|Ts]),_Target).


/*

Cycle checkking:
A thread depends on a target if it waits for it, or if it requests it.
A target depends on a thread if it is pending, and if the thread is working on providing the target.

Invariant: the graph induced by the above relations is always acyclic.
requesting a target constitutes adding an edge. If that edge would close a cylce, an error is reported to the requesting thread.
*/

target_depends_thread(Target,Thread):-
    target_state(Target,state(_,pending(Thread2),_,_)),
    thread_depends_thread(Thread2,Thread).    
    
thread_depends_target(Thread,Target):-
    target_state(Target2,state(_,_,_,Waiting)),
    member(Thread,Waiting),
    target_depends_target(Target2,Target).


target_depends_target(Target,Target).
target_depends_target(Target1,Target2):-
    target_depends_thread(Target1,Thread),
    thread_depends_target(Thread,Target2).

thread_depends_thread(Thread,Thread).
thread_depends_thread(Thread1,Thread2):-
    thread_depends_target(Thread1,Target),
    target_depends_thread(Target,Thread2).

    
closes_cycle(Thread,Target):-  
    target_depends_thread(Target,Thread),
    !.



:- pdt_restart_arbiter.


user:prolog_exception_hook(error(resource_error(stack), local),
                               _, _, _) :-
            writeln(arsch),trace, fail.

            
             

