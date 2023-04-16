import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.text.DecimalFormat;

public class ThreadProposal extends Thread{

    ACLMessage msg;
    SolarAgent myAgent;


    public ThreadProposal(ACLMessage msg, SolarAgent myAgent){
        this.msg = msg;
        this.myAgent = myAgent;
    }


    public void run(){
        //create proposal offer for the requester
        System.out.println(myAgent.getLocalName()+ ": Ricevuta richiesta di energia da " + msg.getSender().getLocalName());
        double ValReq = Double.parseDouble(msg.getContent());

        //current balance of energy (Sender prospective)
        double EB = myAgent.computeSenderBalance();

        if(EB>0){
            this.sendProposal(msg, EB);

            //wait for response by agent receiver (accepted or refused)
            msg = myAgent.blockingReceive(
                    MessageTemplate.or(
                            MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL), MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL)
                    )
            );
            if(msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL)
                ValReq = Double.parseDouble(msg.getContent());

            this.handleFinalResponse(msg, Math.min(ValReq, EB));
        }
        else
            this.sendRefuse(msg);


        //messages for contract gui
        this.Contract_GUI(ValReq, EB, msg);
    }

    //creation of proposal for agent requesting energy
    private void sendProposal(ACLMessage CFP, double PropEner){
        //Create response
        ACLMessage Proposal = new ACLMessage(ACLMessage.PROPOSE);
        Proposal.addReceiver(CFP.getSender());

        //offer a proposal of contract
        EnergyTrade E = new EnergyTrade(myAgent.getLocalName(), PropEner, 0.0);
        Proposal.setContent(E.toString());

        //send proposal
        myAgent.send(Proposal);
    }

    private void sendRefuse(ACLMessage CFP){
        //Create refuse
        ACLMessage Proposal = new ACLMessage(ACLMessage.REFUSE);
        Proposal.addReceiver(CFP.getSender());

        //send refuse
        myAgent.send(Proposal);
    }
    private void handleFinalResponse(ACLMessage Response, double ExcEner){
        //check response (agree ore refuse)
        if(Response.getPerformative() == ACLMessage.ACCEPT_PROPOSAL){
            System.out.println(myAgent.getLocalName() + ": Accettato!");

            //add element to sender list (check if other exchange with same agent is already present)
            EnergyTrade E = new EnergyTrade(Response.getSender().getLocalName(), ExcEner,0.0);
            synchronized(myAgent) {
                int check = 0;
                for (EnergyTrade e : ((SolarAgent) myAgent).EnergySent) {
                    if (e.getProcessName().matches(E.getProcessName())) {
                        e.setEnergyValue(e.getEnergyValue() + E.getEnergyValue());
                        check = 1;
                        break;
                    }
                }
                if (check == 0)
                    ((SolarAgent) myAgent).EnergySent.add(E);

                //compute the new energy step
                ((SolarAgent) myAgent).TEC.setEnergyTimeStep();

                //update gui
                ((SolarAgent) myAgent).gui.updateGUI();
            }
        }
        else
            System.out.println(myAgent.getLocalName() + ": Rifiutato!");
    }


    //update of graphics for contracts
    private void Contract_GUI(double valReq, double EB, ACLMessage msg) {
        DecimalFormat dec = new DecimalFormat();
        dec.setMaximumFractionDigits(3);

        String message = "<html> <p align=center><font size=4> ------------------------- <br></font>";

        //CFP received by agent
        message += ("<font size=5>Partecipante</font> <br> <br>");
        message += ("<font size=4> Ricevuto CFP da " + msg.getSender().getLocalName() + " per " + dec.format(valReq) + " W<br><br>");

        //proposal done
        if(EB > 0) {
            message += ("Proposta inviata: " + dec.format(EB) + " W <br><br>");

            //accepted or rejected
            if(msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                if( valReq < EB)
                    message += ("La mia proposta era superiore alla <br> " +
                            "richiesta, per cui invierò solo " + dec.format(valReq) + " W<br>");
                else
                    message += ("La mia proposta di "+ dec.format(EB) +" W è stata accettata<br>");
            }
            else
                message += "La mia proposta è stata rifiutata!<br>";
        }
        else
            message += ("Non posso soddisfare la richiesta, <br> rifiuto inviato<br>");

        message += "------------------------- <br></font> </p> </html>";
        ((SolarAgent) myAgent).gui.update_Contracts(message);
    }

}
