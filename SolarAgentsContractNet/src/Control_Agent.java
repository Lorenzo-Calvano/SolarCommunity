import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.gui.GuiAgent;
import jade.gui.GuiEvent;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;

public class Control_Agent extends GuiAgent {

    ArrayList<AID> SolarAgents;
    AutoBehaviour AutoControl = new AutoBehaviour();
    FileBehaviour FileScan = new FileBehaviour();
    ControlGUI gui;

    protected void setup(){

        //wait for agents to register at services, then discover
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        this.SolarAgents = new ArrayList<>();
        this.DiscoverAgents();

        this.gui = new ControlGUI(this);
        //end of setup
        System.out.println(this.getLocalName()+ ": pronto a lavorare!");
    }


    public void DiscoverAgents(){
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription s = new ServiceDescription();
        s.setType("Sender");
        template.addServices(s);
        try {
            DFAgentDescription[] result = DFService.search(this, template);

            for (int i = 0; i < result.length; ++i) {
                this.SolarAgents.add(result[i].getName());
            }
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    //set automatic behaviour
    @Override
    protected void onGuiEvent(GuiEvent guiEvent) {
        this.removeBehaviour(this.AutoControl);
        this.removeBehaviour(this.FileScan);
        if(guiEvent.getType() == 1){
            this.addBehaviour(this.AutoControl);
        }
        else if(guiEvent.getType() == 2){
            //get file, read it
            String filename = this.gui.Files.getText();
            this.FileScan.readCSV(filename);

            //start behaviour
            this.addBehaviour(this.FileScan);
        }
    }

    //send message to agent, changing its state (consumed or produced)
    public void send_change(String Rec, double value, String payload){
        //create and send message
        AID receiver = new AID(Rec, AID.ISLOCALNAME);
        ACLMessage change = new ACLMessage(ACLMessage.PROPAGATE);
        change.addReceiver(receiver);
        change.setContent(payload + value);
        this.send(change);
    }

    protected void takeDown(){

    }
}

