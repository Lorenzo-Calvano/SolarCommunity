import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class SensorBehaviour extends CyclicBehaviour {
    //using this template, we simulate a change in the environment
    private MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPAGATE);


    @Override
    public void action() {
        ACLMessage msg = myAgent.receive(mt);
        if(msg!=null){
            System.out.println(myAgent.getLocalName() + ": sensore attivato!");

            //extract content and set value for agent
            String[] con = msg.getContent().split("@");
            String select = con[0].toLowerCase();
            Double value = Double.parseDouble(con[1]);

            //1 --> energy produced || 0 --> energy consumed
            synchronized (myAgent) {
                if (select.matches("produced")) {
                    ((SolarAgent) myAgent).setEnergyProduced(value);
                } else if (select.matches("consumed")) {
                    ((SolarAgent) myAgent).setEnergyConsumed(value);
                }

                //set new energy step
                ((SolarAgent) myAgent).TEC.setEnergyTimeStep();
                //update gui
                ((SolarAgent) myAgent).gui.updateGUI();
            }
        }
        else
            block();
    }
}
