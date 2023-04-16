import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class ContractSenderAlt {

    public void searchEnergyExchange(double EB, SolarAgent Solar){

        //call for proposals
        this.sendCFP(EB, Solar);

        //receive all proposals by agents and decide the best one
        ArrayList<ACLMessage> Proposals = receiveProposals(Solar);
        ArrayList<EnergyTrade> Accepted = BestProposals(Proposals, -EB);

        //create all the new trade of energy (if it is necessary)
        if(Accepted.size() > 0) {

            //change the last value of energy trade
            double sum = 0;
            for (EnergyTrade e : Accepted) {
                sum += e.getEnergyValue();
            }

            EnergyTrade E = Accepted.get(Accepted.size() - 1);
            if (sum > (-EB))
                E.setEnergyValue((-EB) - sum + E.getEnergyValue());

            //update energy received
            updateEnergyReceived(Solar, Accepted);

            Solar.TEC.setEnergyTimeStep();
            Solar.gui.updateGUI();
        }

        //send replies to everyone
        sendResponse(Proposals, Accepted, Solar);

        //display contract net interface
        this.Contract_GUI_Alt(EB, Solar, Accepted, Proposals);
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

    private ArrayList<EnergyTrade> BestProposals(ArrayList<ACLMessage> Proposals, double Balance){
        //search for best proposals
        //sort the proposals (as energy trades)
        ArrayList<EnergyTrade> ET = new ArrayList<>();
        for (ACLMessage acc: Proposals) {
            ET.add(new EnergyTrade(acc.getContent()));
        }
        ET.sort(EnergyTrade::compareTo);

        //get best proposals
        ArrayList <EnergyTrade> Best = new ArrayList<>();

        for(int i = ET.size() - 1; i >= 0; i--){
            Balance -= ET.get(i).getEnergyValue();
            Best.add(ET.get(i));
            if(Balance <= 0)
                break;
        }

        return Best;
    }

    private void updateEnergyReceived(SolarAgent Solar, ArrayList<EnergyTrade> Accepted){
            //control if trade with agent is already present
            //if so, update the element, otherwise add the new one to the list
            synchronized (Solar) {
                int check;

                for (EnergyTrade E : Accepted) {
                    check = 0;
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
                }
            }
    }

    private void sendResponse(ArrayList<ACLMessage> Proposals, ArrayList<EnergyTrade> Accepted, SolarAgent Solar){

        //respond to everyone
        ACLMessage reply;

        int check;
        for (ACLMessage m : Proposals) {
            EnergyTrade tmp = new EnergyTrade(m.getContent());
            check = 0;

            //search in the accepted, if present notify
            for (EnergyTrade E : Accepted) {
                if (tmp.getProcessName().matches(E.getProcessName())) {
                    reply = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    reply.addReceiver(m.getSender());
                    reply.setContent(String.valueOf(E.getEnergyValue()));
                    Solar.send(reply);
                    check = 1;
                    break;
                }
            }

            //if not present, reject
            if(check == 0){
                reply = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                reply.addReceiver(m.getSender());
                Solar.send(reply);
            }
        }
    }



    private void Contract_GUI_Alt(double EB, SolarAgent Solar, ArrayList<EnergyTrade> Accepted, ArrayList<ACLMessage> Proposals){
        DecimalFormat dec = new DecimalFormat();
        dec.setMaximumFractionDigits(3);

        String msg = "<html> <p align=center>";

        //call for proposal sent to agents
        msg += ("<font size=4>Master</font> <br>");
        msg += ("Mando CFP agli agenti per ottenere " + dec.format(-EB) + " W <br> <br> ");

        //get all proposals
        for (ACLMessage prop : Proposals) {
            EnergyTrade tmp = new EnergyTrade(prop.getContent());
            msg += ("Ricevuta proposta da " + prop.getSender().getLocalName() + ": " + dec.format(tmp.getEnergyValue()) + " W<br>");
        }
        if(Proposals.size() < Solar.SolarAgents.size())
            msg += "Uno o più agenti hanno rifiutato <br>";

        msg += "<br>";

        //display selected proposals (if any) and total energy received
        if(Accepted.size() == 0)
            msg += "Nessuna proposta valida ricevuta, Contract Net fallita <br>";
        else{
            double sum = 0;
            for (EnergyTrade e : Accepted) {
                msg += "Ho selezionato " + e.getProcessName() + " per ricevere " + dec.format(e.getEnergyValue()) + " W <br>";
                sum += e.getEnergyValue();
            }
            msg += "In totale, riceverò " + dec.format(sum) + " W";
        }
        msg += "</p> </html>";

        Solar.gui.update_Contracts(msg);
    }
}
