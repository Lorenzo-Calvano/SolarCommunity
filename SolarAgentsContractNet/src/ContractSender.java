import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class ContractSender {

    public void searchEnergyExchange(double EB, SolarAgent Solar){

        //call for proposals
        this.sendCFP(EB, Solar);

        //receive all proposals by agents and decide the best one
        ArrayList<ACLMessage> Proposals = receiveProposals(Solar);
        ACLMessage Accepted = BestProposal(Proposals);

        double value = 0.0;
        //create the new trade of energy (if it is necessary)
        if(Accepted != null){
            EnergyTrade E = new EnergyTrade(Accepted.getContent());

            value = E.getEnergyValue();
            if(E.getEnergyValue() > (-EB)) {
                E.setEnergyValue(-EB);
                value = (-EB);
            }

            //control if trade with agent is already present
            //if so, update the element, otherwise add the new one to the list
            synchronized (Solar) {
                int check = 0;
                for (EnergyTrade Trade : Solar.EnergyReceived) {
                    if (E.getProcessName().matches(Trade.getProcessName())) {
                        Trade.setEnergyValue(Trade.getEnergyValue() + E.getEnergyValue());
                        check = 1;
                        break;
                    }
                }
                //add element if sender agent is not present
                if (check == 0)
                    Solar.EnergyReceived.add(E);

                Solar.TEC.setEnergyTimeStep();
                //visualize
                Solar.gui.updateGUI();
            }
        }

        //send replies to everyone
        sendResponse(Proposals, Accepted, Solar, value);

        //way to display contract net interface
        this.Contract_GUI(EB, Solar, Accepted, Proposals);
    }


    private void sendCFP(double EB, SolarAgent Solar){
        //create message
        ACLMessage msg = new ACLMessage(ACLMessage.CFP);

        //add receivers
        for(int i=0; i<Solar.SolarAgents.size(); i++)
            msg.addReceiver(Solar.SolarAgents.get(i));

        //add additional information (the actual value requested)
        msg.setContent(String.valueOf(-EB));
        //send message
        Solar.send(msg);

    }

    private ArrayList<ACLMessage> receiveProposals(SolarAgent Solar){
        ArrayList<ACLMessage> M = new ArrayList<>();

        //wait for all responses (if one agent fails during this, there is a big problem)
        int i=0;
        while(i < Solar.SolarAgents.size()){
            //wait for refuse or proposal
            ACLMessage msg = Solar.receive(MessageTemplate.or(
                    MessageTemplate.MatchPerformative(ACLMessage.PROPOSE), MessageTemplate.MatchPerformative(ACLMessage.REFUSE)));

            if(msg!= null) {
                //add only proposals
                if(msg.getPerformative() != ACLMessage.REFUSE) {
                    M.add(msg);
                    System.out.println(Solar.getLocalName() + ": ricevuta proposta da " + msg.getSender().getLocalName());
                }
                else
                    System.out.println(Solar.getLocalName() + ": rifiuto da " + msg.getSender().getLocalName());

                i++;
            }
        }

        return M;
    }

    private ACLMessage BestProposal(ArrayList<ACLMessage> Proposals){
        //search for best proposal (the one by agent with most energy to share)
        double max = 0;
        ACLMessage Accepted = null;
        for (ACLMessage m : Proposals) {
            EnergyTrade E = new EnergyTrade(m.getContent());
            if(E.getEnergyValue() > max){
                max = E.getEnergyValue();
                Accepted = m;
            }
        }
        return Accepted;
    }

    private void sendResponse(ArrayList<ACLMessage> Proposals, ACLMessage Accepted, SolarAgent Solar, double value){

        //respond to everyone
        ACLMessage reply;
        for (ACLMessage m : Proposals) {
            if(m == Accepted) {
                reply = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                reply.setContent(String.valueOf(value));
            }
            else
                reply = new ACLMessage(ACLMessage.REJECT_PROPOSAL);


            reply.addReceiver(m.getSender());
            Solar.send(reply);
        }
    }


    private void Contract_GUI(double EB, SolarAgent Solar, ACLMessage Accepted, ArrayList<ACLMessage> Proposals){
        DecimalFormat dec = new DecimalFormat();
        dec.setMaximumFractionDigits(3);

        String msg = "<html> <p align=center><font size=4> ------------------------- <br></font>";

        //call for proposal sent to agents
        msg += ("<font size=5>Master</font> <br> <br>");
        msg += ("<font size=4> Invio CFP per ottenere " + dec.format(-EB) + " W <br> <br> ");




        /*Solar.gui.update_Contracts(msg + "</font> </p> </html>");
        try {
            Thread.sleep(1000);
            ACLMessage c = new ACLMessage(ACLMessage.DISCONFIRM);
            for (AID ag : Solar.SolarAgents) {
                c.addReceiver(ag);
            }
            Solar.send(c);
        } catch(Exception e){
        }
        msg = "<html> <p align=center> <font size=4>";
        for (int i=0; i<Solar.SolarAgents.size(); i++){
            Solar.blockingReceive(MessageTemplate.MatchPerformative(ACLMessage.DISCONFIRM));
        }
        try {
            Thread.sleep(1000);
        } catch(Exception e){

        }*/




        //get all proposals
        for (ACLMessage prop : Proposals) {
            EnergyTrade tmp = new EnergyTrade(prop.getContent());
            msg += ("Proposta da " + prop.getSender().getLocalName() + ": " + dec.format(tmp.getEnergyValue()) + " W <br>");
        }
        if(Proposals.size() < Solar.SolarAgents.size()) {
            int check;
            for (AID sol : Solar.SolarAgents) {
                check = 0;
                for (ACLMessage m : Proposals) {
                    if(m.getSender().getLocalName().matches(sol.getLocalName()))
                        check = 1;

                }
                if(check == 0)
                    msg += (sol.getLocalName() + " ha rifiutato <br>");
            }
        }

        msg += "<br>";




        /*Solar.gui.update_Contracts(msg + "</font> </p> </html>");
        try {
            Thread.sleep(1000);
        } catch(Exception e){

        }
        msg = "<html> <p align=center> <font size=4>";*/




        //remember the accepted one (if any)
        if(Accepted == null)
            msg += "Nessuna proposta valida <br> Contract Net fallita <br>";
        else{
            EnergyTrade e = new EnergyTrade(Accepted.getContent());
            if(e.getEnergyValue() >= -EB)
                msg += "Ho selezionato " + e.getProcessName() + " per ricevere i " + dec.format(-EB) + " W <br>";
            else
                msg += "La proposta di " + e.getProcessName() + " di " + dec.format(e.getEnergyValue()) + " W, anche se" +
                        "<br> inferiore alla richiesta, è stata accettata! <br>";
        }
        msg += "</font> <font size=4>------------------------- <br></font> </p> </html>";

        Solar.gui.update_Contracts(msg);




        /*ACLMessage c = new ACLMessage(ACLMessage.DISCONFIRM);
        for (AID ag : Solar.SolarAgents) {
            c.addReceiver(ag);
        }
        Solar.send(c);*/



    }
}
