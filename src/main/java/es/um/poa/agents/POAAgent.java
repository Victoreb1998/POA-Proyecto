package es.um.poa.agents;

import es.um.poa.utils.AgentLoggerWrapper;
import jade.core.Agent;

public class POAAgent extends Agent {
	private static final long serialVersionUID = 1L;
	private AgentLoggerWrapper logger;
	//inicializacion
	public void setup() {
		this.logger = new AgentLoggerWrapper(this);
	}
	
	public AgentLoggerWrapper getLogger() {
		return this.logger;
	}
	//metodo para eliminar 
	public void takeDown() {
		super.takeDown();
		this.logger.close();
	}
}
