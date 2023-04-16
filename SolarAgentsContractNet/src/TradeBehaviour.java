import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;

//synchronization needed
public class TradeBehaviour extends CyclicBehaviour{
    //template of message to react to
    private MessageTemplate mt = MessageTemplate.or(
                MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchPerformative(ACLMessage.INFORM_IF)
            );


    @Override
    public void action() {

        //System.out.println(myAgent.getLocalName() + ": Controllo INFORM ricevuti");

        //receive inform messages: change in one trade between agents
        ACLMessage msg = myAgent.receive(mt);
        if (msg != null) {
            ArrayList<EnergyTrade> Trades = null;
            String str = null;

            //determine if it is for sender (inform if) or receiver (inform)
            if (msg.getPerformative() == ACLMessage.INFORM) {
                Trades = ((SolarAgent) myAgent).EnergyReceived;
                str = "Sender: ";
            } else {
                Trades = ((SolarAgent) myAgent).EnergySent;
                str = "Receiver: ";
            }

            System.out.println("INFORM ricevuto da " + msg.getSender().getLocalName());

            //create trade with message content
            EnergyTrade Temp = new EnergyTrade(msg.getContent());

            //update value of trade in list (don't know if it works well with synchronization and everything else)
            synchronized(myAgent) {
                for (EnergyTrade E : Trades) {
                    if (E.getProcessName().matches(Temp.getProcessName())) {
                        E.setEnergyValue(Temp.getEnergyValue());
                        Temp = E;
                        System.out.println(myAgent.getLocalName() + "\n" + str + E.getProcessName() + "  --->  " + "Value: " + E.getEnergyValue() + "\n");
                        break;
                    }
                }

                //eliminate value if needed (don't know if it works, check it)
                if (Temp.getEnergyValue() == 0.0)
                    Trades.remove(Temp);

                //compute energy step
                ((SolarAgent) myAgent).TEC.setEnergyTimeStep();

                //update gui
                ((SolarAgent) myAgent).gui.updateGUI();
            }
        }
        else{
            block();
        }

    }
}
