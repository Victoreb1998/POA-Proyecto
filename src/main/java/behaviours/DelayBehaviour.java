package behaviours;

import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;

public class DelayBehaviour extends SimpleBehaviour {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private long timeout, wakeupTime;
	private boolean finished = false;

	public DelayBehaviour(Agent a, long timeout) {
		super(a);
		this.timeout = timeout;
	}

	public void onStart() {
		wakeupTime = System.currentTimeMillis() + timeout;
	}

	public void action() {
		long dt = wakeupTime - System.currentTimeMillis();
		if (dt <= 0) {
			finished = true;
			handleElapsedTimeout();
		} else
			block(dt);

	} // end of action

	protected void handleElapsedTimeout() // by default do nothing !
	{
	}

	public boolean done() {
		return finished;
	}
}