import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.text.DecimalFormat;


//create the thread to manage the contract net system
public class ProposalBehaviour extends CyclicBehaviour {
    private MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);


    @Override
    public void action() {
        //receive request for proposal
        ACLMessage msg = myAgent.receive(mt);

        if(msg != null){
            //create thread to respond to call for proposals
            ThreadProposal TP = new ThreadProposal(msg, (SolarAgent) myAgent);
            TP.start();
        }
        else
            block();
    }

}
