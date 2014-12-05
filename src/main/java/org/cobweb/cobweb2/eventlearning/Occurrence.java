package org.cobweb.cobweb2.eventlearning;

import org.cobweb.cobweb2.core.ComplexAgentLearning;

public abstract class Occurrence implements Queueable {

	public float detectableDistance;
	public ComplexAgentLearning target;
	public long time;
	private MemorableEvent event;
	private boolean hasOccurred = false;
	String desc;

	public Occurrence(ComplexAgentLearning target, float detectableDistance, String desc) {
		this.target = target;
		time = target.getCurrTick();
		this.detectableDistance = detectableDistance;
		this.desc = desc;
		ComplexAgentLearning.allOccurrences.add(this);
	}

	// Effects the agent in whatever way necessary returning a memory of the
	// effect
	public abstract MemorableEvent effect(ComplexAgentLearning concernedAgent);

	// Causes the effect to occur, initializes the event, places the memory
	// in the agent's memory,
	// returns the event
	public final void happen() {
		event = effect(target);
		target.remember(event);
		hasOccurred = true;
	}

	public MemorableEvent getEvent() {
		if (!hasOccurred) {
			throw new IllegalStateException("Cannot get event that has not occured");
		}
		return event;
	}

	public boolean hasOccurred() {
		return hasOccurred;
	}

	public boolean isComplete() {
		return true;
	}

	public final String getDescription() {
		return desc;
	}
}